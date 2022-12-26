package com.sovava.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.sovava.order.config.AlipayTemplate;
import com.sovava.order.service.OrderService;
import com.sovava.order.vo.PayAsyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class OrderPayedListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    /**
     * 只要支付宝收不到success ，支付宝将会努力提交 <br>
     * TODO 由于未做内网穿透 ， 所以没有接受支付宝的异步通知
     *
     * @return
     */
    @PostMapping("/payed/notify")
    public String handleAliPayed(PayAsyncVo payAsyncVo, HttpServletRequest request) throws AlipayApiException {
        log.debug("支付宝的异步通知信息为：{}", payAsyncVo);
        //验签
        Map<String, String> param = new HashMap<>();
        boolean b = AlipaySignature.rsaCertCheckV1(param, alipayTemplate.getAlipay_public_key(), alipayTemplate.getCharset());

        if (b) {
            String result = orderService.handlePayResult(payAsyncVo);
            //验证通过
            log.debug("验证通过");
            return "success";
        } else {
            log.debug("验证不通过");
            return "error";
        }


    }
}
