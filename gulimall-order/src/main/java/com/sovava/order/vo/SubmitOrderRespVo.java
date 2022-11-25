package com.sovava.order.vo;

import com.sovava.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderRespVo {
    private OrderEntity order;
    /**
     * 0 成功 ， 别的，错误
     */
    private Integer code;
}
