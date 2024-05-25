package com.jiang.friendsGatheringBackend.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @Author jiang
 */
@Data
public class userLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 4543907520670048561L;
    /**
     * 用户账户
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
}
