package org.dromara.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysWorkbenchLayout;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.bo.SysWorkbenchLayoutBo;
import org.dromara.system.domain.vo.SysWorkbenchAssetUploadVo;
import org.dromara.system.domain.vo.SysConfigVo;
import org.dromara.system.domain.vo.SysWorkbenchComponentVo;
import org.dromara.system.domain.vo.SysWorkbenchConfigImportPreviewVo;
import org.dromara.system.domain.vo.SysWorkbenchConfigPackageVo;
import org.dromara.system.domain.vo.SysWorkbenchLayoutVo;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysWorkbenchLayoutMapper;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysWorkbenchLayoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
public class SysWorkbenchLayoutServiceImpl implements ISysWorkbenchLayoutService {

    private static final String VISIBLE = "0";
    private static final String HIDDEN = "1";
    private static final String NAVBAR_TITLE_CONFIG_KEY = "crehn.workbench.navbar.title";
    private static final String DEFAULT_NAVBAR_TITLE_CONFIG = "{\"visible\":true,\"title\":\"\"}";
    private static final String STYLE_CONFIG_KEY = "crehn.workbench.style";
    private static final int STYLE_CONFIG_API_VERSION = 2;
    private static final String DEFAULT_STYLE_CONFIG = "{\"enabled\":true,\"preset\":\"sky\",\"pageBackground\":\"linear-gradient(180deg,#f5f9ff 0%,#f8fbff 44%,#ffffff 100%)\",\"cardBackground\":\"#ffffff\",\"cardBorder\":\"#d8e6f5\",\"accentColor\":\"#2563eb\",\"headingColor\":\"#0f2f5f\",\"calendarBackground\":\"linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)\",\"shadow\":\"0 4px 14px rgba(37,99,235,0.05)\",\"sBg\":\"#f4f8ff\",\"sBl\":0,\"sIt\":\"transparent\",\"sHv\":\"#e7f0ff\",\"sBd\":\"#d9e6f6\"}";
    private static final String SECURITY_REMINDER_CONFIG_KEY = "crehn.workbench.security.reminder";
    private static final String ROLE_SHELL_CONFIG_KEY_PREFIX = "crehn.workbench.role.shell.";
    private static final String DEFAULT_SECURITY_REMINDER_CONFIG = "{\"enabled\":true,\"checkDefaultPassword\":true,\"checkMissingPhone\":true,\"checkMissingEmail\":true}";
    private static final String AUDIT_WORKBENCH_COMPONENT_KEY = "audit_workbench";
    private static final String REVIEW_WORKBENCH_COMPONENT_KEY = "review_workbench";
    private static final String PROJECT_SUBMIT_COMPONENT_KEY = "school_project_list";
    private static final int STYLE_CONFIG_MAX_LENGTH = 4000;
    private static final String CONFIG_PACKAGE_VERSION = "1.0";
    private static final String CONFIG_PACKAGE_MANIFEST = "workbench-config.json";
    private static final long CONFIG_PACKAGE_MAX_SIZE = 50L * 1024 * 1024;
    private static final long CONFIG_PACKAGE_MANIFEST_MAX_SIZE = 5L * 1024 * 1024;
    private static final int CONFIG_PACKAGE_MAX_ENTRIES = 200;
    private static final Pattern WORKBENCH_ASSET_KEY_PATTERN = Pattern.compile("\\d{4}/\\d{2}/[a-f0-9]{32}\\.(?:png|jpg|jpeg|webp)");
    private static final long WORKBENCH_ASSET_MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> WORKBENCH_ASSET_CONTENT_TYPES = Map.of(
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "webp", "image/webp"
    );
    private static final Set<String> WORKBENCH_ASSET_ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/pjpeg",
        "image/webp",
        "application/octet-stream"
    );

    private final SysWorkbenchLayoutMapper workbenchLayoutMapper;
    private final SysRoleMapper roleMapper;
    private final ISysRoleService roleService;
    private final ISysConfigService configService;

    @Value("${crehn.workbench.asset-dir:}")
    private String workbenchAssetDir;

    private static final Map<String, SysWorkbenchComponentVo> COMPONENTS = buildComponents();

    @Override
    public List<SysWorkbenchComponentVo> componentOptions(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>(COMPONENTS.values());
        }
        SysRole role = requireRole(roleId);
        return COMPONENTS.values().stream()
            .filter(component -> isAllowedForRole(component, role.getRoleKey()))
            .toList();
    }

    @Override
    public List<SysWorkbenchLayoutVo> queryCurrentLayout() {
        RoleDTO role = resolveCurrentRole();
        String roleKey = role == null ? "" : role.getRoleKey();
        Long roleId = role == null ? null : role.getRoleId();
        return materializeLayout(roleId, roleKey).stream()
            .filter(item -> VISIBLE.equals(item.getVisible()))
            .filter(this::hasComponentPermission)
            .toList();
    }

    @Override
    public SysWorkbenchLayoutVo queryCurrentComponent(String componentKey) {
        RoleDTO role = resolveCurrentRole();
        String roleKey = role == null ? "" : role.getRoleKey();
        Long roleId = role == null ? null : role.getRoleId();
        String key = normalize(componentKey);
        SysWorkbenchComponentVo component = COMPONENTS.get(key);
        if (component == null || !isAllowedForRole(component, roleKey)) {
            throw new ServiceException("component is not allowed for current role");
        }
        return materializeLayout(roleId, roleKey).stream()
            .filter(item -> key.equals(item.getComponentKey()))
            .filter(this::hasComponentPermission)
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<SysWorkbenchLayoutVo> queryRoleLayout(Long roleId) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        return materializeLayout(roleId, role.getRoleKey());
    }

    @Override
    public Map<String, Object> queryCurrentRoleShellConfig() {
        RoleDTO role = resolveCurrentRole();
        if (role == null || role.getRoleId() == null) {
            return normalizeRoleShellConfig(Map.of(), "", false);
        }
        return readRoleShellConfig(role.getRoleId(), role.getRoleKey());
    }

    @Override
    public Map<String, Object> queryRoleShellConfig(Long roleId) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        return readRoleShellConfig(roleId, role.getRoleKey());
    }

    @Override
    public Boolean saveRoleShellConfig(Long roleId, Map<String, Object> config) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        boolean showUserAvatar = booleanValue(config.get("showUserAvatar"), true);
        String userDisplayMode = String.valueOf(config.getOrDefault("userDisplayMode", "nickname")).trim().toLowerCase(Locale.ROOT);
        if (!showUserAvatar && "hidden".equals(userDisplayMode)) {
            throw new ServiceException("右上角头像和账号名称至少需要显示一个");
        }
        Map<String, Object> normalized = normalizeRoleShellConfig(config, role.getRoleKey(), true);
        normalized.remove("configured");
        String configJson = JsonUtils.toJsonString(normalized);
        if (configJson.length() > 1000) {
            throw new ServiceException("角色系统布局配置过长，请控制在 1000 字符以内");
        }
        saveRoleShellConfigValue(roleId, role, configJson);
        return true;
    }

    @Override
    public Boolean restoreRoleShellConfig(Long roleId) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        Map<String, Object> defaultSource = new LinkedHashMap<>();
        defaultSource.put("navbarTitleVisible", true);
        defaultSource.put("navbarTitle", "");
        Map<String, Object> defaults = normalizeRoleShellConfig(defaultSource, role.getRoleKey(), true);
        defaults.remove("configured");
        saveRoleShellConfigValue(roleId, role, JsonUtils.toJsonString(defaults));
        return true;
    }

    @Override
    public String queryNavbarTitleConfig() {
        return queryExactConfigValue(NAVBAR_TITLE_CONFIG_KEY, DEFAULT_NAVBAR_TITLE_CONFIG);
    }

    @Override
    public Boolean saveNavbarTitleConfig(String configJson) {
        SysConfigBo bo = new SysConfigBo();
        bo.setConfigName("艺术评审-工作台顶部单位名称");
        bo.setConfigKey(NAVBAR_TITLE_CONFIG_KEY);
        bo.setConfigValue(configJson == null || configJson.isBlank() ? DEFAULT_NAVBAR_TITLE_CONFIG : configJson);
        bo.setConfigType("Y");
        bo.setRemark("工作台顶部居中单位名称配置，JSON：visible/title");

        saveExactConfig(bo);
        return true;
    }

    @Override
    public String queryStyleConfig() {
        return queryExactConfigValue(STYLE_CONFIG_KEY, DEFAULT_STYLE_CONFIG);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveStyleConfig(String configJson) {
        String normalizedConfigJson = normalizeStyleConfig(configJson);
        List<SysConfigVo> existingStyleConfigs = findExactConfigs(STYLE_CONFIG_KEY);
        if (existingStyleConfigs.size() > 1) {
            throw new ServiceException("检测到重复的工作台样式配置，请先检查 sys_config 中的 crehn.workbench.style");
        }
        SysConfigBo bo = new SysConfigBo();
        bo.setConfigName("Art review workbench style");
        bo.setConfigKey(STYLE_CONFIG_KEY);
        bo.setConfigValue(normalizedConfigJson);
        bo.setConfigType("Y");
        bo.setRemark("Workbench home background/card/sidebar style config JSON");

        saveExactConfig(bo);
        String persistedConfigJson = queryExactConfigValue(STYLE_CONFIG_KEY, DEFAULT_STYLE_CONFIG);
        if (!JsonUtils.isJsonObject(persistedConfigJson)
            || !Objects.equals(parseJsonMap(normalizedConfigJson), parseJsonMap(persistedConfigJson))) {
            throw new ServiceException("工作台样式未完整写入配置存储，请检查 sys_config.config_value 字段长度和重复配置");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiVersion", STYLE_CONFIG_API_VERSION);
        result.put("config", parseJsonMap(persistedConfigJson));
        return result;
    }

    @Override
    public String querySecurityReminderConfig() {
        return queryExactConfigValue(SECURITY_REMINDER_CONFIG_KEY, DEFAULT_SECURITY_REMINDER_CONFIG);
    }

    @Override
    public Boolean saveSecurityReminderConfig(String configJson) {
        String normalizedConfigJson = normalizeSecurityReminderConfig(configJson);
        SysConfigBo bo = new SysConfigBo();
        bo.setConfigName("Art review workbench security reminder");
        bo.setConfigKey(SECURITY_REMINDER_CONFIG_KEY);
        bo.setConfigValue(normalizedConfigJson);
        bo.setConfigType("Y");
        bo.setRemark("Workbench account security reminder config JSON");

        saveExactConfig(bo);
        return true;
    }

    @Override
    public SysWorkbenchAssetUploadVo uploadWorkbenchAsset(MultipartFile file) {
        validateWorkbenchAsset(file);
        String originalName = safeAssetOriginalName(file.getOriginalFilename());
        String extension = assetExtension(originalName);
        String month = new SimpleDateFormat("yyyy/MM").format(new Date());
        String generatedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String fileKey = month + "/" + generatedName;
        Path target = resolveWorkbenchAssetPath(fileKey, false);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ServiceException("工作台图片保存失败");
        }
        SysWorkbenchAssetUploadVo vo = new SysWorkbenchAssetUploadVo();
        vo.setFileKey(fileKey);
        vo.setUrl("/system/workbench/asset?key=" + fileKey);
        vo.setOriginalName(originalName);
        vo.setFileName(generatedName);
        vo.setSize(file.getSize());
        vo.setContentType(contentTypeForExtension(extension));
        return vo;
    }

    @Override
    public Path resolveWorkbenchAssetPath(String fileKey) {
        return resolveWorkbenchAssetPath(fileKey, true);
    }

    @Override
    public String resolveWorkbenchAssetContentType(String fileKey) {
        return contentTypeForExtension(assetExtension(fileKey));
    }

    @Override
    public Boolean deleteWorkbenchAsset(String fileKey) {
        Path path = resolveWorkbenchAssetPath(fileKey, false);
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            throw new ServiceException("工作台图片删除失败");
        }
    }

    @Override
    public byte[] exportConfigPackage() {
        SysWorkbenchConfigPackageVo configPackage = buildConfigPackage();
        Set<String> assetKeys = collectPackageAssetKeys(configPackage);
        Map<String, byte[]> assets = new LinkedHashMap<>();
        for (String assetKey : assetKeys) {
            Path path = resolveWorkbenchAssetPath(assetKey, true);
            if (path == null) {
                throw new ServiceException("工作台配置引用的图片不存在：" + assetKey);
            }
            try {
                byte[] content = Files.readAllBytes(path);
                validatePackageAsset(assetKey, content);
                assets.put(assetKey, content);
            } catch (IOException e) {
                throw new ServiceException("读取工作台配置图片失败：" + assetKey);
            }
        }
        configPackage.setAssetKeys(new ArrayList<>(assets.keySet()));
        byte[] manifest = JsonUtils.toJsonString(configPackage).getBytes(StandardCharsets.UTF_8);
        long packageContentSize = manifest.length + assets.values().stream().mapToLong(content -> content.length).sum();
        if (manifest.length > CONFIG_PACKAGE_MANIFEST_MAX_SIZE
            || assets.size() + 1 > CONFIG_PACKAGE_MAX_ENTRIES
            || packageContentSize > CONFIG_PACKAGE_MAX_SIZE) {
            throw new ServiceException("工作台配置包内容超过 50MB 或文件数量上限");
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            writeZipEntry(zip, CONFIG_PACKAGE_MANIFEST, manifest);
            for (Map.Entry<String, byte[]> asset : assets.entrySet()) {
                writeZipEntry(zip, "assets/" + asset.getKey(), asset.getValue());
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("生成工作台配置包失败");
        }
    }

    @Override
    public SysWorkbenchConfigImportPreviewVo previewConfigPackage(MultipartFile file) {
        ParsedConfigPackage parsed = readConfigPackage(file);
        return buildImportPreview(parsed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean importConfigPackage(MultipartFile file) {
        ParsedConfigPackage parsed = readConfigPackage(file);
        SysWorkbenchConfigPackageVo configPackage = parsed.configPackage();
        Map<String, SysRole> targetRoles = uniqueRolesByKey();
        Set<String> requiredAssets = new HashSet<>();
        for (SysWorkbenchConfigPackageVo.RoleConfig roleConfig : configPackage.getRoles()) {
            if (targetRoles.containsKey(normalize(roleConfig.getRoleKey()))) {
                collectAssetKeys(roleConfig.getShell(), requiredAssets);
                collectAssetKeys(roleConfig.getLayouts(), requiredAssets);
            }
        }

        List<Path> createdAssets = new ArrayList<>();
        try {
            Map<String, String> assetKeyMapping = storeImportedAssets(parsed.assets(), requiredAssets, createdAssets);
            saveStyleConfig(JsonUtils.toJsonString(configPackage.getStyle()));
            for (SysWorkbenchConfigPackageVo.RoleConfig roleConfig : configPackage.getRoles()) {
                SysRole targetRole = targetRoles.get(normalize(roleConfig.getRoleKey()));
                if (targetRole == null) {
                    continue;
                }
                roleService.checkRoleDataScope(targetRole.getRoleId());
                saveRoleShellConfig(targetRole.getRoleId(), remapShellAssets(roleConfig.getShell(), assetKeyMapping));
                List<SysWorkbenchLayoutBo> layouts = roleConfig.getLayouts().stream()
                    .map(layout -> toLayoutBo(layout, assetKeyMapping))
                    .toList();
                saveRoleLayout(targetRole.getRoleId(), layouts);
            }
            return true;
        } catch (RuntimeException e) {
            cleanupCreatedAssets(createdAssets);
            throw e;
        }
    }

    private SysWorkbenchConfigPackageVo buildConfigPackage() {
        SysWorkbenchConfigPackageVo configPackage = new SysWorkbenchConfigPackageVo();
        configPackage.setPackageVersion(CONFIG_PACKAGE_VERSION);
        configPackage.setExportedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        configPackage.setStyle(parseJsonMap(queryStyleConfig()));
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
            .orderByAsc(SysRole::getRoleSort)
            .orderByAsc(SysRole::getRoleId));
        List<SysWorkbenchConfigPackageVo.RoleConfig> roleConfigs = new ArrayList<>();
        for (SysRole role : roles) {
            if (role.getRoleKey() == null || role.getRoleKey().isBlank()) {
                continue;
            }
            Map<String, Object> shell = new LinkedHashMap<>(readRoleShellConfig(role.getRoleId(), role.getRoleKey()));
            boolean configured = Boolean.TRUE.equals(shell.remove("configured"));
            SysWorkbenchConfigPackageVo.RoleConfig roleConfig = new SysWorkbenchConfigPackageVo.RoleConfig();
            roleConfig.setRoleKey(role.getRoleKey());
            roleConfig.setRoleName(role.getRoleName());
            roleConfig.setShellConfigured(configured);
            roleConfig.setShell(shell);
            roleConfig.setLayouts(materializeLayout(role.getRoleId(), role.getRoleKey()).stream().map(this::toPackageLayout).toList());
            roleConfigs.add(roleConfig);
        }
        configPackage.setRoles(roleConfigs);
        return configPackage;
    }

    private SysWorkbenchConfigPackageVo.LayoutConfig toPackageLayout(SysWorkbenchLayoutVo layout) {
        SysWorkbenchConfigPackageVo.LayoutConfig item = new SysWorkbenchConfigPackageVo.LayoutConfig();
        item.setComponentKey(layout.getComponentKey());
        item.setTitle(layout.getTitle());
        item.setWidth(layout.getWidth());
        item.setSortOrder(layout.getSortOrder());
        item.setVisible(layout.getVisible());
        item.setConfigJson(layout.getConfigJson());
        return item;
    }

    private SysWorkbenchLayoutBo toLayoutBo(SysWorkbenchConfigPackageVo.LayoutConfig layout, Map<String, String> assetKeyMapping) {
        SysWorkbenchLayoutBo bo = new SysWorkbenchLayoutBo();
        bo.setComponentKey(layout.getComponentKey());
        bo.setTitle(layout.getTitle());
        bo.setWidth(layout.getWidth());
        bo.setSortOrder(layout.getSortOrder());
        bo.setVisible(layout.getVisible());
        String configJson = layout.getConfigJson();
        if (configJson != null) {
            for (Map.Entry<String, String> mapping : assetKeyMapping.entrySet()) {
                configJson = configJson.replace(mapping.getKey(), mapping.getValue());
            }
        }
        bo.setConfigJson(configJson);
        return bo;
    }

    private SysWorkbenchConfigImportPreviewVo buildImportPreview(ParsedConfigPackage parsed) {
        SysWorkbenchConfigPackageVo configPackage = parsed.configPackage();
        SysWorkbenchConfigImportPreviewVo preview = new SysWorkbenchConfigImportPreviewVo();
        preview.setPackageVersion(configPackage.getPackageVersion());
        preview.setExportedAt(configPackage.getExportedAt());
        addMapDifferences(preview, "style", null, null, "工作台样式", parseJsonMap(queryStyleConfig()), configPackage.getStyle(), true);

        Map<String, SysRole> targetRoles = uniqueRolesByKey();
        Set<String> requiredAssets = new TreeSet<>();
        for (SysWorkbenchConfigPackageVo.RoleConfig importedRole : configPackage.getRoles()) {
            String roleKey = normalize(importedRole.getRoleKey());
            SysRole targetRole = targetRoles.get(roleKey);
            if (targetRole == null) {
                addDifference(preview, "role", importedRole.getRoleKey(), importedRole.getRoleName(), "角色", "skipped", null,
                    "目标环境缺少同 roleKey 角色", false);
                continue;
            }
            collectAssetKeys(importedRole.getShell(), requiredAssets);
            collectAssetKeys(importedRole.getLayouts(), requiredAssets);
            int changedBefore = preview.getChangedCount();
            Map<String, Object> currentShell = new LinkedHashMap<>(readRoleShellConfig(targetRole.getRoleId(), targetRole.getRoleKey()));
            currentShell.remove("configured");
            addMapDifferences(preview, "shell", importedRole.getRoleKey(), targetRole.getRoleName(), "角色系统布局", currentShell,
                importedRole.getShell(), true);
            addLayoutDifferences(preview, targetRole, importedRole);
            if (preview.getChangedCount() == changedBefore) {
                addDifference(preview, "role", importedRole.getRoleKey(), targetRole.getRoleName(), "全部配置", "unchanged", "无变化", "无变化", true);
            }
        }
        addAssetDifferences(preview, parsed.assets(), requiredAssets);
        return preview;
    }

    private void addAssetDifferences(SysWorkbenchConfigImportPreviewVo preview, Map<String, byte[]> assets, Set<String> requiredAssets) {
        for (String assetKey : requiredAssets) {
            byte[] imported = assets.get(assetKey);
            if (imported == null) {
                continue;
            }
            Path currentPath = resolveWorkbenchAssetPath(assetKey, true);
            if (currentPath == null) {
                addDifference(preview, "asset", null, null, "工作台图片 / " + assetKey, "add", null, "随配置包导入", true);
                continue;
            }
            try {
                if (Arrays.equals(Files.readAllBytes(currentPath), imported)) {
                    preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                } else {
                    addDifference(preview, "asset", null, null, "工作台图片 / " + assetKey, "modify", "目标环境同名图片内容不同",
                        "导入为新文件并自动改写引用", true);
                }
            } catch (IOException e) {
                throw new ServiceException("读取目标工作台图片失败：" + assetKey);
            }
        }
    }

    private void addLayoutDifferences(SysWorkbenchConfigImportPreviewVo preview, SysRole targetRole,
                                      SysWorkbenchConfigPackageVo.RoleConfig importedRole) {
        Map<String, SysWorkbenchConfigPackageVo.LayoutConfig> current = new LinkedHashMap<>();
        for (SysWorkbenchLayoutVo item : materializeLayout(targetRole.getRoleId(), targetRole.getRoleKey())) {
            SysWorkbenchConfigPackageVo.LayoutConfig packageItem = toPackageLayout(item);
            current.put(packageItem.getComponentKey(), packageItem);
        }
        Map<String, SysWorkbenchConfigPackageVo.LayoutConfig> imported = new LinkedHashMap<>();
        for (SysWorkbenchConfigPackageVo.LayoutConfig item : importedRole.getLayouts()) {
            imported.put(item.getComponentKey(), item);
        }
        Set<String> keys = new TreeSet<>();
        keys.addAll(current.keySet());
        keys.addAll(imported.keySet());
        for (String key : keys) {
            SysWorkbenchConfigPackageVo.LayoutConfig currentItem = current.get(key);
            SysWorkbenchConfigPackageVo.LayoutConfig importedItem = imported.get(key);
            String basePath = "首页布局 / " + key;
            if (currentItem == null) {
                addDifference(preview, "layout", importedRole.getRoleKey(), targetRole.getRoleName(), basePath, "add", null, "新增组件", true);
                continue;
            }
            if (importedItem == null) {
                addDifference(preview, "layout", importedRole.getRoleKey(), targetRole.getRoleName(), basePath, "remove", "现有组件", null, true);
                continue;
            }
            Map<String, Object> currentFields = layoutFields(currentItem);
            Map<String, Object> importedFields = layoutFields(importedItem);
            addMapDifferences(preview, "layout", importedRole.getRoleKey(), targetRole.getRoleName(), basePath,
                currentFields, importedFields, true);
        }
    }

    private Map<String, Object> layoutFields(SysWorkbenchConfigPackageVo.LayoutConfig item) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("标题", item.getTitle());
        values.put("宽度", item.getWidth());
        values.put("顺序", item.getSortOrder());
        values.put("显示状态", item.getVisible());
        if (item.getConfigJson() == null || item.getConfigJson().isBlank()) {
            values.put("组件配置", null);
        } else if (JsonUtils.isJsonObject(item.getConfigJson())) {
            values.put("组件配置", parseJsonMap(item.getConfigJson()));
        } else {
            values.put("组件配置", item.getConfigJson());
        }
        return values;
    }

    private void addMapDifferences(SysWorkbenchConfigImportPreviewVo preview, String scope, String roleKey, String roleName,
                                   String basePath, Map<String, Object> current, Map<String, Object> imported, boolean importable) {
        Map<String, String> currentFlat = flattenMap(current);
        Map<String, String> importedFlat = flattenMap(imported);
        Set<String> paths = new TreeSet<>();
        paths.addAll(currentFlat.keySet());
        paths.addAll(importedFlat.keySet());
        for (String path : paths) {
            boolean hasCurrent = currentFlat.containsKey(path);
            boolean hasImported = importedFlat.containsKey(path);
            String currentValue = currentFlat.get(path);
            String importedValue = importedFlat.get(path);
            if (hasCurrent && hasImported && Objects.equals(currentValue, importedValue)) {
                preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                continue;
            }
            String changeType = !hasCurrent ? "add" : !hasImported ? "remove" : "modify";
            addDifference(preview, scope, roleKey, roleName, basePath + (path.isBlank() ? "" : " / " + path), changeType,
                currentValue, importedValue, importable);
        }
    }

    private void addDifference(SysWorkbenchConfigImportPreviewVo preview, String scope, String roleKey, String roleName,
                               String path, String changeType, String currentValue, String importedValue, boolean importable) {
        preview.getDifferences().add(new SysWorkbenchConfigImportPreviewVo.DiffItem(scope, roleKey, roleName, path, changeType,
            abbreviate(currentValue), abbreviate(importedValue), importable));
        if ("skipped".equals(changeType)) {
            preview.setSkippedCount(preview.getSkippedCount() + 1);
        } else if ("unchanged".equals(changeType)) {
            preview.setUnchangedCount(preview.getUnchangedCount() + 1);
        } else {
            preview.setChangedCount(preview.getChangedCount() + 1);
        }
    }

    private Map<String, String> flattenMap(Map<String, Object> source) {
        Map<String, String> result = new LinkedHashMap<>();
        flattenValue("", source == null ? Map.of() : source, result);
        return result;
    }

    private void flattenValue(String path, Object value, Map<String, String> result) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty() && !path.isBlank()) {
                result.put(path, "{}");
                return;
            }
            map.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> flattenValue(path.isBlank() ? String.valueOf(entry.getKey()) : path + " / " + entry.getKey(), entry.getValue(), result));
            return;
        }
        if (value instanceof List<?> list) {
            result.put(path, JsonUtils.toJsonString(list));
            return;
        }
        result.put(path, value == null ? null : String.valueOf(value));
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 497) + "...";
    }

    private Map<String, SysRole> uniqueRolesByKey() {
        Map<String, List<SysRole>> grouped = new HashMap<>();
        for (SysRole role : roleMapper.selectList(new LambdaQueryWrapper<SysRole>())) {
            grouped.computeIfAbsent(normalize(role.getRoleKey()), ignored -> new ArrayList<>()).add(role);
        }
        Map<String, SysRole> result = new HashMap<>();
        grouped.forEach((key, roles) -> {
            if (!key.isBlank() && roles.size() == 1) {
                result.put(key, roles.get(0));
            }
        });
        return result;
    }

    private SysWorkbenchConfigPackageVo.LayoutConfig validateLayoutConfig(SysWorkbenchConfigPackageVo.LayoutConfig layout,
                                                                           Set<String> componentKeys) {
        if (layout == null || layout.getComponentKey() == null || !COMPONENTS.containsKey(layout.getComponentKey())) {
            throw new ServiceException("配置包包含未知工作台组件");
        }
        if (!componentKeys.add(layout.getComponentKey())) {
            throw new ServiceException("配置包角色布局包含重复组件：" + layout.getComponentKey());
        }
        if (layout.getConfigJson() != null && layout.getConfigJson().length() > CONFIG_PACKAGE_MANIFEST_MAX_SIZE) {
            throw new ServiceException("配置包组件配置过长：" + layout.getComponentKey());
        }
        return layout;
    }

    private ParsedConfigPackage readConfigPackage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择工作台配置包");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".zip")) {
            throw new ServiceException("仅支持 ZIP 工作台配置包");
        }
        if (file.getSize() > CONFIG_PACKAGE_MAX_SIZE) {
            throw new ServiceException("工作台配置包不能超过 50MB");
        }
        byte[] manifest = null;
        Map<String, byte[]> assets = new LinkedHashMap<>();
        Set<String> entryNames = new HashSet<>();
        long totalSize = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entryCount++;
                if (entryCount > CONFIG_PACKAGE_MAX_ENTRIES) {
                    throw new ServiceException("工作台配置包文件数量过多");
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("../") || !entryNames.add(name)) {
                    throw new ServiceException("工作台配置包包含非法或重复路径");
                }
                long limit = CONFIG_PACKAGE_MANIFEST.equals(name) ? CONFIG_PACKAGE_MANIFEST_MAX_SIZE : WORKBENCH_ASSET_MAX_SIZE;
                byte[] content = readZipEntry(zip, limit);
                totalSize += content.length;
                if (totalSize > CONFIG_PACKAGE_MAX_SIZE) {
                    throw new ServiceException("工作台配置包解压后内容过大");
                }
                if (CONFIG_PACKAGE_MANIFEST.equals(name)) {
                    manifest = content;
                } else if (name.startsWith("assets/")) {
                    String assetKey = name.substring("assets/".length());
                    resolveWorkbenchAssetPath(assetKey, false);
                    validatePackageAsset(assetKey, content);
                    assets.put(assetKey, content);
                } else {
                    throw new ServiceException("工作台配置包包含未知文件：" + name);
                }
            }
        } catch (IOException e) {
            throw new ServiceException("工作台配置包读取失败");
        }
        if (manifest == null) {
            throw new ServiceException("工作台配置包缺少 " + CONFIG_PACKAGE_MANIFEST);
        }
        SysWorkbenchConfigPackageVo configPackage;
        try {
            configPackage = JsonUtils.parseObject(manifest, SysWorkbenchConfigPackageVo.class);
        } catch (RuntimeException e) {
            throw new ServiceException("工作台配置包清单不是有效 JSON");
        }
        validateConfigPackage(configPackage, assets);
        return new ParsedConfigPackage(configPackage, assets);
    }

    private void validateConfigPackage(SysWorkbenchConfigPackageVo configPackage, Map<String, byte[]> assets) {
        if (configPackage == null || !CONFIG_PACKAGE_VERSION.equals(configPackage.getPackageVersion())) {
            throw new ServiceException("不支持的工作台配置包版本");
        }
        if (configPackage.getStyle() == null || configPackage.getRoles() == null || configPackage.getAssetKeys() == null) {
            throw new ServiceException("工作台配置包结构不完整");
        }
        normalizeStyleConfig(JsonUtils.toJsonString(configPackage.getStyle()));
        Set<String> roleKeys = new HashSet<>();
        Set<String> referencedAssets = new HashSet<>();
        for (SysWorkbenchConfigPackageVo.RoleConfig role : configPackage.getRoles()) {
            String roleKey = role == null ? "" : normalize(role.getRoleKey());
            if (roleKey.isBlank() || !roleKeys.add(roleKey) || role.getShell() == null || role.getLayouts() == null) {
                throw new ServiceException("工作台配置包包含无效或重复角色");
            }
            Set<String> componentKeys = new HashSet<>();
            role.getLayouts().forEach(layout -> validateLayoutConfig(layout, componentKeys));
            collectAssetKeys(role.getShell(), referencedAssets);
            collectAssetKeys(role.getLayouts(), referencedAssets);
        }
        Set<String> declaredAssets = new HashSet<>(configPackage.getAssetKeys());
        if (declaredAssets.size() != configPackage.getAssetKeys().size()
            || !declaredAssets.equals(assets.keySet())
            || !declaredAssets.equals(referencedAssets)) {
            throw new ServiceException("工作台配置包图片清单与文件不一致");
        }
    }

    private byte[] readZipEntry(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if ((long) output.size() + read > limit) {
                throw new ServiceException("工作台配置包内文件过大");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private Set<String> collectPackageAssetKeys(SysWorkbenchConfigPackageVo configPackage) {
        Set<String> keys = new TreeSet<>();
        for (SysWorkbenchConfigPackageVo.RoleConfig role : configPackage.getRoles()) {
            collectAssetKeys(role.getShell(), keys);
            collectAssetKeys(role.getLayouts(), keys);
        }
        return keys;
    }

    private void collectAssetKeys(Map<String, Object> shell, Set<String> target) {
        if (shell == null || !"asset".equals(normalize(String.valueOf(shell.get("navbarTitleLogoMode"))))) {
            return;
        }
        Object rawAssetKey = shell.get("navbarTitleLogoAssetKey");
        String assetKey = trimToEmpty(rawAssetKey == null ? "" : String.valueOf(rawAssetKey));
        if (WORKBENCH_ASSET_KEY_PATTERN.matcher(assetKey).matches()) {
            target.add(assetKey);
        }
    }

    private void collectAssetKeys(List<SysWorkbenchConfigPackageVo.LayoutConfig> layouts, Set<String> target) {
        for (SysWorkbenchConfigPackageVo.LayoutConfig layout : layouts) {
            if (layout.getConfigJson() == null) {
                continue;
            }
            Matcher matcher = WORKBENCH_ASSET_KEY_PATTERN.matcher(layout.getConfigJson());
            while (matcher.find()) {
                target.add(matcher.group());
            }
        }
    }

    private Map<String, Object> remapShellAssets(Map<String, Object> shell, Map<String, String> assetKeyMapping) {
        Map<String, Object> remapped = new LinkedHashMap<>(shell == null ? Map.of() : shell);
        Object rawSourceKey = remapped.get("navbarTitleLogoAssetKey");
        String sourceKey = trimToEmpty(rawSourceKey == null ? "" : String.valueOf(rawSourceKey));
        if (!sourceKey.isBlank() && assetKeyMapping.containsKey(sourceKey)) {
            remapped.put("navbarTitleLogoAssetKey", assetKeyMapping.get(sourceKey));
        }
        return remapped;
    }

    private void validatePackageAsset(String assetKey, byte[] content) {
        if (content == null || content.length == 0 || content.length > WORKBENCH_ASSET_MAX_SIZE) {
            throw new ServiceException("工作台配置图片大小不合法：" + assetKey);
        }
        String extension = assetExtension(assetKey);
        if (!hasValidAssetHeader(extension, Arrays.copyOf(content, Math.min(12, content.length)))) {
            throw new ServiceException("工作台配置图片内容不合法：" + assetKey);
        }
    }

    private Map<String, String> storeImportedAssets(Map<String, byte[]> assets, Set<String> requiredAssets, List<Path> createdAssets) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String sourceKey : requiredAssets) {
            byte[] content = assets.get(sourceKey);
            if (content == null) {
                Path existing = resolveWorkbenchAssetPath(sourceKey, true);
                if (existing == null) {
                    throw new ServiceException("配置引用的工作台图片未包含在配置包中：" + sourceKey);
                }
                continue;
            }
            validatePackageAsset(sourceKey, content);
            String targetKey = sourceKey;
            Path target = resolveWorkbenchAssetPath(targetKey, false);
            try {
                if (Files.isRegularFile(target)) {
                    if (Arrays.equals(Files.readAllBytes(target), content)) {
                        continue;
                    }
                    String extension = assetExtension(sourceKey);
                    targetKey = new SimpleDateFormat("yyyy/MM").format(new Date()) + "/"
                        + UUID.randomUUID().toString().replace("-", "") + "." + extension;
                    target = resolveWorkbenchAssetPath(targetKey, false);
                    mapping.put(sourceKey, targetKey);
                }
                Files.createDirectories(target.getParent());
                Files.write(target, content, StandardOpenOption.CREATE_NEW);
                createdAssets.add(target);
            } catch (IOException e) {
                throw new ServiceException("导入工作台配置图片失败：" + sourceKey);
            }
        }
        return mapping;
    }

    private void cleanupCreatedAssets(List<Path> createdAssets) {
        for (Path path : createdAssets) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Database transaction still rolls back; retain the original failure.
            }
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = JsonUtils.parseObject(json, Map.class);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (RuntimeException e) {
            throw new ServiceException("工作台配置 JSON 解析失败");
        }
    }

    private record ParsedConfigPackage(SysWorkbenchConfigPackageVo configPackage, Map<String, byte[]> assets) {
    }

    private String queryExactConfigValue(String configKey, String defaultValue) {
        return findExactConfigs(configKey).stream()
            .max(Comparator.comparing(SysConfigVo::getConfigId, Comparator.nullsFirst(Long::compareTo)))
            .map(SysConfigVo::getConfigValue)
            .filter(value -> value != null && !value.isBlank())
            .orElse(defaultValue);
    }

    private Map<String, Object> readRoleShellConfig(Long roleId, String roleKey) {
        String configKey = roleShellConfigKey(roleId);
        List<SysConfigVo> configs = findExactConfigs(configKey);
        SysConfigVo latest = configs.stream()
            .max(Comparator.comparing(SysConfigVo::getConfigId, Comparator.nullsFirst(Long::compareTo)))
            .orElse(null);
        if (latest == null || latest.getConfigValue() == null || latest.getConfigValue().isBlank()) {
            return normalizeRoleShellConfig(Map.of(), roleKey, false);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = JsonUtils.parseObject(latest.getConfigValue(), Map.class);
            return normalizeRoleShellConfig(parsed == null ? Map.of() : parsed, roleKey, true);
        } catch (RuntimeException ignored) {
            return normalizeRoleShellConfig(Map.of(), roleKey, false);
        }
    }

    private void saveRoleShellConfigValue(Long roleId, SysRole role, String configJson) {
        SysConfigBo bo = new SysConfigBo();
        bo.setConfigName("工作台-角色系统布局-" + blankToDefault(role.getRoleName(), String.valueOf(roleId)));
        bo.setConfigKey(roleShellConfigKey(roleId));
        bo.setConfigValue(configJson);
        bo.setConfigType("Y");
        bo.setRemark("管理员按角色统一下发的导航、主题、圆角、顶部单位名称、页面路径和首页导航显隐配置");
        saveExactConfig(bo);
    }

    private String roleShellConfigKey(Long roleId) {
        if (roleId == null) {
            throw new ServiceException("角色不能为空");
        }
        return ROLE_SHELL_CONFIG_KEY_PREFIX + roleId;
    }

    private Map<String, Object> normalizeRoleShellConfig(Map<String, Object> source, String roleKey, boolean configured) {
        Map<String, Object> defaults = defaultRoleShellConfig(roleKey);
        Map<String, Object> normalized = new LinkedHashMap<>();
        String navType = String.valueOf(source.getOrDefault("navType", defaults.get("navType"))).trim().toLowerCase(Locale.ROOT);
        normalized.put("navType", Set.of("left", "mix", "top").contains(navType) ? navType : defaults.get("navType"));
        String theme = String.valueOf(source.getOrDefault("theme", defaults.get("theme"))).trim();
        normalized.put("theme", theme.matches("^#[0-9a-fA-F]{6}$") ? theme : defaults.get("theme"));
        normalized.put("radiusBase", boundedInteger(source.get("radiusBase"), (Integer) defaults.get("radiusBase"), 0, 24));
        normalized.put("tagsView", booleanValue(source.get("tagsView"), (Boolean) defaults.get("tagsView")));
        normalized.put("tagsIcon", booleanValue(source.get("tagsIcon"), (Boolean) defaults.get("tagsIcon")));
        normalized.put("fixedHeader", booleanValue(source.get("fixedHeader"), (Boolean) defaults.get("fixedHeader")));
        normalized.put("sidebarLogo", booleanValue(source.get("sidebarLogo"), (Boolean) defaults.get("sidebarLogo")));
        normalized.put("dynamicTitle", booleanValue(source.get("dynamicTitle"), (Boolean) defaults.get("dynamicTitle")));
        normalized.put("breadcrumbVisible", booleanValue(source.get("breadcrumbVisible"), (Boolean) defaults.get("breadcrumbVisible")));
        normalized.put("hideHomeBreadcrumb", booleanValue(source.get("hideHomeBreadcrumb"), (Boolean) defaults.get("hideHomeBreadcrumb")));
        normalized.put("hideHomeTagsView", booleanValue(source.get("hideHomeTagsView"), (Boolean) defaults.get("hideHomeTagsView")));
        Map<String, Object> legacyNavbarTitle = legacyNavbarTitleConfig();
        normalized.put(
            "navbarTitleVisible",
            booleanValue(source.get("navbarTitleVisible"), booleanValue(legacyNavbarTitle.get("visible"), true))
        );
        String legacyNavbarTitleText = boundedText(legacyNavbarTitle.get("title"), "", 60);
        normalized.put("navbarTitle", boundedText(source.get("navbarTitle"), legacyNavbarTitleText, 60));
        String navbarTitleAlign = String.valueOf(source.getOrDefault("navbarTitleAlign", "left")).trim().toLowerCase(Locale.ROOT);
        normalized.put("navbarTitleAlign", Set.of("left", "center", "right").contains(navbarTitleAlign) ? navbarTitleAlign : "left");
        normalized.put("navbarTitleFontSize", boundedInteger(source.get("navbarTitleFontSize"), 15, 12, 28));
        String navbarTitleColor = boundedText(source.get("navbarTitleColor"), "#29445f", 7);
        normalized.put("navbarTitleColor", navbarTitleColor.matches("^#[0-9a-fA-F]{6}$") ? navbarTitleColor : "#29445f");
        String navbarTitleFontWeight = String.valueOf(source.getOrDefault("navbarTitleFontWeight", "bold")).trim().toLowerCase(Locale.ROOT);
        normalized.put("navbarTitleFontWeight", Set.of("normal", "medium", "bold").contains(navbarTitleFontWeight) ? navbarTitleFontWeight : "bold");
        String navbarTitleFontFamily = String.valueOf(source.getOrDefault("navbarTitleFontFamily", defaults.get("navbarTitleFontFamily"))).trim().toLowerCase(Locale.ROOT);
        normalized.put(
            "navbarTitleFontFamily",
            Set.of("system", "microsoft-yahei", "simhei", "simsun", "kaiti").contains(navbarTitleFontFamily)
                ? navbarTitleFontFamily
                : defaults.get("navbarTitleFontFamily")
        );
        String navbarTitleLogoMode = String.valueOf(source.getOrDefault("navbarTitleLogoMode", defaults.get("navbarTitleLogoMode"))).trim().toLowerCase(Locale.ROOT);
        normalized.put(
            "navbarTitleLogoMode",
            Set.of("hidden", "system", "asset").contains(navbarTitleLogoMode) ? navbarTitleLogoMode : defaults.get("navbarTitleLogoMode")
        );
        String navbarTitleLogoAssetKey = boundedText(source.get("navbarTitleLogoAssetKey"), "", 80);
        normalized.put(
            "navbarTitleLogoAssetKey",
            WORKBENCH_ASSET_KEY_PATTERN.matcher(navbarTitleLogoAssetKey).matches() ? navbarTitleLogoAssetKey : ""
        );
        normalized.put("navbarTitleLogoHeight", boundedInteger(source.get("navbarTitleLogoHeight"), 28, 16, 36));
        normalized.put(
            "showUserNickname",
            booleanValue(source.get("showUserNickname"), (Boolean) defaults.get("showUserNickname"))
        );
        normalized.put("showScreenfull", booleanValue(source.get("showScreenfull"), (Boolean) defaults.get("showScreenfull")));
        normalized.put("showSizeSelect", booleanValue(source.get("showSizeSelect"), (Boolean) defaults.get("showSizeSelect")));
        normalized.put("showUserAvatar", booleanValue(source.get("showUserAvatar"), (Boolean) defaults.get("showUserAvatar")));
        String legacyUserDisplayMode = booleanValue(source.get("showUserNickname"), true) ? "nickname" : "hidden";
        String userDisplayMode = String.valueOf(source.getOrDefault("userDisplayMode", legacyUserDisplayMode)).trim().toLowerCase(Locale.ROOT);
        normalized.put("userDisplayMode", Set.of("nickname", "username", "hidden").contains(userDisplayMode) ? userDisplayMode : legacyUserDisplayMode);
        String brandTitleMode = String.valueOf(source.getOrDefault("brandTitleMode", defaults.get("brandTitleMode"))).trim().toLowerCase(Locale.ROOT);
        normalized.put("brandTitleMode", Set.of("role", "custom", "activity", "hidden").contains(brandTitleMode) ? brandTitleMode : defaults.get("brandTitleMode"));
        normalized.put(
            "brandTitleKeepVisibleOnCollapse",
            booleanValue(source.get("brandTitleKeepVisibleOnCollapse"), (Boolean) defaults.get("brandTitleKeepVisibleOnCollapse"))
        );
        normalized.put("brandTitle", boundedText(source.get("brandTitle"), (String) defaults.get("brandTitle"), 40));
        normalized.put("brandActivityId", boundedText(source.get("brandActivityId"), "", 64));
        normalized.put("configured", configured);
        return normalized;
    }

    private Map<String, Object> legacyNavbarTitleConfig() {
        String value = queryExactConfigValue(NAVBAR_TITLE_CONFIG_KEY, DEFAULT_NAVBAR_TITLE_CONFIG);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = JsonUtils.parseObject(value, Map.class);
            return parsed == null ? Map.of() : parsed;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private Map<String, Object> defaultRoleShellConfig(String roleKey) {
        boolean schoolRole = matchesRoleKey(roleKey, "crehn_school") || matchesRoleKey(roleKey, "school");
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("navType", "left");
        defaults.put("theme", "#2563EB");
        defaults.put("radiusBase", 8);
        defaults.put("tagsView", true);
        defaults.put("tagsIcon", false);
        defaults.put("fixedHeader", true);
        defaults.put("sidebarLogo", true);
        defaults.put("dynamicTitle", false);
        defaults.put("breadcrumbVisible", true);
        defaults.put("hideHomeBreadcrumb", schoolRole);
        defaults.put("hideHomeTagsView", schoolRole);
        defaults.put("navbarTitleFontFamily", "system");
        defaults.put("navbarTitleLogoMode", "hidden");
        defaults.put("navbarTitleLogoAssetKey", "");
        defaults.put("navbarTitleLogoHeight", 28);
        defaults.put("showUserNickname", true);
        defaults.put("showScreenfull", true);
        defaults.put("showSizeSelect", true);
        defaults.put("showUserAvatar", true);
        defaults.put("brandTitleMode", "role");
        defaults.put("brandTitleKeepVisibleOnCollapse", false);
        defaults.put("brandTitle", "工作台");
        return defaults;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue && ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue))) {
            return Boolean.parseBoolean(stringValue);
        }
        return defaultValue;
    }

    private String boundedText(Object value, String defaultValue, int maxLength) {
        String text = value == null ? defaultValue : String.valueOf(value).trim();
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private int boundedInteger(Object value, int defaultValue, int min, int max) {
        try {
            int number = value instanceof Number numericValue ? numericValue.intValue() : Integer.parseInt(String.valueOf(value));
            return Math.min(Math.max(number, min), max);
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }

    private void saveExactConfig(SysConfigBo bo) {
        List<SysConfigVo> existingConfigs = findExactConfigs(bo.getConfigKey());
        if (CollUtil.isEmpty(existingConfigs)) {
            configService.insertConfig(bo);
            return;
        }
        for (SysConfigVo existingConfig : existingConfigs) {
            configService.updateConfig(copyConfigBo(bo, existingConfig.getConfigId()));
        }
    }

    private List<SysConfigVo> findExactConfigs(String configKey) {
        SysConfigBo query = new SysConfigBo();
        query.setConfigKey(configKey);
        return configService.selectConfigList(query).stream()
            // SysConfigService list queries use LIKE for the admin search page.
            .filter(config -> configKey.equals(config.getConfigKey()))
            .toList();
    }

    private SysConfigBo copyConfigBo(SysConfigBo source, Long configId) {
        SysConfigBo target = new SysConfigBo();
        target.setConfigId(configId);
        target.setConfigName(source.getConfigName());
        target.setConfigKey(source.getConfigKey());
        target.setConfigValue(source.getConfigValue());
        target.setConfigType(source.getConfigType());
        target.setRemark(source.getRemark());
        return target;
    }

    private String normalizeStyleConfig(String configJson) {
        String value = configJson == null || configJson.isBlank() ? DEFAULT_STYLE_CONFIG : configJson;
        if (value.length() > STYLE_CONFIG_MAX_LENGTH) {
            throw new ServiceException("工作台样式配置过长，请控制在 " + STYLE_CONFIG_MAX_LENGTH + " 字符以内");
        }
        if (!JsonUtils.isJsonObject(value)) {
            throw new ServiceException("工作台样式配置必须是 JSON 对象");
        }
        return value;
    }

    private String normalizeSecurityReminderConfig(String configJson) {
        String value = configJson == null || configJson.isBlank() ? DEFAULT_SECURITY_REMINDER_CONFIG : configJson;
        if (value.length() > 1000) {
            throw new ServiceException("Security reminder config is too long, keep it within 1000 characters");
        }
        if (!JsonUtils.isJsonObject(value)) {
            throw new ServiceException("Security reminder config must be a JSON object");
        }
        return value;
    }

    private void validateWorkbenchAsset(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择工作台图片");
        }
        if (file.getSize() > WORKBENCH_ASSET_MAX_SIZE) {
            throw new ServiceException("工作台图片大小不能超过 5MB");
        }
        String originalName = safeAssetOriginalName(file.getOriginalFilename());
        String extension = assetExtension(originalName);
        String contentType = trimToEmpty(file.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && !WORKBENCH_ASSET_ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceException("工作台图片类型不合法");
        }
        if (!contentType.isBlank()
            && !"application/octet-stream".equals(contentType)
            && !("image/pjpeg".equals(contentType) && ("jpg".equals(extension) || "jpeg".equals(extension)))
            && !contentTypeForExtension(extension).equals(contentType)) {
            throw new ServiceException("工作台图片类型与扩展名不匹配");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!hasValidAssetHeader(extension, header)) {
                throw new ServiceException("工作台图片内容不合法");
            }
        } catch (IOException e) {
            throw new ServiceException("工作台图片读取失败");
        }
    }

    private boolean hasValidAssetHeader(String extension, byte[] header) {
        return switch (extension) {
            case "png" -> header.length >= 8
                && unsigned(header[0]) == 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G'
                && unsigned(header[4]) == 0x0D
                && unsigned(header[5]) == 0x0A
                && unsigned(header[6]) == 0x1A
                && unsigned(header[7]) == 0x0A;
            case "jpg", "jpeg" -> header.length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF;
            case "webp" -> header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private Path workbenchAssetRoot() {
        Path root = workbenchAssetDir == null || workbenchAssetDir.isBlank()
            ? Path.of(System.getProperty("user.dir"), "data", "workbench-assets")
            : Path.of(workbenchAssetDir);
        return root.toAbsolutePath().normalize();
    }

    private Path resolveWorkbenchAssetPath(String fileKey, boolean existingOnly) {
        String normalizedKey = trimToEmpty(fileKey).replace('\\', '/');
        if (normalizedKey.isBlank()
            || normalizedKey.startsWith("/")
            || normalizedKey.contains("../")
            || normalizedKey.contains("/..")
            || !normalizedKey.matches("^\\d{4}/\\d{2}/[a-f0-9]{32}\\.(png|jpg|jpeg|webp)$")) {
            throw new ServiceException("工作台图片路径不合法");
        }
        Path root = workbenchAssetRoot();
        Path path = root.resolve(normalizedKey).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new ServiceException("工作台图片路径不合法");
        }
        if (existingOnly && !Files.isRegularFile(path)) {
            return null;
        }
        return path;
    }

    private String assetExtension(String fileName) {
        String name = trimToEmpty(fileName).replace('\\', '/');
        int index = name.lastIndexOf('.');
        String extension = index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
        if (!WORKBENCH_ASSET_CONTENT_TYPES.containsKey(extension)) {
            throw new ServiceException("仅支持 png、jpg、jpeg、webp 图片");
        }
        return extension;
    }

    private String contentTypeForExtension(String extension) {
        return WORKBENCH_ASSET_CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private String safeAssetOriginalName(String originalName) {
        String name = trimToEmpty(originalName).replaceAll("[\\\\/:*?\"<>|,]+", "_").trim();
        return name.isBlank() ? "workbench-image.png" : name;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveRoleLayout(Long roleId, List<SysWorkbenchLayoutBo> layouts) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        String roleKey = normalize(role.getRoleKey());
        workbenchLayoutMapper.delete(new LambdaQueryWrapper<SysWorkbenchLayout>().eq(SysWorkbenchLayout::getRoleId, roleId));
        if (CollUtil.isEmpty(layouts)) {
            return true;
        }

        List<SysWorkbenchLayout> rows = new ArrayList<>();
        int order = 1;
        for (SysWorkbenchLayoutBo layout : layouts) {
            SysWorkbenchComponentVo component = COMPONENTS.get(layout.getComponentKey());
            if (component == null) {
                throw new ServiceException("unknown workbench component: " + layout.getComponentKey());
            }
            if (!isAllowedForRole(component, roleKey)) {
                throw new ServiceException("component is not allowed for role: " + component.getComponentName());
            }
            SysWorkbenchLayout row = new SysWorkbenchLayout();
            row.setRoleId(roleId);
            row.setComponentKey(component.getComponentKey());
            row.setTitle(blankToDefault(layout.getTitle(), component.getDefaultTitle()));
            row.setWidth(normalizeWidth(layout.getWidth(), component.getDefaultWidth()));
            row.setSortOrder(layout.getSortOrder() == null ? order : layout.getSortOrder());
            row.setVisible(HIDDEN.equals(layout.getVisible()) ? HIDDEN : VISIBLE);
            row.setConfigJson(layout.getConfigJson());
            rows.add(row);
            order++;
        }
        return workbenchLayoutMapper.insertBatch(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean restoreRoleComponentDefault(Long roleId, String componentKey) {
        roleService.checkRoleDataScope(roleId);
        SysRole role = requireRole(roleId);
        String key = normalize(componentKey);
        SysWorkbenchComponentVo component = COMPONENTS.get(key);
        if (component == null || !isAllowedForRole(component, role.getRoleKey())) {
            throw new ServiceException("component is not allowed for role");
        }
        if (!PROJECT_SUBMIT_COMPONENT_KEY.equals(key)) {
            throw new ServiceException("component does not support independent restore");
        }
        List<SysWorkbenchLayout> rows = workbenchLayoutMapper.selectList(
            new LambdaQueryWrapper<SysWorkbenchLayout>()
                .eq(SysWorkbenchLayout::getRoleId, roleId)
                .eq(SysWorkbenchLayout::getComponentKey, key)
        );
        for (SysWorkbenchLayout row : rows) {
            row.setConfigJson(null);
            workbenchLayoutMapper.updateById(row);
        }
        return true;
    }

    private List<SysWorkbenchLayoutVo> materializeLayout(Long roleId, String roleKey) {
        List<SysWorkbenchLayout> saved = roleId == null ? List.of() : workbenchLayoutMapper.selectList(
            new LambdaQueryWrapper<SysWorkbenchLayout>()
                .eq(SysWorkbenchLayout::getRoleId, roleId)
                .orderByAsc(SysWorkbenchLayout::getSortOrder)
                .orderByAsc(SysWorkbenchLayout::getId));
        if (CollUtil.isEmpty(saved)) {
            return defaultLayout(roleId, roleKey);
        }
        return saved.stream()
            .map(row -> toLayoutVo(row, roleKey))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(SysWorkbenchLayoutVo::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private List<SysWorkbenchLayoutVo> defaultLayout(Long roleId, String roleKey) {
        List<SysWorkbenchLayoutVo> rows = new ArrayList<>();
        int order = 1;
        for (SysWorkbenchComponentVo component : COMPONENTS.values()) {
            if (!isAllowedForRole(component, roleKey)) {
                continue;
            }
            SysWorkbenchLayoutVo row = new SysWorkbenchLayoutVo();
            row.setRoleId(roleId);
            row.setRoleKey(roleKey);
            row.setComponentKey(component.getComponentKey());
            row.setComponentName(component.getComponentName());
            row.setComponentType(component.getComponentType());
            row.setTitle(component.getDefaultTitle());
            row.setWidth(component.getDefaultWidth());
            row.setSortOrder(order++);
            row.setVisible(VISIBLE);
            row.setPermission(component.getPermission());
            rows.add(row);
        }
        return rows;
    }

    private SysWorkbenchLayoutVo toLayoutVo(SysWorkbenchLayout row, String roleKey) {
        SysWorkbenchComponentVo component = COMPONENTS.get(row.getComponentKey());
        if (component == null || !isAllowedForRole(component, roleKey)) {
            return null;
        }
        SysWorkbenchLayoutVo vo = new SysWorkbenchLayoutVo();
        vo.setId(row.getId());
        vo.setRoleId(row.getRoleId());
        vo.setRoleKey(roleKey);
        vo.setComponentKey(row.getComponentKey());
        vo.setComponentName(component.getComponentName());
        vo.setComponentType(component.getComponentType());
        vo.setTitle(blankToDefault(row.getTitle(), component.getDefaultTitle()));
        vo.setWidth(normalizeWidth(row.getWidth(), component.getDefaultWidth()));
        vo.setSortOrder(row.getSortOrder());
        vo.setVisible(HIDDEN.equals(row.getVisible()) ? HIDDEN : VISIBLE);
        vo.setConfigJson(row.getConfigJson());
        vo.setPermission(component.getPermission());
        return vo;
    }

    private RoleDTO resolveCurrentRole() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || CollUtil.isEmpty(loginUser.getRoles())) {
            return null;
        }
        List<String> priority = List.of("crehn_school", "school", "crehn_admin", "admin", "superadmin", "crehn_ops", "ops", "crehn_auditor", "auditor", "crehn_project_viewer", "crehn_expert", "expert");
        return loginUser.getRoles().stream()
            .min(Comparator.comparingInt(role -> rolePriority(role.getRoleKey(), priority)))
            .orElse(loginUser.getRoles().get(0));
    }

    private int rolePriority(String roleKey, List<String> priority) {
        String key = normalize(roleKey);
        for (int i = 0; i < priority.size(); i++) {
            if (matchesRoleKey(key, priority.get(i))) {
                return i;
            }
        }
        return priority.size();
    }

    private boolean hasComponentPermission(SysWorkbenchLayoutVo layout) {
        return layout.getPermission() == null || layout.getPermission().isBlank() || StpUtil.hasPermission(layout.getPermission());
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("role does not exist");
        }
        return role;
    }

    private boolean isAllowedForRole(SysWorkbenchComponentVo component, String roleKey) {
        String key = normalize(roleKey);
        if (key.isBlank()) {
            return true;
        }
        String effectiveKey = "superadmin".equals(key) ? "admin" : key;
        return component.getRoleKeys().stream().anyMatch(allowed -> matchesRoleKey(effectiveKey, allowed));
    }

    private boolean matchesRoleKey(String roleKey, String allowed) {
        String key = normalize(roleKey);
        String target = normalize(allowed);
        return key.equals(target) || key.startsWith(target + "_");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeWidth(String value, String defaultWidth) {
        String width = blankToDefault(value, defaultWidth);
        return switch (width) {
            case "1/2", "1/3", "2/3", "1/1" -> width;
            default -> "1/1";
        };
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static Map<String, SysWorkbenchComponentVo> buildComponents() {
        Map<String, SysWorkbenchComponentVo> map = new LinkedHashMap<>();
        put(map, "school_stage_notice", "学校活动公告", "notice", "学校活动公告", "1/1", "crehn:project:list", List.of("crehn_school", "school"));
        put(map, "admin_stage_notice", "管理员公告", "notice", "管理员公告", "1/1", "crehn:result:summary", List.of("crehn_admin", "admin", "crehn_ops", "ops"));
        put(map, "audit_stage_notice", "审核公告", "notice", "审核公告", "1/1", "crehn:audit:list", List.of("crehn_auditor", "auditor", "crehn_admin"));
        put(map, "review_stage_notice", "评审公告", "notice", "评审公告", "1/1", "crehn:review:task", List.of("crehn_expert", "expert"));
        put(map, "school_submission", "节目和作品报送", "business", "节目和作品报送", "1/1", "crehn:project:list", List.of("crehn_school", "school"));
        put(map, "school_project_submit", "学校数据看板", "dashboard", "学校数据看板", "1/1", "crehn:project:list", List.of("crehn_school", "school"));
        put(map, PROJECT_SUBMIT_COMPONENT_KEY, "统一提交列表", "list", "统一提交", "1/1", "crehn:project:list", List.of("crehn_school", "school"));
        put(map, AUDIT_WORKBENCH_COMPONENT_KEY, "审核工作台", "business", "审核工作台", "1/1", "crehn:audit:list", List.of("crehn_auditor", "auditor"));
        put(map, REVIEW_WORKBENCH_COMPONENT_KEY, "评分工作台", "business", "评分工作台", "1/1", "crehn:review:task", List.of("crehn_expert", "expert"));
        put(map, "audit_overview", "我的审核概览", "stats", "我的审核概览", "1/2", "crehn:audit:list", List.of("crehn_admin"));
        put(map, "audit_pending", "待审核项目", "list", "待审核项目", "1/2", "crehn:audit:list", List.of("crehn_admin"));
        put(map, "admin_project_summary", "报送总览", "stats", "报送总览", "1/2", "crehn:result:summary", List.of("crehn_admin", "admin", "crehn_ops", "ops"));
        put(map, "admin_upload_summary", "类别分布", "stats", "类别分布", "1/2", "crehn:result:summary", List.of("crehn_admin", "admin", "crehn_ops", "ops"));
        put(map, "project_upload_overview", "作品上传总览", "stats", "作品上传总览", "1/1", "", List.of("crehn_admin", "admin", "crehn_ops", "ops", "crehn_project_viewer"));
        put(map, "quota_ratio_overview", "名额比例监控", "stats", "名额比例监控", "1/2", "crehn:result:summary", List.of("crehn_admin", "admin", "crehn_ops", "ops"));
        put(map, "universal_dashboard", "通用数据看板", "dashboard", "数据看板", "1/1", "", List.of(
            "crehn_school", "school", "crehn_admin", "admin", "crehn_ops", "ops",
            "crehn_auditor", "auditor", "crehn_expert", "expert", "crehn_project_viewer", "project_viewer"));
        return map;
    }

    private static void put(Map<String, SysWorkbenchComponentVo> map, String key, String name, String type, String title,
                            String width, String permission, List<String> roleKeys) {
        map.put(key, new SysWorkbenchComponentVo(key, name, type, title, width, permission, roleKeys));
    }

}
