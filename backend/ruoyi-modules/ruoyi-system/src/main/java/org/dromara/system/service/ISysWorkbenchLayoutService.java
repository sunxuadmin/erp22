package org.dromara.system.service;

import org.dromara.system.domain.bo.SysWorkbenchLayoutBo;
import org.dromara.system.domain.vo.SysWorkbenchAssetUploadVo;
import org.dromara.system.domain.vo.SysWorkbenchComponentVo;
import org.dromara.system.domain.vo.SysWorkbenchConfigImportPreviewVo;
import org.dromara.system.domain.vo.SysWorkbenchLayoutVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ISysWorkbenchLayoutService {

    List<SysWorkbenchComponentVo> componentOptions(Long roleId);

    List<SysWorkbenchLayoutVo> queryCurrentLayout();

    SysWorkbenchLayoutVo queryCurrentComponent(String componentKey);

    List<SysWorkbenchLayoutVo> queryRoleLayout(Long roleId);

    Map<String, Object> queryCurrentRoleShellConfig();

    Map<String, Object> queryRoleShellConfig(Long roleId);

    Boolean saveRoleShellConfig(Long roleId, Map<String, Object> config);

    Boolean restoreRoleShellConfig(Long roleId);

    String queryNavbarTitleConfig();

    Boolean saveNavbarTitleConfig(String configJson);

    String queryStyleConfig();

    Map<String, Object> saveStyleConfig(String configJson);

    String querySecurityReminderConfig();

    Boolean saveSecurityReminderConfig(String configJson);

    SysWorkbenchAssetUploadVo uploadWorkbenchAsset(MultipartFile file);

    Path resolveWorkbenchAssetPath(String fileKey);

    String resolveWorkbenchAssetContentType(String fileKey);

    Boolean deleteWorkbenchAsset(String fileKey);

    byte[] exportConfigPackage();

    SysWorkbenchConfigImportPreviewVo previewConfigPackage(MultipartFile file);

    Boolean importConfigPackage(MultipartFile file);

    Boolean saveRoleLayout(Long roleId, List<SysWorkbenchLayoutBo> layouts);

    Boolean restoreRoleComponentDefault(Long roleId, String componentKey);

}
