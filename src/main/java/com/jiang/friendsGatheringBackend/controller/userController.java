package com.jiang.friendsGatheringBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiang.friendsGatheringBackend.common.BaseResponse;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.common.ResultUtils;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.request.userLoginRequest;
import com.jiang.friendsGatheringBackend.model.request.userRegisterRequest;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import com.jiang.friendsGatheringBackend.model.vo.LoginUserVO;
import com.jiang.friendsGatheringBackend.service.impl.UserServiceImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @Author jiang
 */
@RequestMapping("/users")
@RestController
public class userController {
    @Resource
    private UserServiceImpl userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    /**
     * 用户注册接口
     *
     * @param userRegisterRequest1 用户注册请求体
     * @return
     */
    @PostMapping("/register")
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
    @PostMapping("/login")
    public BaseResponse<String> userLogin(@RequestBody userLoginRequest userLoginRequest1, HttpServletRequest request){
        if(userLoginRequest1==null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest1.getUserAccount();
        String userPassword = userLoginRequest1.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            return  ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }

        String sessionId= null;
        try {
            sessionId = userService.userLogin(userAccount,userPassword,request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResultUtils.success(sessionId);
    }


    /**
     * 用户退出登录态
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 根据用户名搜索用户（管理员权限）
     * @param username
     * @param request
     * @return
     */
    @PostMapping("/search")
    public BaseResponse<List<User>> userSearch(String username,HttpServletRequest request){
        //如果不是管理员，则抛出无权限的错误
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user))
                    .collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 根据标签列表搜索用户
     *
     * @param tagNameList
     * @return
     */
    @PostMapping("/search/tags")
    public BaseResponse<List<User>> searchByTags(@RequestParam(required = false) List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        if(CollectionUtils.isEmpty(userList)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"没有符合条件的队伍");
        }
        return ResultUtils.success(userList);
    }

    /**
     * 为当前用户推荐 其他用户（从整个用户库中选一部分进行推荐、通过分页，限制展示的条数），
     * 采用缓存实现，可以提升响应速度
     *
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommend(@RequestParam long pageSize,@RequestParam long pageNum,HttpServletRequest request){
        //获取当前登录用户
        SessionData loginUser = userService.getLoginUser(request);
        //从缓存中读取数据
        String redisKey = String.format("friendsGathering:user:recommend:%s+%s+%s",loginUser.getUserId(),pageNum,pageSize);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Page<User> page = (Page<User>) valueOperations.get(redisKey);
        if(page!=null){
            return ResultUtils.success(page);
        }
        //如果无缓存,从数据库中读取
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        //写缓存
        valueOperations.set(redisKey,userPage,24, TimeUnit.HOURS);
        return ResultUtils.success(userPage);
    }

    /**
     * 匹配用户
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUser(long num,HttpServletRequest request){
        if(num<0||num>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"匹配人数不能超过20人，也不能为空");
        }
        SessionData loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUser(num,loginUser));
    }


    @PostMapping("/tags")
    public ResponseEntity<BaseResponse> uploadTags(@RequestBody String tagsJson,HttpServletRequest request) {
        // 将 JSON 字符串转换为 List<String>
        List<String> tags;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(new BaseResponse(ErrorCode.PARAMS_ERROR,"Invalid tagsString"));
        }

        // 校验 tags 是否合法
        if (tags == null || tags.isEmpty()) {
            return ResponseEntity.badRequest().body(new BaseResponse(ErrorCode.PARAMS_ERROR, "Tags must not be empty"));
        }

        // 转换为 JSON 字符串保存到数据库
        try {
            String tagsJsonString = new ObjectMapper().writeValueAsString(tags);
            // 调用 UserService 的方法保存 tagsJson 到数据库
            userService.saveUserTags(tagsJsonString,request);
            return ResponseEntity.ok().body(new BaseResponse(ErrorCode.SUCCESS, "Tags uploaded successfully"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new BaseResponse(ErrorCode.SYSTEM_ERROR, "Failed to process tags"));
        }
    }
}
