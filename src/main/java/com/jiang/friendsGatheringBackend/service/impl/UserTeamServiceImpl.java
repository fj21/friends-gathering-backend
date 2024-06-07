package com.jiang.friendsGatheringBackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiang.friendsGatheringBackend.model.domain.UserTeam;
import com.jiang.friendsGatheringBackend.service.UserTeamService;
import com.jiang.friendsGatheringBackend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author jiang
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-05-25 19:39:03
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




