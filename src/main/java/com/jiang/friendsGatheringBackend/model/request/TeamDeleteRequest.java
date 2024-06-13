package com.jiang.friendsGatheringBackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除队伍请求体
 */
@Data
public class TeamDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 想要退出的队伍id
     */
    private Long id;
}
