package com.sovava.cart.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.sovava.cart.feign.ProductFeignService;
import com.sovava.cart.interceptor.CartInterceptor;
import com.sovava.cart.service.CartService;
import com.sovava.cart.vo.CartItemVo;
import com.sovava.cart.vo.CartVo;
import com.sovava.cart.vo.SkuInfoVo;
import com.sovava.cart.vo.UserInfoTo;
import com.sovava.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    private final String CART_PREFIX = "valmall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {
        CartItemVo cartItemVo = new CartItemVo();
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            log.debug("购物车中无此商品，执行添加操作");
            addNewToCart(skuId, num, cartItemVo, cartOps);
        } else {
            log.debug("购物车中有此商品，更新数量");
            CartItemVo update = JSON.parseObject(res, CartItemVo.class);
            update.setCount(update.getCount() + num);
            cartItemVo = update;
            String jsonString = JSON.toJSONString(cartItemVo);
            cartOps.put("" + skuId, jsonString);

        }


        return cartItemVo;
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        CartItemVo cartItemVo = new CartItemVo();
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String jsonString = (String) cartOps.get(skuId.toString());
        if (!StringUtils.isEmpty(jsonString)) {
            cartItemVo = JSON.parseObject(jsonString, CartItemVo.class);
        }
        return cartItemVo;
    }

    @Override
    public CartVo cartService() {
        CartVo cartVo = new CartVo();
        //登录
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //登陆了 ,先合并
            //如果临时购物车的数据还没有进行合并 ， 就要合并，
            List<CartItemVo> tempCartItems = getCartItems(userInfoTo.getUserKey());
            if (tempCartItems != null && tempCartItems.size() != 0) {
                //临时购物车有数据 ， 合并
                BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(CART_PREFIX + userInfoTo.getUserKey());
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());

                }
                //清空临时购物车
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }

            //获取登陆后的购物车（包含合并过来的临时购物车数据以及用户登录购物车的数据
            List<CartItemVo> loginCartItems = getCartItems(userInfoTo.getUserId().toString());
            cartVo.setItems(loginCartItems);
        } else {
            //没登录
            List<CartItemVo> cartItems = getCartItems(userInfoTo.getUserKey());
            cartVo.setItems(cartItems);
        }
        return cartVo;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void updateChecked(Long skuId, Integer checked) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(checked == 1);

        String jsonString = JSON.toJSONString(cartItem);
        getCartOps().put("" + skuId, jsonString);
    }

    @Override
    public void updateCount(Long skuId, Integer num) {
        if (num > 0) {
            CartItemVo cartItem = getCartItem(skuId);
            cartItem.setCount(num);
            String jsonString = JSON.toJSONString(cartItem);
            getCartOps().put("" + skuId, jsonString);
        } else {
            getCartOps().delete("" + skuId);
        }

    }

    private void addNewToCart(Long skuId, Integer num, CartItemVo cartItemVo, BoundHashOperations<String, Object, Object> cartOps) {
        //远程查询当前要添加的商品的信息
        R info = productFeignService.info(skuId);
        CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
            if (info.getCode() == 0) {
                log.debug("远程查询商品信息成功");
                SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                cartItemVo.setCount(num);
                cartItemVo.setCheck(true);
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setSkuId(skuId);
            }
        }, executor);

        //远程查询属性信息
        CompletableFuture<Void> getSkuSaleAttrValue = CompletableFuture.runAsync(() -> {
            R r = productFeignService.getSkuSaleAttrValue(skuId);
            if (r.getCode() == 0) {
                log.debug("远程查询商品属性成功");
                cartItemVo.setSkuAttr(r.getData(new TypeReference<List<String>>() {
                }));
            } else {
                cartItemVo.setSkuAttr(new ArrayList<String>());
            }
        }, executor);

        //编排线程
        try {
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValue).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        String jsonString = JSON.toJSONString(cartItemVo);
        cartOps.put("" + skuId, jsonString);
    }

    /**
     * 获取到要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();
        String cartKey = "";
        if (userId != null) {
            log.debug("说明已经登陆了");
            cartKey = CART_PREFIX + userId;
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        //判断当前购物车里面有无此物品
        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(cartKey);
        return boundHashOps;
    }

    /**
     * 获取userStr下的购物车数据
     *
     * @param userStr
     * @return
     */
    private List<CartItemVo> getCartItems(String userStr) {
        String cartKey = CART_PREFIX + userStr;
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        List<CartItemVo> cartItemVos = null;
        if (values != null && values.size() > 0) {
            cartItemVos = values.stream().map((item) -> {

                String jsonString = (String) item;
                CartItemVo cartItemVo = JSON.parseObject(jsonString, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());

        }
        return cartItemVos;
    }

}
