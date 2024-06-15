package com.jiang.friendsGatheringBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.mapper.UserMapper;
import com.jiang.friendsGatheringBackend.model.domain.User;
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
    public User userLogin(String userAccount, String password, HttpServletRequest request) {
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
        //3.用户脱敏
        User safetyUser = getSafetyUser(user);
        //4.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);

        return safetyUser;
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
    public boolean isAdmin(User loginUser) {
        if(loginUser==null) {
            return false;
        }
        return loginUser.getUserRole()==ADMIN_ROLE;
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
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if(userObj == null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }

        return (User) userObj;
    }

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUser(long num, User loginUser) {
        //1. 校验参数
        if(num<0||num>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"匹配人数不能超过20人，也不能为空");
        }
        //2. 得到当前登录用户的 tagNameList
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagNameList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
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
            if("[]".equals(userTags)||StringUtils.isBlank(userTags)||loginUser.getId().equals(user.getId())){
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
        List<Long> toReturnIdList = topUserList.stream()
                .map(pair -> {
                    return pair.getFirst().getId();
                })
                .collect(Collectors.toList());
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
}




