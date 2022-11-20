package com.sovava.cart.vo;

import lombok.Data;

@Data
public class UserInfoTo {
    /**
     * 如果已经登陆，用户id
     */
    private Long userId;
    /**
     * 临时用户的key
     */
    private String userKey;
    /**
     * 是否为临时用户
     */
    private boolean isTempUser = false;

}
