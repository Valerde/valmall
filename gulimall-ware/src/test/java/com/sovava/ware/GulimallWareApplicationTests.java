package com.sovava.ware;

import com.sovava.ware.service.WareInfoService;
import com.sovava.ware.service.WareSkuService;
import com.sovava.ware.vo.SkuHasStockVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
class GulimallWareApplicationTests {

    @Test
    void contextLoads() {
    }
    @Autowired
    private WareSkuService wareSkuService;

    @Test
    public void testStock(){
        List<Long> list = new ArrayList<>();
        list.add(10L);
        List<SkuHasStockVo> stockBySkuIds = wareSkuService.getSkusHasStockBySkuIds(list);
        for (SkuHasStockVo stockBySkuId : stockBySkuIds) {
            log.debug(stockBySkuId.toString());
        }

    }

    @Autowired
    private WareInfoService wareInfoService;
    @Test
    public void testShipping(){
//        BigDecimal fare = wareInfoService.getFare(1);
//        log.debug(fare.toString());
    }

}
