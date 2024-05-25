package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.model.domain.user;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author jiang
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-05-05 10:44:37
*/
public interface UserService extends IService<user> {

    /**
     * 用户注册
     *
     * @param userAccount 用户账户
     * @param password 密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 用户id
     */
    long userRegister(String userAccount,String password,String checkPassword,String planetCode);


    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param password  用户密码
     * @param request   http请求
     * @return 脱敏后的用户信息
     */
    user userLogin(String userAccount, String password, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return 脱敏后的用户信息
     */
    user getSafetyUser(user originUser);
}
