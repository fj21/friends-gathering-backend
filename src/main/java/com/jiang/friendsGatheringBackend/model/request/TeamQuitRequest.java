package com.jiang.friendsGatheringBackend.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class TeamQuitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 想要退出的队伍id
     */
    private Long id;
}
