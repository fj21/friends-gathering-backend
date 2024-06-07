package com.jiang.friendsGatheringBackend.model.enums;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 队伍状态（公开、私有、加密）枚举类
 *
 * @author jiang
 */
@Getter
public enum TeamStatusEnum {

    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");


    public int value;
    public String text;

    TeamStatusEnum(int value,String text){
        this.value = value;
        this.text = text;
    }

    /**
     * 通过 value 返回对应的队伍状态（公开、私有、加密）
     * @param value
     * @return
     */
    public static TeamStatusEnum getEnumByValue(Integer value){
        if(value == null){
            return null;
        }
        //获取所有的enum
        TeamStatusEnum[] values = TeamStatusEnum.values();
        //遍历enum数组,若enum的value与传入的参数相等,则返回对应的enum
        for (TeamStatusEnum curTeamStatusEnum : values) {
            if(curTeamStatusEnum.getValue() == value){
                return curTeamStatusEnum;
            }
        }
        return null;
    }

}
