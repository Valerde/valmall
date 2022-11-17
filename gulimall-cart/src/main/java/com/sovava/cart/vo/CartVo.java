package com.sovava.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 寻要计算的属性必须重写get方法 ， 保证每次获取属性都要重新计算
 */
public class CartVo {

    private List<CartItemVo> items;
    /**
     * 商品总数
     */
    private Integer countNum;
    /**
     * 商品类型数量
     */
    private Integer countType;
    /**
     * 商品总价
     */
    private BigDecimal totalAmount;
    /**
     * 减免
     */
    private BigDecimal reduce = new BigDecimal("0");

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if (this.items != null && this.items.size() != 0) {
            for (CartItemVo cartItem : this.items) {
                count += cartItem.getCount();
            }
        }
        return count;
    }


    public Integer getCountType() {
        int count = 0;
        if (this.items != null) {
            count = this.items.size();
        }
        return count;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal totalAmount = new BigDecimal("0");
        //计算购物项总价
        if (this.items != null && this.items.size() != 0) {
            for (CartItemVo cartItem : this.items) {
                totalAmount = totalAmount.add(cartItem.getTotalPrice());
            }
        }
        //减去总价
        totalAmount = totalAmount.subtract(getReduce());
        return totalAmount;
    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
