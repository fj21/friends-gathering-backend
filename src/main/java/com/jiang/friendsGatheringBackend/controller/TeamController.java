package com.jiang.friendsGatheringBackend.controller;

import com.jiang.friendsGatheringBackend.common.BaseResponse;
import com.jiang.friendsGatheringBackend.common.ErrorCode;
import com.jiang.friendsGatheringBackend.common.ResultUtils;
import com.jiang.friendsGatheringBackend.exception.BusinessException;
import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.request.TeamAddRequest;
import com.jiang.friendsGatheringBackend.service.TeamService;
import com.jiang.friendsGatheringBackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

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
}
