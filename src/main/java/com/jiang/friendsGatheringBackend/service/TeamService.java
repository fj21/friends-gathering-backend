package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiang.friendsGatheringBackend.model.domain.User;

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
}
