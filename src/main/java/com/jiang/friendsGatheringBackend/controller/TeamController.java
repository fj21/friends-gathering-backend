package com.jiang.friendsGatheringBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiang.friendsGatheringBackend.common.BaseResponse;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.common.ResultUtils;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.domain.UserTeam;
import com.jiang.friendsGatheringBackend.model.dto.TeamQuery;
import com.jiang.friendsGatheringBackend.model.request.TeamAddRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamUpdateRequest;
import com.jiang.friendsGatheringBackend.model.vo.TeamUserVO;
import com.jiang.friendsGatheringBackend.service.TeamService;
import com.jiang.friendsGatheringBackend.service.UserService;
import com.jiang.friendsGatheringBackend.service.UserTeamService;
import com.jiang.friendsGatheringBackend.service.impl.UserTeamServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 创建队伍接口
     *
     * @param teamAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long>  addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if(teamAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍信息为空");
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        long teamId = teamService.createTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 查询队伍接口，返回符合查询条件的队伍列表
     * @param teamQuery
     * @param request
     * @return
     */
    @PostMapping("/list")
    public BaseResponse<List<TeamUserVO>> queryTeams(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"查询条件不能为空");
        }
        boolean isAdmin = userService.isAdmin(request);
        //得到满足查询条件的队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        //得到满足查询条件的队伍id列表
        List<Long> teamIdList = teamList.stream()
                                        .map(teamUserVO -> teamUserVO.getId())
                                        .collect(Collectors.toList());
        //4.判断当前用户是否已加入了队伍
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        //判断用户是否在满足查询条件的这些队伍中
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        queryWrapper.in("teamId",teamIdList);
        Set<Long> userInTeamIdSet = userTeamService.list(queryWrapper).stream()
                .map(userTeam -> userTeam.getTeamId())
                .collect(Collectors.toSet());
        teamList.stream().forEach(teamUserVO -> {
            //如果当前用户加入了队伍
            boolean isJoin = userInTeamIdSet.contains(teamUserVO.getId());
            teamUserVO.setHasJoin(isJoin);
        });
        //5.查询已加入队伍的人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId",teamIdList);
        //userTeamList是符合查询要求的team和其成员的列表
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        Map<Long, List<UserTeam>> teamIdToUserListMap =
                userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.stream().forEach(teamUserVO -> {
            teamUserVO.setHasJoinNum(teamIdToUserListMap.getOrDefault(teamUserVO.getId(),new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }


    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        //1.判断请求参数是否为空
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"您提供的修改请求为空");
        }

        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(result);
    }



}
