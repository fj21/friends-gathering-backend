package com.jiang.friendsGatheringBackend.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.domain.UserTeam;
import com.jiang.friendsGatheringBackend.model.dto.TeamQuery;
import com.jiang.friendsGatheringBackend.model.enums.TeamStatusEnum;
import com.jiang.friendsGatheringBackend.model.request.TeamDeleteRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamJoinRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamQuitRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamUpdateRequest;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import com.jiang.friendsGatheringBackend.model.vo.TeamUserVO;
import com.jiang.friendsGatheringBackend.model.vo.UserVO;
import com.jiang.friendsGatheringBackend.service.TeamService;
import com.jiang.friendsGatheringBackend.mapper.TeamMapper;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author jiang
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-05-25 19:38:45
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamServiceImpl userTeamService;

    @Resource
    private UserServiceImpl userService;


    @Resource
    private RedissonClient redissonClient;

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     */
    @Override
    public long createTeam(Team team, SessionData loginUser) {
        //1. 请求参数是否为空？
        if(team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        //2. 是否登录，未登录不允许创建
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum<=1||maxNum>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不符合要求");
        }
        //   2. 队伍标题 <= 20
        String teamName = team.getName();
        if(StringUtils.isBlank(teamName)||teamName.length()>20)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍标题长度不符合要求");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isBlank(description)||description.length()>512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍描述长度不符合要求");
        }
        //   4. status 是否公开（int），不传默认为 0（公开）
        Integer statusValue = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(statusValue);
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if(TeamStatusEnum.SECRET.equals(enumByValue)){
            if(StringUtils.isBlank(password)||password.length()>32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码设置不正确");
            }
        }
        //   6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超时时间设置不正确");
        }
        //   7. 校验用户最多创建 5 个队伍
        Long createrUserId = Long.valueOf(loginUser.getUserId());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",createrUserId);
        long count = this.count(queryWrapper);
        if(count>=5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多创建5个队伍");
        }
        //4. 插入队伍信息到队伍表,TODO 记得将创建用户的ID插入
        team.setId(null);
        team.setUserId(createrUserId);
        boolean save = this.save(team);
        Long teamId = team.getId();
        if(!save || teamId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍创建失败");
        }
        //5. 插入 用户  => 队伍 关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(createrUserId);
        userTeam.setJoinTime(new Date());
        boolean saved = userTeamService.save(userTeam);
        if(!saved){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍创建失败");
        }
        return teamId;
    }

    /**
     * 查询队伍,返回m满足查询条件的队伍列表
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> teamWrapper = new QueryWrapper<>();
        //1.组合查询条件
        if(teamQuery!=null){
            //1.根据 teamId 和 idList 查询
            Long id = teamQuery.getId();
            if(id!=null&&id>0){
                teamWrapper.eq("id",id);
            }
            List<Long> idList = teamQuery.getIdList();
            if(CollectionUtils.isNotEmpty(idList)){
                teamWrapper.in("id",idList);
            }
            //2.根据 searchText 查询
            String searchText = teamQuery.getSearchText();
            if(StringUtils.isNotBlank(searchText)){
                teamWrapper.and(qw -> qw.like("name",searchText).or().like("descrption",searchText));
            }
            //3.根据name列和descrption列查询
            String name = teamQuery.getName();
            if(StringUtils.isNotBlank(name)){
                teamWrapper.eq("name",name);
            }
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)){
                teamWrapper.eq("description",description);
            }
            //6.根据最大人数查询
            Integer maxNum = teamQuery.getMaxNum();
            if(maxNum!=null&&maxNum>0){
                teamWrapper.eq("maxNum",maxNum);
            }
            //7.根据创建人查询
            Long userId = teamQuery.getUserId();
            if(userId!=null&&userId>0){
                teamWrapper.eq("userId",userId);
            }
            //8.根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
            if(enumByValue == null){
                //默认查询公开队伍
                enumByValue = TeamStatusEnum.PUBLIC;
            }
            //如果不是管理员但想要查询私有队伍的话，抛出无权限的异常
            if(!isAdmin&&enumByValue.equals(TeamStatusEnum.PRIVATE)){
                throw new BusinessException(ErrorCode.NO_AUTH,"您没有权限查看私有队伍列表");
            }
            teamWrapper.eq("status",enumByValue.getValue());
        }
        //2.不展示已经过期的队伍
        teamWrapper.gt("expireTime",new Date());
        List<Team> queryedTeamList = this.list(teamWrapper);
        List<TeamUserVO> queryedTeamUserVOList = new ArrayList<>();
        queryedTeamList.stream().forEach(team -> {
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);
            queryedTeamUserVOList.add(teamUserVO);
        });
        //3.关联查询创建人的用户信息 TODO 在userService 新建一个getLoginUser(userId)方法优化
        QueryWrapper userWrapper = new QueryWrapper();
        queryedTeamUserVOList.stream().forEach(teamUserVO -> {
            Long userId = teamUserVO.getUserId();
            userWrapper.eq("id",userId);
            List list = userService.list(userWrapper);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(list.get(0),userVO);
            teamUserVO.setCreateUser(userVO);
        });


        return queryedTeamUserVOList;
    }

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, SessionData loginUser) {
        //1.判断请求参数是否为空
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"您提供的修改请求为空");
        }
        //2.查询队伍是否存在
        Long id = teamUpdateRequest.getId();
        if(id==null||id<=0){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您想要修改的队伍不存在");
        }
        Team oldTeam = this.getById(id);
        if(oldTeam == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您想要修改的队伍不存在");
        }
        //3.只有管理员或者队伍的创建者可以修改
        boolean isAdmin = userService.isAdmin(loginUser);
        if(!isAdmin && !oldTeam.getUserId().equals(Long.valueOf(loginUser.getUserId()))){
            throw new BusinessException(ErrorCode.NO_AUTH,"您没有权限修改此队伍信息");
        }
        //4.如果队伍状态改为加密，必须要有密码
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if(TeamStatusEnum.SECRET.equals(enumByValue)){
            String password = teamUpdateRequest.getPassword();
            if(StringUtils.isBlank(password)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密房间必须提供密码");
            }
        }
        //5.更新队伍
        Team newTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,newTeam);

        //这里如果newTeam中有字段是null，我们不希望其覆盖oldTeam中非null的字段
        //所以就用updateById默认的更新策略not_null就好，不会将newTeam中为null的属性更新到oldTeam中
        boolean isUpdated = this.updateById(newTeam);
        return isUpdated;
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        //1.校验非空
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求体不能为空");
        }
        SessionData loginUser = userService.getLoginUser(request);
        String userId = loginUser.getUserId();
        Long teamId = teamJoinRequest.getTeamId();
        Team team =  getTeamById(teamId);
        Date expireTime = team.getExpireTime();

        //如果队伍已经过期，则不能加入
        if(expireTime!=null && expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已经过期");
        }
        //4.禁止加入私有的队伍
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(team.getStatus());
        if(TeamStatusEnum.PRIVATE.equals(enumByValue)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能加入私有的队伍");
        }
        //5.如果加入的队伍是加密的,必须密码匹配才可以
        if(TeamStatusEnum.SECRET.equals(enumByValue)){
            String inputPassword = teamJoinRequest.getPassword();
            if(StringUtils.isBlank(inputPassword)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已加密，密码不能为空");
            }
            // TODO 如果队伍需要加密，需要将密码加密存储在数据库中，比对密码时也需要先将密码加密再比对
            if(!inputPassword.equals(team.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不正确");
            }
        }
        //加分布式锁，用户在加入队伍前必须先获得锁
        RLock lock = redissonClient.getLock("friendsGathering:join_team"+String.valueOf(teamId));
        try {
            //尝试去获取锁
            while (true){
                if(lock.tryLock(500,10000,TimeUnit.MILLISECONDS)){
                    //用户最多加入5个队伍（包含创建的队伍）
                    QueryWrapper<UserTeam> userTeamWrapper = new QueryWrapper<>();
                    userTeamWrapper.eq("userId",userId);
                    long count = userTeamService.count(userTeamWrapper);
                    if(count>=5){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多加入5个队伍");
                    }
                    //2.队伍必须存在，只能加入未满队伍
                    Integer maxNum = team.getMaxNum();
                    //获取要加入的队伍的当前人数
                    long numInthisTeam = getNumInthisTeamByTeamId(teamId);
                    userTeamWrapper = new QueryWrapper<>();
                    //如果队伍已满,则不能加入
                    if(numInthisTeam>=maxNum){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
                    }
                    //不能加入自己的队伍
                    if(team.getUserId().equals(Long.valueOf(userId))){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能加入自己的队伍");
                    }
                    //不能重复加入已加入的队伍
                    //获取当前用户已加入过的队伍的队伍列表
                    userTeamWrapper = new QueryWrapper<>();
                    userTeamWrapper.eq("userId",userId);
                    List<UserTeam> userHasJoinTeamList = userTeamService.list(userTeamWrapper);
                    Set<Long> userHasJoinTeamIdSet = userHasJoinTeamList.stream()
                            .map(userTeam -> {
                                return userTeam.getTeamId();
                            }).collect(Collectors.toSet());
                    if(userHasJoinTeamIdSet.contains(teamId)){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能重复加入已经加入过的队伍");
                    }

                    //6.新增 队伍-用户 关联信息，需要给方法加上事务注解
                    UserTeam userTeam = new UserTeam();
                    try {
                        Long longValue = Long.parseLong(userId);
                        userTeam.setUserId(longValue);
                        // 使用 longValue
                    } catch (NumberFormatException e) {
                        // 处理转换异常
                        e.printStackTrace();
                    }
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean isSaved = userTeamService.save(userTeam);
                    return isSaved;
                }
            }
        } catch (InterruptedException e) {
            log.error("join team faild",e);
            return false;
        } finally {
            if(lock.isHeldByCurrentThread()){
                System.out.println("unlock"+Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }
        //1. 校验队伍是否存在
        Long teamId = teamQuitRequest.getId();
        Team team = getTeamById(teamId);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您想要退出的队伍不存在");
        }
        //2. 校验我是否已加入队伍
        SessionData loginUser = userService.getLoginUser(request);
        String userId = loginUser.getUserId();
        try {
            Long longValue = Long.parseLong(userId);
            if(!isInTeam(longValue, teamId)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"您未加入此队伍");
            }
            // 使用 longValue
        } catch (NumberFormatException e) {
            // 处理转换异常
            e.printStackTrace();
        }
        //3. 如果 队伍只剩一人，队伍解散
        long numInthisTeamByTeamId = getNumInthisTeamByTeamId(teamId);
        if(numInthisTeamByTeamId==1){
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId",teamId);
            userTeamQueryWrapper.eq("userId",userId);
            Long userTeamId = userTeamService.list(userTeamQueryWrapper).get(0).getId();
            //需要加上事务注解，避免出现数据不一致现象
            boolean isDeleteTeam = this.removeById(teamId);
            boolean isDeleteUserTeam = userTeamService.removeById(userTeamId);
            return isDeleteTeam&&isDeleteUserTeam;
        }
        //4. 还有其他人
        //   1. 如果是队长退出队伍，权限转移给第二早加入的用户 -- 先来后到（只用取 id 最小的两条数据）
        Long longUserId = Long.valueOf(userId);
        if(team.getUserId().equals(longUserId)){
            QueryWrapper<UserTeam> userTeamWrapper = new QueryWrapper<>();
            userTeamWrapper.eq("teamId", teamId);
            List<UserTeam> userTeamList = userTeamService.list(userTeamWrapper);
            List<Long> idList = userTeamList.stream().map(UserTeam::getId).collect(Collectors.toList());
            Collections.sort(idList);
            //删除当前队长与队伍的关联关系
            boolean isDelete = userTeamService.removeById(idList.get(0));
            //权限转移给第二早加入的用户
            Team curteam = new Team();
            Long nextTeamCreaterUser = userTeamService.getById(idList.get(1)).getUserId();
            curteam.setId(teamId);
            curteam.setUserId(nextTeamCreaterUser);
            boolean isUpdated = this.updateById(curteam);
            return isDelete && isUpdated;
        }
        //   2. 非队长，自己退出队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        Long id = userTeamService.list(userTeamQueryWrapper).get(0).getId();
        boolean isDelete = userTeamService.removeById(id);
        return isDelete;
    }

    /**
     * 解散队伍
     * @param teamDeleteRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTeam(TeamDeleteRequest teamDeleteRequest, SessionData loginUser) {
        //1. 校验请求参数
        if(teamDeleteRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }
        //2. 校验队伍是否存在
        Long teamId = teamDeleteRequest.getId();
        Team team = getTeamById(teamId);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您要加入的队伍不存在或已删除");
        }
        //3. 校验当前用户是不是队伍的队长
        if(!team.getUserId().equals(Long.valueOf(loginUser.getUserId()))){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"操作失败,您不是当前队伍的队长");
        }
        //4. 移除所有加入队伍的关联信息
        //需要加事务注解
        QueryWrapper<UserTeam> userTeamWrapper = new QueryWrapper<>();
        userTeamWrapper.eq("teamId",teamId);
        boolean isUserTeamRemoved = userTeamService.remove(userTeamWrapper);
        if(!isUserTeamRemoved){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍关联信息失败");
        }
        //5. 删除队伍
        return this.removeById(teamId);
    }

    @Override
    public List<Team> listMyJoinTeams(SessionData loginUser) {
        //1.校验参数
        if(loginUser == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未登录");
        }
        Long userId = Long.valueOf(loginUser.getUserId());
        QueryWrapper<Team> teamWrapper = new QueryWrapper<>();
        teamWrapper.eq("userId",userId);

        return null;
    }

    private Boolean isInTeam(Long userId, Long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if(count==0){
            return false;
        }
        return true;
    }

    private long getNumInthisTeamByTeamId(Long teamId) {
        QueryWrapper<UserTeam> userTeamWrapper = new QueryWrapper<>();
        userTeamWrapper.eq("teamId", teamId);
        long numInthisTeam = userTeamService.count(userTeamWrapper);
        return numInthisTeam;
    }

    private Team getTeamById(Long teamId) {
        QueryWrapper<Team> teamWrapper = new QueryWrapper<>();
        teamWrapper.eq("id", teamId);
        List<Team> list = this.list(teamWrapper);
        if(CollectionUtils.isEmpty(list)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"您要加入的队伍不存在");
        }
        return list.get(0);
    }
}




