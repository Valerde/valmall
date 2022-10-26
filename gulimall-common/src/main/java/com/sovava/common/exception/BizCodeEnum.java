package com.sovava.common.exception;

public enum BizCodeEnum {

    UNKNOWN_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10010,"参数校验异常");
    private int code;
    private String message;
    BizCodeEnum(int code,String message){
        this.code = code;
        this.message = message;
    }

    public int getCode(){
        return this.code;
    }

    public String getMessage(){
        return this.message;
    }
}
