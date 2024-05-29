package com.jiang.friendsGatheringBackend.common;

/**
 * 返回工具类
 *
 * @Author jiang
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static  BaseResponse error(ErrorCode errorCode){
        return new BaseResponse(errorCode);
    }


    /**
     * 失败
     *
     * @param errorCode
     * @param decrption
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String decrption){
        return new BaseResponse(errorCode.getCode(),null,errorCode.getMsg(),decrption);
    }

    public static BaseResponse error(ErrorCode errorCode,String msg,String descrption){
        return new BaseResponse(errorCode.getCode(),errorCode.getMsg(),errorCode.getDescrption());
    }


    /**
     * 失败
     *
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(int code, String message, String description) {
        return new BaseResponse(code,null,message,description);
    }
}
