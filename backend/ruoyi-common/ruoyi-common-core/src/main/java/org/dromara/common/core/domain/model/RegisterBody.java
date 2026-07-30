package org.dromara.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterBody extends LoginBody {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = 2, max = 30, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = 5, max = 30, message = "{user.password.length.valid}")
//    @Pattern(regexp = RegexConstants.PASSWORD, message = "{user.password.format.valid}")
    private String password;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 手机号
     */
    private String phonenumber;

    /**
     * 注册码
     */
    private String registrationCode;

    /**
     * 注册时选择或带出的学校ID。
     */
    private Long schoolId;

    /**
     * 注册时新建学校名称。
     */
    private String schoolName;

    /**
     * 学校动态资料 JSON。
     */
    private String schoolProfileJson;

}
