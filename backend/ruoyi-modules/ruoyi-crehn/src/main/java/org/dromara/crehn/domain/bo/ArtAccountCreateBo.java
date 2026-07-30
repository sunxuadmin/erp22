package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ArtAccountCreateBo {
    private String userName;
    private String loginAlias;
    private String nickName;
    private String password;
    private String userType;
    private Long schoolId;
    private String email;
    private String phonenumber;
    private String remark;
    private String schoolReviewStatus;
    private Long[] roleIds;
}
