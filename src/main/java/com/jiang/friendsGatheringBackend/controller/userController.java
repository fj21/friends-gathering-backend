package com.jiang.friendsGatheringBackend.controller;

import com.jiang.friendsGatheringBackend.common.BaseResponse;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.common.ResultUtils;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.domain.request.userLoginRequest;
import com.jiang.friendsGatheringBackend.model.domain.request.userRegisterRequest;
import com.jiang.friendsGatheringBackend.model.domain.user;
import com.jiang.friendsGatheringBackend.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @Author jiang
 */
@RequestMapping("/users")
@RestController
public class userController {
    @Autowired
    private UserServiceImpl userService;

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest1 用户注册请求体
     * @return
     */

    public BaseResponse<Long> userRegister(@RequestBody userRegisterRequest userRegisterRequest1){
        //进行一个简单的校验，这里的校验不涉及业务逻辑
        //代码越少越好，如果这里的代码过多，可以考虑在业务层或者定义一个方法实现
        if(userRegisterRequest1==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userRegisterRequest1.getUserAccount();
        String userPassword = userRegisterRequest1.getUserPassword();
        String checkPassword = userRegisterRequest1.getCheckPassword();
        String planetCode = userRegisterRequest1.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录接口
     *
     * @param userLoginRequest1
     * @param request
     * @return
     */
    public BaseResponse<user> userLogin(userLoginRequest userLoginRequest1, HttpServletRequest request){
        if(userLoginRequest1==null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest1.getUserAccount();
        String userPassword = userLoginRequest1.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            return  ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        user user1= userService.userLogin(userAccount,userPassword,request);
        return ResultUtils.success(user1);
    }

}
