package com.jiang.friendsGatheringBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jiang.friendsGatheringBackend.Manager.RedisSessionManager;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.mapper.UserMapper;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import com.jiang.friendsGatheringBackend.service.UserService;


import com.jiang.friendsGatheringBackend.util.AlgrithomUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jiang.friendsGatheringBackend.constant.userConstant.ADMIN_ROLE;
import static com.jiang.friendsGatheringBackend.constant.userConstant.USER_LOGIN_STATE;

/**
* @author jiang
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-05-05 10:44:37
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    public static final String SALT = "jiang";
    @Autowired
    private UserMapper userMapper;

    /**
     *  用户注册
     * @param userAccount 用户账户
     * @param password 密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 用户id
     */
    @Override
    public long userRegister(String userAccount, String password, String checkPassword, String planetCode) {
        //1.校验用户账户、密码、校验密码、星球编号是否符合要求
        //非空
        if(StringUtils.isAnyBlank(userAccount,password,checkPassword,planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }
        //账户长度不小于4位
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户长度不小于4位");
        }
        //密码不小于8位
        if(password.length()<8||checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不小于8位");
        }
        //星球编号不大于5位
        if(planetCode.length()>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号不大于5位");
        }
        //账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        boolean containsSpecialCharacter = Pattern.compile(validPattern).matcher(userAccount).find();
        if(containsSpecialCharacter){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户不能包含特殊字符");
        }
        //密码和校验密码相同
        if(!StringUtils.equals(password,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码不相同");
        }
        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("userAccount",userAccount);
        Long count = userMapper.selectCount(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户已存在");
        }
        //星球编号不能重复
        queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("planetCode",planetCode);
        count = userMapper.selectCount(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号重复");
        }
        //2.加密
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT+password).getBytes());
        //3.向数据库插入用户数据
        User u = new User();
        u.setUserAccount(userAccount);
        u.setUserPassword(encryptedPassword);
        u.setPlanetCode(planetCode);
        boolean isSaved = this.save(u); /// 这里的 this 是 service
        if(!isSaved){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户注册失败");
        }
        return u.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param password  用户密码
     * @param request   http请求
     * @return
     */
    @Override
    public String userLogin(String userAccount, String password, HttpServletRequest request) throws JsonProcessingException {

        // 假设这里有一个验证用户名和密码的逻辑
        User isAuthenticated = authenticate(userAccount, password);
        if (isAuthenticated!=null) {
            // 生成一个唯一的 sessionId
            String sessionId = generateSessionId();
            //3.用户脱敏
            User safetyUser = getSafetyUser(isAuthenticated);

            // 假设这里有一个获取用户信息的逻辑，构建 SessionData 对象
            //4.记录用户的登录态
//            HttpSession session = request.getSession();
//            String sessionId = session.getId();
            RedisSessionManager redisSessionManager =  new RedisSessionManager();
            SessionData sessionData = new SessionData();
            sessionData.setUserName(safetyUser.getUsername());
            sessionData.setUserId(String.valueOf(safetyUser.getId()));
            sessionData.setRole(safetyUser.getUserRole());
            sessionData.setTags(safetyUser.getTags());
            //redisSessionManager.saveSession(String.valueOf(safetyUser.getId()),sessionData);
            try {
                // 将 SessionData 存储到 Redis 中
                redisSessionManager.saveSession(sessionId, sessionData);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                redisSessionManager.close();
                throw new BusinessException(ErrorCode.LOGIN_FAILED,"请稍后重新登录");
                // 处理存储异常情况
            }
            return sessionId; // 返回 sessionId 给客户端
        }
        return null; // 登录失败，返回 null 或者其他错误信息


    }

        private String generateSessionId() {
            // 生成一个唯一的 sessionId，这里简单使用 UUID
            return UUID.randomUUID().toString();
        }
        private User authenticate(String userAccount, String password) {
            //1.校验用户账户和密码是否合法
            if(StringUtils.isAnyBlank(userAccount,password)){
                return null;
            }
            if(userAccount.length()<4){
                return null;
            }
            if(password.length()<8){
                return null;
            }
            //账户不能包含特殊字符
            String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
            Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
            if(matcher.find()){
                return null;
            }
            //2.加密
            String encryptedPassword = DigestUtils.md5DigestAsHex((SALT+password).getBytes());
            //查询数据库，校验密码是否正确
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userPassword",encryptedPassword);
            queryWrapper.eq("userAccount",userAccount);
            User user = userMapper.selectOne(queryWrapper);
            //用户不存在
            if(user==null){
                log.info("userLogin failed,userAccount can not match userPassword");
                return null;
            }

            // 假设这里有验证用户名和密码的逻辑，例如从数据库中查询
            // 实际中根据具体情况实现验证逻辑
            // 返回验证结果
            return user; // 这里暂时简单返回 true，实际中需根据用户名和密码进行验证
        }

    /**
     * 用户脱敏
     *
     * @param originUser 需要进行信息脱敏的用户
     * @return 信息脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        if(originUser==null){
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if(user==null){
            return false;
        }
        return user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(SessionData loginUser) {
        if(loginUser==null) {
            return false;
        }
        return loginUser.getRole()==ADMIN_ROLE;
    }

    /**
     * 根据标签搜索用户
     * @param tagsNameList
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagsNameList) {
        if(CollectionUtils.isEmpty(tagsNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = this.list(queryWrapper);
        Gson gson = new Gson();
        List<User> list = userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagnameSet = gson.fromJson(tagsStr, new TypeToken<>() {
            }.getType());
            tempTagnameSet = Optional.ofNullable(tempTagnameSet).orElse(new HashSet<>());
            for (String tagName : tagsNameList) {
                if (!tempTagnameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        return list;
    }

    /**
     * 用户退出登录
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String sessionId = authorizationHeader.substring(7); // 获取 sessionId 部分
            RedisSessionManager redisSessionManager = new RedisSessionManager();
            redisSessionManager.logout(sessionId);
        }
        return 1;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public SessionData getLoginUser(HttpServletRequest request) {

        String authorizationHeader = request.getHeader("Authorization");
        SessionData sessionData = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String sessionId = authorizationHeader.substring(7); // 获取 sessionId 部分
            RedisSessionManager redisSessionManager = new RedisSessionManager();
            try {
                sessionData = redisSessionManager.getSession(sessionId);
                if(sessionData==null){
                    throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
                }
                //当用户使用到登录功能后，更新其 redis 中 session 存储的时间
                redisSessionManager.updateSessionExpiration(sessionId);

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if(sessionData==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }
        return sessionData;
    }

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUser(long num, SessionData loginUser) {
        //1. 校验参数
        if(num<0||num>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"匹配人数不能超过20人，也不能为空");
        }
        //2. 得到当前登录用户的 tagNameList
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagNameList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您的标签为空,匹配失败");
        }
        //3. 从数据库中获取所有用户的 tagNameList
        //   1. （除了当前用户自身）
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.select("id","tags");
        userWrapper.isNotNull("tags");
        List<User> userList = this.list(userWrapper);

        //   2. 并将所有用户的 tagNameList 与当前用户的 tagNameList 进行比较，得到一个分值。
        // 用户（只包括 id,tags） ===> 相似度
        List<Pair<User,Long>> list = new ArrayList<>();
        for(int i=0;i<userList.size();i++){
            User user = userList.get(i);
            String userTags = user.getTags();
            //标签为空或者是当前用户则跳过
            if("[]".equals(userTags)||StringUtils.isBlank(userTags)||loginUser.getUserId().equals(user.getId())){
                continue;
            }
            List<String> curUserTagNameList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            long score = AlgrithomUtils.minDistance(tagNameList, curUserTagNameList);
            list.add(Pair.of(user,score));
        }
        //4. 按照编辑距离由小到大排序
        List<Pair<User, Long>> topUserList = list.stream()
                .sorted((a, b) -> (int) (a.getSecond() - b.getSecond()))
                .limit(num)
                .toList();
        //5. 得到一个 TOP 用户 idList
        List<Long> toReturnIdList = getMinNElements(topUserList,num);
        //6. 通过用户 Id 得到用户，将用户列表返回。
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",toReturnIdList);
        Map<Long, List<User>> topUserIdToSafetyUserMap = this.list(userQueryWrapper).stream()
                .map(user-> getSafetyUser(user))
                .collect(Collectors.groupingBy(user->user.getId()));
        List<User> finalInoderReturnUserList = new ArrayList<>();
        for(int i=0;i<toReturnIdList.size();i++){
            finalInoderReturnUserList.add(topUserIdToSafetyUserMap.get(toReturnIdList.get(i)).get(0));
        }
        return finalInoderReturnUserList;
    }
    private List<Long> getMinNElements(List<Pair<User, Long>> userList, Long N) {
        // 创建一个大顶堆优先队列，按照 Integer 升序排列
        PriorityQueue<Pair<User, Long>> maxHeap = new PriorityQueue<>(N, (a, b) -> (b.getSecond() - a.getSecond()));

        // 遍历 userList
        for (Pair<User, Long> pair : userList) {
            maxHeap.offer(pair); // 将元素加入大顶堆

            // 如果大顶堆的大小超过了 N，移除堆顶元素（值最大的元素）
            if (maxHeap.size() > N) {
                maxHeap.poll();
            }
        }

        // 从大顶堆中提取值最小的 N 个元素的用户 id
        return maxHeap.stream()
                .map(pair -> pair.getFirst().getId())
                .collect(Collectors.toList());
    }

    /**
     * 用户上传标签
     * @param tagsJsonString
     */
    @Override
    public void saveUserTags(String tagsJsonString,HttpServletRequest request) {
        String userId = getLoginUser(request).getUserId();
        // 查询当前用户的标签信息
        User user = userMapper.selectById(userId);

        // 获取原有的标签字段
        String originalTagsJsonString = user.getTags();
        List<String> originalTags = new ArrayList<>();

        // 如果原有标签字段不为空且不是空数组，则解析成列表
        if (!StringUtils.isEmpty(originalTagsJsonString) && !"[]".equals(originalTagsJsonString)) {
            try {
                originalTags = new ObjectMapper().readValue(originalTagsJsonString, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace(); // 处理 JSON 解析异常
            }
        }

        // 构建新的标签列表，将新上传的标签加入
        List<String> updatedTags = new ArrayList<>(originalTags);
        try {
            List<String> newTags = new ObjectMapper().readValue(tagsJsonString, new TypeReference<List<String>>() {});
            updatedTags.addAll(newTags);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // 处理 JSON 解析异常
        }

        // 将更新后的标签列表转换为 JSON 字符串
        String updatedTagsJsonString = "";
        try {
            updatedTagsJsonString = new ObjectMapper().writeValueAsString(updatedTags);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // 处理 JSON 转换异常
        }

        // 更新用户的 tags 字段
        user.setTags(updatedTagsJsonString);
        userMapper.updateById(user); // 更新用户信息
    }
}




