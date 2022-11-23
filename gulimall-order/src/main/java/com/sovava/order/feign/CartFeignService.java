package com.sovava.order.feign;

import com.sovava.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/currentUserCartItem")
    @ResponseBody
    List<OrderItemVo> getCurrentUserCartItem();
}
