package org.dromara.crehn.config.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtNoticeTemplateService;
import org.dromara.crehn.domain.NoticeTemplate;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.NoticeTemplateVo;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.mapper.NoticeTemplateMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ArtNoticeTemplateServiceImpl implements IArtNoticeTemplateService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final String HOME_NOTICE_GROUP = "展演阶段";
    private static final String REGISTER_NOTICE_GROUP = "注册公告";

    private final NoticeTemplateMapper noticeTemplateMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ISysOssService ossService;
    private final ISysUserService userService;

    @Override
    public TableDataInfo<NoticeTemplateVo> queryPage(NoticeTemplate query, PageQuery pageQuery) {
        Page<NoticeTemplateVo> page = noticeTemplateMapper.selectVoPage(pageQuery.build(), buildQuery(query));
        fillAttachments(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<NoticeTemplateVo> listForRegister(String userType, Long activityId, Long schoolId, String schoolType) {
        List<NoticeTemplateVo> list = noticeTemplateMapper.selectVoList(Wrappers.lambdaQuery(NoticeTemplate.class)
            .eq(NoticeTemplate::getEnabled, true)
            .eq(NoticeTemplate::getNoticeGroup, REGISTER_NOTICE_GROUP)
            .and(w -> w.eq(NoticeTemplate::getTargetMode, "all")
                .or().isNull(NoticeTemplate::getTargetMode)
                .or(StringUtils.isNotBlank(userType), c -> c.eq(NoticeTemplate::getTargetMode, "user_type")
                    .eq(NoticeTemplate::getTargetUserType, userType))
                .or(activityId != null, c -> c.eq(NoticeTemplate::getTargetMode, "activity")
                    .eq(NoticeTemplate::getActivityId, activityId)))
            .and(w -> w.isNull(NoticeTemplate::getSchoolId)
                .or(schoolId != null, c -> c.eq(NoticeTemplate::getSchoolId, schoolId)))
            .and(w -> w.isNull(NoticeTemplate::getSchoolType)
                .or(StringUtils.isNotBlank(schoolType), c -> c.eq(NoticeTemplate::getSchoolType, schoolType)))
            .orderByAsc(NoticeTemplate::getSortOrder)
            .orderByDesc(NoticeTemplate::getCreateTime));
        fillAttachments(list);
        return list;
    }

    @Override
    public List<NoticeTemplateVo> listForHome() {
        List<NoticeTemplateVo> list = noticeTemplateMapper.selectVoList(Wrappers.lambdaQuery(NoticeTemplate.class)
            .eq(NoticeTemplate::getEnabled, true)
            .eq(NoticeTemplate::getNoticeGroup, HOME_NOTICE_GROUP)
            .orderByAsc(NoticeTemplate::getSortOrder)
            .orderByDesc(NoticeTemplate::getCreateTime));
        fillAttachments(list);
        return list;
    }

    @Override
    public TableDataInfo<NoticeTemplateVo> queryHomePage(PageQuery pageQuery) {
        NoticeTemplate query = new NoticeTemplate();
        query.setNoticeGroup(HOME_NOTICE_GROUP);
        Page<NoticeTemplateVo> page = noticeTemplateMapper.selectVoPage(pageQuery.build(), buildQuery(query));
        fillAttachments(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public int save(NoticeTemplate template) {
        normalize(template);
        if (template.getId() == null) {
            return noticeTemplateMapper.insert(template);
        }
        return noticeTemplateMapper.updateById(template);
    }

    @Override
    public int saveHomePage(NoticeTemplate template) {
        if (template.getId() != null) {
            NoticeTemplate exist = noticeTemplateMapper.selectById(template.getId());
            if (exist != null && !HOME_NOTICE_GROUP.equals(exist.getNoticeGroup())) {
                throw new ServiceException("只能维护首页展演阶段配置");
            }
        }
        template.setNoticeGroup(HOME_NOTICE_GROUP);
        template.setTargetMode("all");
        template.setTargetUserType(null);
        template.setActivityId(null);
        template.setSchoolId(null);
        template.setSchoolType(null);
        if (StringUtils.isBlank(template.getTemplateName())) {
            template.setTemplateName(template.getNoticeTitle());
        }
        return save(template);
    }

    @Override
    public int deleteHomePage(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return noticeTemplateMapper.delete(Wrappers.lambdaQuery(NoticeTemplate.class)
            .in(NoticeTemplate::getId, Arrays.asList(ids))
            .eq(NoticeTemplate::getNoticeGroup, HOME_NOTICE_GROUP));
    }

    @Override
    public void downloadAttachment(Long ossId, HttpServletResponse response) throws IOException {
        if (ossId == null || !isEnabledNoticeAttachment(ossId)) {
            throw new ServiceException("公告附件不存在或已停用");
        }
        ossService.download(ossId, response);
    }

    @Override
    public int delete(Long[] ids) {
        return noticeTemplateMapper.deleteByIds(Arrays.asList(ids));
    }

    private void fillAttachments(List<NoticeTemplateVo> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        notices.forEach(notice -> notice.setAttachments(buildAttachments(notice.getAttachmentOssIds())));
    }

    private List<NoticeTemplateVo.AttachmentVo> buildAttachments(String ossIds) {
        List<Long> ids = parseOssIds(ossIds);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<NoticeTemplateVo.AttachmentVo> attachments = new ArrayList<>();
        for (SysOssVo item : ossService.listByIds(ids)) {
            NoticeTemplateVo.AttachmentVo attachment = new NoticeTemplateVo.AttachmentVo();
            attachment.setOssId(item.getOssId());
            attachment.setFileName(item.getFileName());
            attachment.setOriginalName(item.getOriginalName());
            attachment.setUrl(item.getUrl());
            attachments.add(attachment);
        }
        return attachments;
    }

    private boolean isEnabledNoticeAttachment(Long ossId) {
        String userType = currentNoticeUserType();
        Long schoolId = currentSchoolId(userType);
        String schoolType = currentSchoolType(schoolId);
        return noticeTemplateMapper.selectList(Wrappers.lambdaQuery(NoticeTemplate.class)
                .eq(NoticeTemplate::getEnabled, true)
                .isNotNull(NoticeTemplate::getAttachmentOssIds))
            .stream()
            .anyMatch(template -> isVisibleToCurrentUser(template, userType, schoolId, schoolType)
                && parseOssIds(template.getAttachmentOssIds()).contains(ossId));
    }

    private boolean isVisibleToCurrentUser(NoticeTemplate template, String userType, Long schoolId, String schoolType) {
        String targetMode = StringUtils.blankToDefault(template.getTargetMode(), "all");
        if ("user_type".equals(targetMode) && !StringUtils.equals(template.getTargetUserType(), userType)) {
            return false;
        }
        if (template.getSchoolId() != null && !Objects.equals(template.getSchoolId(), schoolId)) {
            return false;
        }
        return StringUtils.isBlank(template.getSchoolType()) || StringUtils.equals(template.getSchoolType(), schoolType);
    }

    private String currentNoticeUserType() {
        if (!StpUtil.isLogin()) {
            return "";
        }
        String userType = LoginHelper.getUserType().getUserType();
        return "sys_user".equals(userType) ? "admin" : userType;
    }

    private Long currentSchoolId(String userType) {
        if (!"school".equals(userType)) {
            return null;
        }
        SysUserVo user = userService.selectUserById(LoginHelper.getUserId());
        return user == null ? null : user.getSchoolId();
    }

    private String currentSchoolType(Long schoolId) {
        if (schoolId == null) {
            return null;
        }
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        return school == null ? null : school.getSchoolType();
    }

    private List<Long> parseOssIds(String ossIds) {
        if (StringUtils.isBlank(ossIds)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String id : ossIds.split(",")) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                throw new ServiceException("公告附件ID格式不正确：" + id.trim(), e);
            }
        }
        return ids;
    }

    private LambdaQueryWrapper<NoticeTemplate> buildQuery(NoticeTemplate query) {
        return Wrappers.lambdaQuery(NoticeTemplate.class)
            .like(StringUtils.isNotBlank(query.getTemplateName()), NoticeTemplate::getTemplateName, query.getTemplateName())
            .eq(StringUtils.isNotBlank(query.getNoticeGroup()), NoticeTemplate::getNoticeGroup, query.getNoticeGroup())
            .eq(StringUtils.isNotBlank(query.getTargetUserType()), NoticeTemplate::getTargetUserType, query.getTargetUserType())
            .eq(query.getEnabled() != null, NoticeTemplate::getEnabled, query.getEnabled())
            .orderByAsc(NoticeTemplate::getSortOrder)
            .orderByDesc(NoticeTemplate::getCreateTime);
    }

    private void normalize(NoticeTemplate template) {
        if (StringUtils.isBlank(template.getTemplateName())) {
            throw new ServiceException("公告模板名称不能为空");
        }
        if (StringUtils.isBlank(template.getNoticeTitle())) {
            throw new ServiceException("公告标题不能为空");
        }
        if (StringUtils.isBlank(template.getNoticeContent())) {
            throw new ServiceException("公告内容不能为空");
        }
        if (StringUtils.isBlank(template.getTemplateCode())) {
            template.setTemplateCode("notice_" + System.currentTimeMillis());
        }
        template.setTemplateCode(template.getTemplateCode().trim().toLowerCase(Locale.ROOT));
        if (!CODE_PATTERN.matcher(template.getTemplateCode()).matches()) {
            throw new ServiceException("公告模板编码只能使用小写字母、数字和下划线");
        }
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (StringUtils.isBlank(template.getTargetMode())) {
            template.setTargetMode("all");
        }
        if ("all".equals(template.getTargetMode())) {
            template.setTargetUserType(null);
            template.setActivityId(null);
        } else if ("user_type".equals(template.getTargetMode())) {
            template.setActivityId(null);
            if (StringUtils.isBlank(template.getTargetUserType())) {
                throw new ServiceException("按账号类型推送时必须选择账号类型");
            }
        } else if ("activity".equals(template.getTargetMode())) {
            template.setTargetUserType(null);
            if (template.getActivityId() == null) {
                throw new ServiceException("按活动推送时必须选择活动");
            }
        } else {
            throw new ServiceException("公告推送范围不正确");
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
    }
}
