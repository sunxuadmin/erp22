package org.dromara.crehn.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.config.service.IArtSchoolInfoService;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.SchoolInfoExportVo;
import org.dromara.crehn.domain.vo.SchoolInfoImportVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.domain.vo.SchoolImpactVo;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class ArtSchoolInfoServiceImpl implements IArtSchoolInfoService {

    private static final String ART_ROOT_CATEGORY = "crehn_root";
    private static final String ART_SCHOOL_CATEGORY_PREFIX = "crehn_school_";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_RECYCLED = "recycled";
    private static final Set<String> ALLOWED_STATUS = Set.of("enabled", "disabled", "recycled");

    private final SchoolInfoMapper schoolInfoMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public TableDataInfo<SchoolInfoVo> queryPage(SchoolInfo query, PageQuery pageQuery) {
        Page<SchoolInfoVo> page = schoolInfoMapper.selectVoPage(pageQuery.build(), buildQuery(query));
        fillImpactCounts(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<SchoolInfoVo> queryList(SchoolInfo query) {
        return schoolInfoMapper.selectVoList(buildQuery(query));
    }

    @Override
    public SchoolInfoVo getInfo(Long id) {
        SchoolInfoVo vo = schoolInfoMapper.selectVoById(id);
        if (vo != null) {
            fillImpactCounts(List.of(vo));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrGetSchool(Long schoolId, String schoolName, String profileJson) {
        if (schoolId != null) {
            SchoolInfo existing = schoolInfoMapper.selectById(schoolId);
            if (existing == null) {
                throw new ServiceException("单位不存在");
            }
            if (!STATUS_ENABLED.equals(existing.getStatus())) {
                throw new ServiceException("所选学校/单位已停用或在回收站，不能用于注册绑定");
            }
            return schoolId;
        }
        if (StringUtils.isBlank(schoolName)) {
            throw new ServiceException("单位名称不能为空");
        }
        SchoolInfo existing = schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
            .eq(SchoolInfo::getSchoolName, schoolName.trim()));
        if (existing != null) {
            throw new ServiceException("学校名称已存在，请选择已有学校");
        }
        SchoolInfo school = new SchoolInfo();
        school.setSchoolName(schoolName.trim());
        school.setProfileJson(profileJson);
        school.setStatus(STATUS_ENABLED);
        schoolInfoMapper.insert(school);
        syncSchoolDept(school);
        return school.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(SchoolInfo school) {
        if (StringUtils.isBlank(school.getSchoolName())) {
            throw new ServiceException("单位名称不能为空");
        }
        school.setSchoolName(school.getSchoolName().trim());
        SchoolInfo sameName = schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
            .eq(SchoolInfo::getSchoolName, school.getSchoolName())
            .ne(school.getId() != null, SchoolInfo::getId, school.getId()));
        if (sameName != null) {
            throw new ServiceException("学校名称已存在，请选择已有学校");
        }
        if (StringUtils.isBlank(school.getStatus())) {
            school.setStatus(STATUS_ENABLED);
        }
        String oldStatus = null;
        String oldName = null;
        if (school.getId() != null) {
            SchoolInfo before = schoolInfoMapper.selectById(school.getId());
            if (before != null) {
                oldStatus = before.getStatus();
                oldName = before.getSchoolName();
            }
        }
        int rows;
        if (school.getId() == null) {
            rows = schoolInfoMapper.insert(school);
        } else {
            rows = schoolInfoMapper.updateById(school);
        }
        syncSchoolDept(school);
        auditUnitStatus("单位保存", school.getId(), StringUtils.blankToDefault(school.getSchoolName(), oldName), oldStatus, school.getStatus());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long[] ids, boolean confirmed) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("单位ID不能为空");
        }
        if (!confirmed && hasAccountOrProject(ids)) {
            throw new ServiceException("该单位下存在账号或填报项目，请先处理账号，或在查看影响统计后二次确认移入回收站");
        }
        int rows = 0;
        for (Long id : ids) {
            SchoolInfo school = schoolInfoMapper.selectById(id);
            if (school == null) {
                continue;
            }
            String oldStatus = school.getStatus();
            school.setStatus(STATUS_RECYCLED);
            rows += schoolInfoMapper.updateById(school);
            syncSchoolDept(school);
            auditUnitStatus("单位回收", school.getId(), school.getSchoolName(), oldStatus, school.getStatus());
        }
        return rows;
    }

    @Override
    public List<SchoolImpactVo> impact(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return List.of();
        }
        List<SchoolInfo> schools = schoolInfoMapper.selectBatchIds(Arrays.asList(ids));
        return schools.stream().map(this::buildImpact).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restore(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("单位ID不能为空");
        }
        int rows = 0;
        for (Long id : ids) {
            SchoolInfo school = schoolInfoMapper.selectById(id);
            if (school == null) {
                continue;
            }
            String oldStatus = school.getStatus();
            school.setStatus(STATUS_ENABLED);
            rows += schoolInfoMapper.updateById(school);
            syncSchoolDept(school);
            auditUnitStatus("单位恢复", school.getId(), school.getSchoolName(), oldStatus, school.getStatus());
        }
        return rows;
    }

    private void fillImpactCounts(List<SchoolInfoVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SchoolInfoVo item : list) {
            if (item == null || item.getId() == null) {
                continue;
            }
            SchoolImpactVo impact = buildImpact(item.getId(), item.getSchoolName(), item.getStatus());
            item.setAccountCount(impact.getAccountCount());
            item.setActiveAccountCount(impact.getActiveAccountCount());
            item.setProjectCount(impact.getProjectCount());
            item.setVisibleProjectCount(impact.getVisibleProjectCount());
            item.setRecycledProjectCount(impact.getRecycledProjectCount());
            item.setFileCount(impact.getFileCount());
            item.setMemberCount(impact.getMemberCount());
        }
    }

    private boolean hasAccountOrProject(Long[] ids) {
        return impact(ids).stream()
            .anyMatch(item -> positive(item.getAccountCount()) || positive(item.getProjectCount()));
    }

    private boolean positive(Long value) {
        return value != null && value > 0;
    }

    private SchoolImpactVo buildImpact(SchoolInfo school) {
        return buildImpact(school.getId(), school.getSchoolName(), school.getStatus());
    }

    private SchoolImpactVo buildImpact(Long schoolId, String schoolName, String schoolStatus) {
        SchoolImpactVo vo = new SchoolImpactVo();
        vo.setSchoolId(schoolId);
        vo.setSchoolName(schoolName);
        vo.setSchoolStatus(schoolStatus);
        if (schoolId == null) {
            return vo;
        }
        vo.setAccountCount(countAccounts(schoolId));
        vo.setActiveAccountCount(countActiveAccounts(schoolId));
        vo.setProjectCount(countProjects(schoolId, false));
        vo.setVisibleProjectCount(countProjects(schoolId, true));
        vo.setRecycledProjectCount(countRecycledProjects(schoolId));
        List<Long> projectIds = selectProjectIds(schoolId);
        vo.setFileCount(countFiles(projectIds));
        vo.setMemberCount(countMembers(projectIds));
        return vo;
    }

    private long countAccounts(Long schoolId) {
        return sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .eq(SysUser::getSchoolId, schoolId));
    }

    private long countActiveAccounts(Long schoolId) {
        return sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .eq(SysUser::getSchoolId, schoolId)
            .eq(SysUser::getSchoolReviewStatus, ArtReviewConstants.SCHOOL_REVIEW_ENABLED));
    }

    private long countProjects(Long schoolId, boolean excludeRecycled) {
        LambdaQueryWrapper<Project> wrapper = Wrappers.lambdaQuery(Project.class)
            .eq(Project::getSchoolId, schoolId);
        if (excludeRecycled) {
            wrapper.ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        }
        return projectMapper.selectCount(wrapper);
    }

    private long countRecycledProjects(Long schoolId) {
        return projectMapper.selectCount(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getSchoolId, schoolId)
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED));
    }

    private List<Long> selectProjectIds(Long schoolId) {
        List<Object> projectIds = projectMapper.selectObjs(Wrappers.lambdaQuery(Project.class)
                .select(Project::getId)
                .eq(Project::getSchoolId, schoolId));

        return projectIds.stream()
            .map(this::toLong)
            .filter(Objects::nonNull)
            .toList();
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private long countFiles(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return 0L;
        }
        return projectFileMapper.selectCount(Wrappers.lambdaQuery(ProjectFile.class)
            .in(ProjectFile::getProjectId, projectIds));
    }

    private long countMembers(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return 0L;
        }
        return projectMemberMapper.selectCount(Wrappers.lambdaQuery(ProjectMember.class)
            .in(ProjectMember::getProjectId, projectIds));
    }

    @Override
    public List<SchoolInfoExportVo> exportList(SchoolInfo query) {
        return schoolInfoMapper.selectVoList(buildQuery(query)).stream().map(item -> {
            SchoolInfoExportVo vo = new SchoolInfoExportVo();
            vo.setId(item.getId());
            vo.setSchoolName(item.getSchoolName());
            vo.setSchoolCode(item.getSchoolCode());
            vo.setSchoolType(item.getSchoolType());
            vo.setRegion(item.getRegion());
            vo.setAddress(item.getAddress());
            vo.setContactName(item.getContactName());
            vo.setContactPhone(item.getContactPhone());
            vo.setContactEmail(item.getContactEmail());
            vo.setStatus(item.getStatus());
            vo.setRemark(item.getRemark());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importData(List<SchoolInfoImportVo> list, boolean updateSupport) {
        if (list == null || list.isEmpty()) {
            throw new ServiceException("导入数据不能为空");
        }
        int insertNum = 0;
        int updateNum = 0;
        int skipNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        List<String> seenKeys = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            SchoolInfoImportVo row = list.get(i);
            String schoolName = trimToNull(row.getSchoolName());
            try {
                String rowKey = importRowKey(row, schoolName);
                if (StringUtils.isNotBlank(rowKey) && seenKeys.contains(rowKey)) {
                    skipNum++;
                    successMsg.append("<br/>第 ").append(i + 2).append(" 行：单位记录在本次文件中重复，已跳过");
                    continue;
                }
                if (StringUtils.isNotBlank(rowKey)) {
                    seenKeys.add(rowKey);
                }
                SchoolInfo existing = findImportExisting(row, schoolName);
                if (existing != null) {
                    if (!updateSupport) {
                        skipNum++;
                        successMsg.append("<br/>第 ").append(i + 2).append(" 行：学校名称“").append(existing.getSchoolName()).append("”已存在，已跳过");
                        continue;
                    }
                    if (!applyImportNonBlankFields(existing, row)) {
                        skipNum++;
                        successMsg.append("<br/>第 ").append(i + 2).append(" 行：学校名称“").append(existing.getSchoolName()).append("”无可覆盖内容，已跳过");
                        continue;
                    }
                    save(existing);
                    updateNum++;
                    successMsg.append("<br/>第 ").append(i + 2).append(" 行：学校名称“").append(existing.getSchoolName()).append("”覆盖成功");
                    continue;
                }
                if (StringUtils.isBlank(schoolName)) {
                    throw new ServiceException("学校名称不能为空");
                }
                SchoolInfo school = new SchoolInfo();
                school.setSchoolName(schoolName);
                school.setSchoolCode(trimToNull(row.getSchoolCode()));
                school.setSchoolType(trimToNull(row.getSchoolType()));
                school.setRegion(trimToNull(row.getRegion()));
                school.setAddress(trimToNull(row.getAddress()));
                school.setContactName(trimToNull(row.getContactName()));
                school.setContactPhone(trimToNull(row.getContactPhone()));
                school.setContactEmail(trimToNull(row.getContactEmail()));
                school.setStatus(normalizeStatus(row.getStatus()));
                school.setRemark(trimToNull(row.getRemark()));
                save(school);
                insertNum++;
                successMsg.append("<br/>第 ").append(i + 2).append(" 行：学校名称“").append(schoolName).append("”新增成功");
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>第 ").append(i + 2).append(" 行：学校名称“")
                    .append(StringUtils.blankToDefault(schoolName, "空")).append("”导入失败：").append(e.getMessage());
            }
        }
        if (failureNum > 0) {
            throw new ServiceException("导入失败，共 " + failureNum + " 条错误：" + failureMsg);
        }
        return "导入完成：新增 " + insertNum + " 条，覆盖 " + updateNum + " 条，跳过 " + skipNum + " 条。" + successMsg;
    }

    private SchoolInfo findImportExisting(SchoolInfoImportVo row, String schoolName) {
        if (row.getId() != null) {
            SchoolInfo existing = schoolInfoMapper.selectById(row.getId());
            if (existing != null) {
                return existing;
            }
        }
        String schoolCode = trimToNull(row.getSchoolCode());
        if (StringUtils.isNotBlank(schoolCode)) {
            SchoolInfo existing = schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
                .eq(SchoolInfo::getSchoolCode, schoolCode)
                .last("limit 1"));
            if (existing != null) {
                return existing;
            }
        }
        if (StringUtils.isNotBlank(schoolName)) {
            return schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
                .eq(SchoolInfo::getSchoolName, schoolName)
                .last("limit 1"));
        }
        return null;
    }

    private String importRowKey(SchoolInfoImportVo row, String schoolName) {
        if (row.getId() != null) {
            return "id:" + row.getId();
        }
        String schoolCode = trimToNull(row.getSchoolCode());
        if (StringUtils.isNotBlank(schoolCode)) {
            return "code:" + schoolCode;
        }
        if (StringUtils.isNotBlank(schoolName)) {
            return "name:" + schoolName;
        }
        return null;
    }

    private boolean applyImportNonBlankFields(SchoolInfo school, SchoolInfoImportVo row) {
        boolean changed = false;
        changed |= setIfPresent(row.getSchoolName(), school::getSchoolName, school::setSchoolName);
        changed |= setIfPresent(row.getSchoolCode(), school::getSchoolCode, school::setSchoolCode);
        changed |= setIfPresent(row.getSchoolType(), school::getSchoolType, school::setSchoolType);
        changed |= setIfPresent(row.getRegion(), school::getRegion, school::setRegion);
        changed |= setIfPresent(row.getAddress(), school::getAddress, school::setAddress);
        changed |= setIfPresent(row.getContactName(), school::getContactName, school::setContactName);
        changed |= setIfPresent(row.getContactPhone(), school::getContactPhone, school::setContactPhone);
        changed |= setIfPresent(row.getContactEmail(), school::getContactEmail, school::setContactEmail);
        changed |= setIfPresent(row.getRemark(), school::getRemark, school::setRemark);
        String status = trimToNull(row.getStatus());
        if (StringUtils.isNotBlank(status)) {
            String normalizedStatus = normalizeStatus(status);
            if (!Objects.equals(school.getStatus(), normalizedStatus)) {
                school.setStatus(normalizedStatus);
                changed = true;
            }
        }
        return changed;
    }

    private boolean setIfPresent(String value, java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter) {
        String next = trimToNull(value);
        if (StringUtils.isBlank(next) || Objects.equals(getter.get(), next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    private void auditUnitStatus(String action, Long schoolId, String schoolName, String fromStatus, String toStatus) {
        if (Objects.equals(fromStatus, toStatus)) {
            return;
        }
        log.info(
            "[ART_AUDIT] action={}, operator={}, time={}, unitId={}, unitName={}, fromStatus={}, toStatus={}",
            action,
            currentUsername(),
            new Date(),
            schoolId,
            schoolName,
            fromStatus,
            toStatus
        );
    }

    private String currentUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String normalizeStatus(String status) {
        String value = trimToNull(status);
        if (StringUtils.isBlank(value)) {
            return STATUS_ENABLED;
        }
        value = switch (value) {
            case "启用", "已启用" -> "enabled";
            case "停用", "已停用" -> "disabled";
            case "回收站", "已回收" -> "recycled";
            default -> value;
        };
        if (!ALLOWED_STATUS.contains(value)) {
            throw new ServiceException("状态只支持启用、停用、回收站");
        }
        return value;
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private LambdaQueryWrapper<SchoolInfo> buildQuery(SchoolInfo query) {
        return Wrappers.lambdaQuery(SchoolInfo.class)
            .like(StringUtils.isNotBlank(query.getSchoolName()), SchoolInfo::getSchoolName, query.getSchoolName())
            .like(StringUtils.isNotBlank(query.getSchoolCode()), SchoolInfo::getSchoolCode, query.getSchoolCode())
            .eq(StringUtils.isNotBlank(query.getSchoolType()), SchoolInfo::getSchoolType, query.getSchoolType())
            .eq(StringUtils.isNotBlank(query.getStatus()), SchoolInfo::getStatus, query.getStatus())
            .orderByDesc(SchoolInfo::getCreateTime);
    }

    private SysDept ensureArtRootDept() {
        SysDept root = sysDeptMapper.selectOne(Wrappers.lambdaQuery(SysDept.class)
            .eq(SysDept::getDeptCategory, ART_ROOT_CATEGORY)
            .last("limit 1"));
        if (root != null) {
            if (!"艺术评审组织".equals(root.getDeptName()) || !"0".equals(root.getStatus())) {
                root.setDeptName("艺术评审组织");
                root.setStatus("0");
                sysDeptMapper.updateById(root);
            }
            return root;
        }
        root = new SysDept();
        root.setParentId(0L);
        root.setAncestors("0");
        root.setDeptName("艺术评审组织");
        root.setDeptCategory(ART_ROOT_CATEGORY);
        root.setOrderNum(90);
        root.setStatus("0");
        root.setDelFlag("0");
        sysDeptMapper.insert(root);
        return root;
    }

    private void syncSchoolDept(SchoolInfo school) {
        if (school == null || school.getId() == null || StringUtils.isBlank(school.getSchoolName())) {
            return;
        }
        SysDept root = ensureArtRootDept();
        String category = ART_SCHOOL_CATEGORY_PREFIX + school.getId();
        SysDept dept = sysDeptMapper.selectOne(Wrappers.lambdaQuery(SysDept.class)
            .eq(SysDept::getDeptCategory, category)
            .last("limit 1"));
        if (dept == null) {
            dept = new SysDept();
            dept.setParentId(root.getDeptId());
            dept.setAncestors(root.getAncestors() + "," + root.getDeptId());
            dept.setDeptCategory(category);
            dept.setOrderNum(1);
            dept.setDelFlag("0");
        }
        dept.setDeptName(school.getSchoolName());
        dept.setPhone(school.getContactPhone());
        dept.setEmail(school.getContactEmail());
        dept.setStatus(STATUS_ENABLED.equals(school.getStatus()) ? "0" : "1");
        if (dept.getDeptId() == null) {
            sysDeptMapper.insert(dept);
        } else {
            sysDeptMapper.updateById(dept);
        }
    }
}
