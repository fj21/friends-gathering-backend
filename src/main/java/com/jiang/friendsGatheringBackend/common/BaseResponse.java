package com.jiang.friendsGatheringBackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 *通用返回类
 *
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    /**
     * 状态码
     */
    private int code;
    /**
     * 数据
     */
    private T data;
    /**
     * 消息
     */
    private String msg;
    /**
     * 描述
     */
    private String descrption;
    public BaseResponse(int code,T data,String msg,String descrption){
        this.code=code;
        this.data=data;
        this.msg=msg;
        this.descrption=descrption;
    }
    public BaseResponse(int code, T data,String msg){
        this(code,data,msg,"");
    }
    public BaseResponse(int code, T data){
        this(code,data,"","");
    }
    public BaseResponse(int code){
        this(code,null,"","");
    }
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMsg(),errorCode.getDescrption());
    }
    public BaseResponse(ErrorCode errorCode,String msg){
        this(errorCode.getCode(),null,errorCode.getMsg(),msg);
    }
}
