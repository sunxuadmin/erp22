package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ReviewResultBo {
    private Long activityId;
    private Long categoryId;
    private Boolean showScore;
    private Boolean showRank;
    private Boolean showComment;
    private Boolean notifySchoolAccounts;
    private String remark;
}
