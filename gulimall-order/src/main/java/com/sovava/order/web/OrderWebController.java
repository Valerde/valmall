package com.sovava.order.web;

import com.sovava.order.service.OrderService;
import com.sovava.order.vo.OrderConfirmVo;
import com.sovava.order.vo.OrderSubmitVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@Slf4j
public class OrderWebController {
    @Autowired
    private OrderService orderService;


    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) {

        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmOrderData", confirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder( OrderSubmitVo orderSubmitVo) {
        log.debug("提交的表单信息为：{}", orderSubmitVo.toString());
        //下单
        //下单成功，支付首页
        //下单失败，回单订单确认页
        return "detail";
    }

}
