package com.sovava.order;

import com.sovava.order.feign.CartFeignService;
import com.sovava.order.vo.OrderItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class FeignTest {
    @Autowired
    private CartFeignService cartFeignService;
    @Test
    public void testCartFeign(){
        List<OrderItemVo> currentUserCartItem =cartFeignService.getCurrentUserCartItem();
        for (OrderItemVo vo:currentUserCartItem){
            log.debug("购物项为：{}",vo.toString());
        }
    }

}
