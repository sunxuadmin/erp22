package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.config.properties.AliyunOssProperties;
import org.dromara.system.domain.SysOssConfig;
import org.dromara.system.domain.bo.SysOssConfigBo;
import org.dromara.system.domain.vo.SysOssConfigVo;
import org.dromara.system.mapper.SysOssConfigMapper;
import org.dromara.system.service.ISysOssConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 对象存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private final SysOssConfigMapper baseMapper;
    private final AliyunOssProperties aliyunOssProperties;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void init() {
        syncAliyunOssConfig();
        List<SysOssConfig> list = baseMapper.selectList();
        // 加载OSS初始化配置
        for (SysOssConfig config : list) {
            String configKey = config.getConfigKey();
            if ("0".equals(config.getStatus())) {
                RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
            }
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
    }

    /**
     * Sync deployment-level Aliyun OSS settings into the dynamic OSS table.
     */
    private void syncAliyunOssConfig() {
        if (!aliyunOssProperties.isEnabled()) {
            return;
        }
        validateAliyunOssConfig();

        String configKey = StringUtils.blankToDefault(StringUtils.trim(aliyunOssProperties.getConfigKey()), "aliyun");
        SysOssConfig config = baseMapper.selectOne(new LambdaQueryWrapper<SysOssConfig>()
            .eq(SysOssConfig::getConfigKey, configKey));

        if (aliyunOssProperties.isDefaultConfig()) {
            baseMapper.update(null, new LambdaUpdateWrapper<SysOssConfig>()
                .set(SysOssConfig::getStatus, "1"));
        }

        boolean insert = ObjectUtil.isNull(config);
        if (insert) {
            config = new SysOssConfig();
            config.setConfigKey(configKey);
        }

        applyAliyunOssConfig(config);
        int rows = insert ? baseMapper.insert(config) : baseMapper.updateById(config);
        if (rows <= 0) {
            throw new ServiceException("阿里云 OSS 配置同步失败");
        }
        log.info("Aliyun OSS config synced, configKey={}, default={}", configKey, aliyunOssProperties.isDefaultConfig());
    }

    private void validateAliyunOssConfig() {
        if (StringUtils.isBlank(aliyunOssProperties.getAccessKey())) {
            throw new ServiceException("阿里云 OSS 已启用，但 ALIYUN_OSS_ACCESS_KEY 为空");
        }
        if (StringUtils.isBlank(aliyunOssProperties.getSecretKey())) {
            throw new ServiceException("阿里云 OSS 已启用，但 ALIYUN_OSS_SECRET_KEY 为空");
        }
        if (StringUtils.isBlank(aliyunOssProperties.getBucketName())) {
            throw new ServiceException("阿里云 OSS 已启用，但 ALIYUN_OSS_BUCKET 为空");
        }
        if (StringUtils.isBlank(normalizeHost(aliyunOssProperties.getEndpoint()))) {
            throw new ServiceException("阿里云 OSS 已启用，但 ALIYUN_OSS_ENDPOINT 为空");
        }
    }

    private void applyAliyunOssConfig(SysOssConfig config) {
        config.setAccessKey(StringUtils.trim(aliyunOssProperties.getAccessKey()));
        config.setSecretKey(StringUtils.trim(aliyunOssProperties.getSecretKey()));
        config.setBucketName(StringUtils.trim(aliyunOssProperties.getBucketName()));
        config.setPrefix(trimToEmpty(aliyunOssProperties.getPrefix()));
        config.setEndpoint(normalizeHost(aliyunOssProperties.getEndpoint()));
        config.setDomain(normalizeHost(aliyunOssProperties.getDomain()));
        config.setRegion(trimToEmpty(aliyunOssProperties.getRegion()));
        config.setIsHttps(normalizeYesNo(aliyunOssProperties.getIsHttps()));
        config.setAccessPolicy(normalizeAccessPolicy(aliyunOssProperties.getAccessPolicy()));
        config.setStatus(aliyunOssProperties.isDefaultConfig() ? "0" : "1");
        config.setRemark(StringUtils.blankToDefault(aliyunOssProperties.getRemark(), "Aliyun OSS object storage"));
    }

    private String normalizeHost(String value) {
        String host = trimToEmpty(value);
        String lower = host.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            host = host.substring("https://".length());
        } else if (lower.startsWith("http://")) {
            host = host.substring("http://".length());
        }
        while (host.endsWith(StringUtils.SLASH)) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    private String trimToEmpty(String value) {
        return StringUtils.isBlank(value) ? "" : StringUtils.trim(value);
    }

    private String normalizeYesNo(String value) {
        String normalized = StringUtils.blankToDefault(value, "Y").toLowerCase(Locale.ROOT);
        if (StringUtils.equalsAny(normalized, "y", "yes", "true", "1")) {
            return "Y";
        }
        if (StringUtils.equalsAny(normalized, "n", "no", "false", "0")) {
            return "N";
        }
        throw new ServiceException("ALIYUN_OSS_HTTPS 仅支持 Y/N、true/false 或 1/0");
    }

    private String normalizeAccessPolicy(String value) {
        String normalized = StringUtils.blankToDefault(value, "0").toLowerCase(Locale.ROOT);
        if (StringUtils.equalsAny(normalized, "0", "private")) {
            return "0";
        }
        if (StringUtils.equalsAny(normalized, "1", "public")) {
            return "1";
        }
        if (StringUtils.equalsAny(normalized, "2", "custom")) {
            return "2";
        }
        throw new ServiceException("ALIYUN_OSS_ACCESS_POLICY 仅支持 0/private、1/public 或 2/custom");
    }

    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return baseMapper.selectVoById(ossConfigId);
    }

    @Override
    public TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        LambdaQueryWrapper<SysOssConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getConfigKey()), SysOssConfig::getConfigKey, bo.getConfigKey());
        lqw.like(StringUtils.isNotBlank(bo.getBucketName()), SysOssConfig::getBucketName, bo.getBucketName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysOssConfig::getStatus, bo.getStatus());
        lqw.orderByAsc(SysOssConfig::getOssConfigId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = baseMapper.insert(config) > 0;
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = baseMapper.selectById(config.getOssConfigId());
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        LambdaUpdateWrapper<SysOssConfig> luw = new LambdaUpdateWrapper<>();
        luw.set(ObjectUtil.isNull(config.getPrefix()), SysOssConfig::getPrefix, "");
        luw.set(ObjectUtil.isNull(config.getRegion()), SysOssConfig::getRegion, "");
        luw.set(ObjectUtil.isNull(config.getExt1()), SysOssConfig::getExt1, "");
        luw.set(ObjectUtil.isNull(config.getRemark()), SysOssConfig::getRemark, "");
        luw.eq(SysOssConfig::getOssConfigId, config.getOssConfigId());
        boolean flag = baseMapper.update(config, luw) > 0;
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = baseMapper.selectById(config.getOssConfigId());
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        if (StringUtils.isNotEmpty(entity.getConfigKey())
            && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'{}'失败, 配置key已存在!", entity.getConfigKey());
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids) {
            SysOssConfig config = baseMapper.selectById(configId);
            list.add(config);
        }
        boolean flag = baseMapper.deleteByIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, sysOssConfig.getConfigKey()));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        long ossConfigId = ObjectUtils.notNull(sysOssConfig.getOssConfigId(), -1L);
        SysOssConfig info = baseMapper.selectOne(new LambdaQueryWrapper<SysOssConfig>()
            .select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
            .eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey()));
        if (ObjectUtil.isNotNull(info) && info.getOssConfigId() != ossConfigId) {
            return false;
        }
        return true;
    }

    /**
     * 启用禁用状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateOssConfigStatus(SysOssConfigBo bo) {
        SysOssConfig sysOssConfig = MapstructUtils.convert(bo, SysOssConfig.class);
        int row = baseMapper.update(null, new LambdaUpdateWrapper<SysOssConfig>()
            .set(SysOssConfig::getStatus, "1"));
        row += baseMapper.updateById(sysOssConfig);
        if (row > 0) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, sysOssConfig.getConfigKey());
        }
        return row;
    }

}
