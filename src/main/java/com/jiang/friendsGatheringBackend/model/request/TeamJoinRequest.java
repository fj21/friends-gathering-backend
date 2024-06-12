package com.jiang.friendsGatheringBackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求体
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}
