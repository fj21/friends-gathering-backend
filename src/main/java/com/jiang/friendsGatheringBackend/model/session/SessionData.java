package com.jiang.friendsGatheringBackend.model.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SessionData {
    String userId;
    String userName;
    int role;
    String tags;
}
