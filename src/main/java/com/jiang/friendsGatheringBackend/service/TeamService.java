package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.model.dto.TeamQuery;
import com.jiang.friendsGatheringBackend.model.request.TeamDeleteRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamJoinRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamQuitRequest;
import com.jiang.friendsGatheringBackend.model.request.TeamUpdateRequest;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import com.jiang.friendsGatheringBackend.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
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
    long createTeam(Team team, SessionData loginUser);

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
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, SessionData loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    Boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request);

    Boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request);

    /**
     * 解散队伍
     * @param teamDeleteRequest
     * @param loginUser
     * @return
     */
    Boolean deleteTeam(TeamDeleteRequest teamDeleteRequest, SessionData loginUser);

    /**
     * 获取当前用户已加入的队伍
     * @param loginUser
     * @return
     */
    List<Team> listMyJoinTeams(SessionData loginUser);
}
