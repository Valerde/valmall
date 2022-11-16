package com.sovava.thirdparty.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsComponent {

    /**
     * 发送验证码
     *
     * @param phone
     * @param code
     */
    public void sendSmsCode(String phone, String code) {
        log.error("发送短信成功");
    }
}
