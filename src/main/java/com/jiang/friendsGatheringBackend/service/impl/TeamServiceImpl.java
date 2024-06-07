package com.jiang.friendsGatheringBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.domain.UserTeam;
import com.jiang.friendsGatheringBackend.model.enums.TeamStatusEnum;
import com.jiang.friendsGatheringBackend.service.TeamService;
import com.jiang.friendsGatheringBackend.mapper.TeamMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

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


    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     */
    @Override
    public long createTeam(Team team, User loginUser) {
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
        Long createrUserId = loginUser.getId();
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
}




