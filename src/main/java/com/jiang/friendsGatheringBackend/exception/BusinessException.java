package com.jiang.friendsGatheringBackend.exception;

import com.jiang.friendsGatheringBackend.common.ErrorCode;
import lombok.Data;

/**
 * 自定义异常类
 *
 * @Author jiang
 */
@Data
public class BusinessException extends RuntimeException{
    /**
     * 异常码
     */
    private final int code;
    /**
     * 描述
     */
    private final String description;
    public BusinessException(int code,String msg,String description){
        super(msg);
        this.code=code;
        this.description=description;
    }

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMsg());
        this.code=errorCode.getCode();
        this.description=errorCode.getDescrption();
    }

    public BusinessException(ErrorCode errorCode,String description){
        super(errorCode.getMsg());
        this.code=errorCode.getCode();
        this.description=errorCode.getDescrption();
    }
}
