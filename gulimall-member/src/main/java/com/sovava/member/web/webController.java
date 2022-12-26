package com.sovava.member.web;

import com.alibaba.fastjson2.TypeReference;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.R;
import com.sovava.member.feign.OrderFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class webController {
    @Autowired
    private OrderFeignService orderFeignService;

    @GetMapping("/orderList")
    public String orderList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                            Model model) {

        Map<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());
        R r = orderFeignService.listWithItem(page);
        model.addAttribute("orders", r);
        log.debug("返回的R为：{}", r.toString());
        log.debug("用户的订单为：{}", r.getData("page", new TypeReference<PageUtils>() {
        }).toString());

        return "orderList";
    }
}
