package com.sovava.cart.service;

import com.sovava.cart.vo.CartItemVo;
import com.sovava.cart.vo.CartVo;

import java.util.List;

public interface CartService {
    /**
     * 将商品添加或合并到购物车
     *
     * @param skuId
     * @param num
     * @return
     */
    CartItemVo addToCart(Long skuId, Integer num);

    /**
     * 获取购物车内属性
     *
     * @param skuId
     * @return
     */
    CartItemVo getCartItem(Long skuId);

    CartVo cartService();

    void clearCart(String cartKey);

    /**
     * 更改购物项的选中状态
     * @param skuId
     * @param checked
     */
    void updateChecked(Long skuId, Integer checked);

    void updateCount(Long skuId, Integer num);

    List<CartItemVo> getUserCartItems(String toString);
}
