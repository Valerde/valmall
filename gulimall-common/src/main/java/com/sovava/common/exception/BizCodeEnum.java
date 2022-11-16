package com.sovava.common.exception;

public enum BizCodeEnum {

    UNKNOWN_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数校验异常"),

    SMS_CODE_EXCEPTION(10002, "验证码获取频率太高，请稍候再试"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),

    USER_EXIST_EXCEPTION(15001, "用户已存在异常"),

    PHONE_EXIST_EXCEPTION(15002, "手机号已存在异常"),

    LOGINACCT_PASSWORD_INVALID_EXCEPTION(15003,"账号或密码错误");


    private int code;
    private String message;

    BizCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
