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
import com.jiang.friendsGatheringBackend.model.request.*;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import com.jiang.friendsGatheringBackend.model.vo.TeamUserVO;
import com.jiang.friendsGatheringBackend.service.TeamService;
import com.jiang.friendsGatheringBackend.service.UserService;
import com.jiang.friendsGatheringBackend.service.UserTeamService;
import com.jiang.friendsGatheringBackend.service.impl.UserTeamServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.domain.geo.RadiusShape;
import org.springframework.web.bind.annotation.*;

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
        SessionData loginUser = userService.getLoginUser(request);
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
    public BaseResponse<List<TeamUserVO>> queryTeams(@RequestBody TeamQuery teamQuery,HttpServletRequest request){
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
        SessionData loginUser = userService.getLoginUser(request);
        Long userId = Long.valueOf(loginUser.getUserId());
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


    /**
     * 更新队伍信息接口
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        //1.判断请求参数是否为空
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"您提供的修改请求为空");
        }

        SessionData loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 加入队伍接口
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        //1.判断请求体是否为空
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }
        Boolean result = teamService.joinTeam(teamJoinRequest, request);
        if(result!=null && result){
            return ResultUtils.success(result);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"加入队伍失败");

    }

    /**
     * 退出队伍接口
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(TeamQuitRequest teamQuitRequest,HttpServletRequest request){
        //1.判断请求体是否为空
        if(teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }
        Boolean result = teamService.quitTeam(teamQuitRequest, request);
        if(result!=null && result){
            return ResultUtils.success(result);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"退出队伍失败");
    }

    /**
     * 解散队伍
     * @param teamDeleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody TeamDeleteRequest teamDeleteRequest,HttpServletRequest request){
        //1.校验请求参数
        if(teamDeleteRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        SessionData loginUser = userService.getLoginUser(request);
        Boolean result = teamService.deleteTeam(teamDeleteRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 获取我加入的队伍
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeam(TeamQuery teamQuery, HttpServletRequest request){
        SessionData loginUser = userService.getLoginUser(request);
        Long userId = Long.valueOf(loginUser.getUserId());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        List<Long> teamIdList = userTeamList.stream().map(UserTeam::getTeamId).toList();
        teamQuery.setIdList(teamIdList);
        List<TeamUserVO> teamUserVOS = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamUserVOS);
    }

    /**
     * 获取我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeam(TeamQuery teamQuery,HttpServletRequest request){
        SessionData loginUser = userService.getLoginUser(request);
        Long userId = Long.valueOf(loginUser.getUserId());
        QueryWrapper<Team> teamWrapper = new QueryWrapper<>();
        teamWrapper.eq("userId",userId);
        List<Team> teamList = teamService.list(teamWrapper);
        List<Long> idList = teamList.stream().map(Team::getId).toList();
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamUserVOS = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamUserVOS);
    }
}
