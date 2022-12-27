package com.sovava.coupon;

import com.sovava.coupon.service.SeckillSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallCouponApplicationTests {

    @Autowired
    private SeckillSessionService seckillSessionService;
    @Test
    void contextLoads() {
        seckillSessionService.getLasted3DaysSession();
    }

}
