package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.dto.TeamQuery;
import com.jiang.friendsGatheringBackend.model.request.TeamUpdateRequest;
import com.jiang.friendsGatheringBackend.model.vo.TeamUserVO;

import java.util.List;

/**
* @author jiang
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-05-25 19:38:45
*/
public interface TeamService extends IService<Team> {

    /**
     * 添加队伍
     *
     * @param team
     * @param loginUser
     */
    long createTeam(Team team, User loginUser);

    /**
     * 查询队伍
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

}
