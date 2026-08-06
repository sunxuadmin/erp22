package org.dromara.crehn.participant.service;

import jakarta.servlet.http.HttpServletRequest;
import org.dromara.crehn.domain.ParticipantProfile;
import org.dromara.crehn.domain.bo.ParticipantActivateBo;
import org.dromara.crehn.domain.bo.ParticipantActivationPreviewBo;
import org.dromara.crehn.domain.bo.ParticipantBatchImportBo;
import org.dromara.crehn.domain.bo.ParticipantCodeReissueBo;
import org.dromara.crehn.domain.vo.ParticipantActivationIssueVo;
import org.dromara.crehn.domain.vo.ParticipantActivationPreviewVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IParticipantAccountService {
    TableDataInfo<ParticipantProfile> queryPage(ParticipantProfile query, PageQuery pageQuery);
    List<ParticipantActivationIssueVo> importParticipants(ParticipantBatchImportBo bo);
    ParticipantActivationPreviewVo previewActivation(ParticipantActivationPreviewBo bo);
    void activate(ParticipantActivateBo bo, HttpServletRequest request);
    ParticipantActivationIssueVo reissue(ParticipantCodeReissueBo bo, HttpServletRequest request);
}
