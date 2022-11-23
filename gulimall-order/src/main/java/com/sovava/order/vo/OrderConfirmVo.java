package com.sovava.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要的数据
 */
@ToString
public class OrderConfirmVo {
    /**
     * 收货地址列表 ums_member_receive_address
     */
    @Getter
    @Setter
    private List<MemberAddressVo> address;
    /**
     * 所有选中的购物项
     */
    @Getter
    @Setter
    private List<OrderItemVo> items;

    /**
     * 订单防重令牌
     */
    @Getter
    @Setter
    private String orderToken = "haha";
    //发票....

    // 优惠券信息
    @Getter @Setter
    Map<Long,Boolean> stocks;

    /**
     * 积分
     */
    @Getter
    @Setter
    private Integer integration;

    /**
     * 订单总额 ， 需要进行计算
     */
    private BigDecimal total;

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getCount())));
            }
        }
        this.total = total;
        return this.total;
    }

    /**
     * 应付总额
     */
    private BigDecimal totalPrice;

    public BigDecimal getTotalPrice() {
        return total;
    }

    private Integer count;

    public Integer getCount() {
        int total = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                total += item.getCount();
            }
        }
        this.count = total;
        return this.count;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
