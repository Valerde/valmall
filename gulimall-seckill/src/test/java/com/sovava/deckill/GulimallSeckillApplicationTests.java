package com.sovava.deckill;

import com.sovava.deckill.service.SeckillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallSeckillApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private SeckillService seckillService;
    @Test
    void testGetLasted3DaysSession(){
        seckillService.uploadSeckillSkuLatest3Days();
    }

}
