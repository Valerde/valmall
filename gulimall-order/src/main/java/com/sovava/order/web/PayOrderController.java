package com.sovava.order.web;

import com.alipay.api.AlipayApiException;
import com.sovava.order.config.AlipayTemplate;
import com.sovava.order.service.OrderService;
import com.sovava.order.vo.PayVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class PayOrderController {

    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private OrderService orderService;

    /**
     * 将支付页让浏览器显示<br>
     * 支付成功后，返回支付成功页
     * @param orderSn
     * @return
     * @throws AlipayApiException
     */
    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getpayVoByOrderSn(orderSn);

        String pay = alipayTemplate.pay(payVo);
        log.debug("支付宝返回数据为：{}", pay);
        return pay;
    }
}
