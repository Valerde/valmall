package com.sovava.member.exception;

public class PhoneExistException extends RuntimeException{
    public PhoneExistException() {
        super("手机号已经存在");
    }
}
