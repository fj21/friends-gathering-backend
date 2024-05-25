package com.jiang.friendsGatheringBackend.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @Author jiang
 */
@Data
public class userRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 596092136752082995L;
    /**
     * 用户账户
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 校验密码
     */
    private String checkPassword;
    /**
     * 星球编号
     */
    private String planetCode;
}
