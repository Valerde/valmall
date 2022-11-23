package com.sovava.order.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.sovava.common.constant.OrderConstant;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.order.feign.CartFeignService;
import com.sovava.order.feign.MemberFeignService;
import com.sovava.order.feign.WareFeignService;
import com.sovava.order.interceptor.LoginUserInterceptor;
import com.sovava.order.vo.MemberAddressVo;
import com.sovava.order.vo.OrderConfirmVo;
import com.sovava.order.vo.OrderItemVo;
import com.sovava.order.vo.SkuHasStockVo;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.order.dao.OrderDao;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        //获取之之前的请求
        RequestAttributes oldRequestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //每一个线程都要获取主线程的上下文环境
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            //远程查询收货地址
            R addressesR = memberFeignService.getAddresses(memberRespVo.getId());
            if (addressesR.getCode() == 0) {
                log.debug("远程查询会员收货地址成功");
                List<MemberAddressVo> memberAddressVos = addressesR.getData(new TypeReference<List<MemberAddressVo>>() {
                });
                orderConfirmVo.setAddress(memberAddressVos);
            }
        }, executor);

        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);//复制原线程的上下文路径
            //远程查询购物车选中的购物项
            List<OrderItemVo> cartItem = cartFeignService.getCurrentUserCartItem();
            orderConfirmVo.setItems(cartItem);
            //feign在远程调用之前会构造很多请求 ， 会丢失浏览器的请求头 ， 没有请求头cookie和session ， cart服务认为用户没有登录
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.selectHasStock(skuIds);
            Map<Long, Boolean> map = new HashMap<>();
            if (r.getCode() == 0) {
                List<SkuHasStockVo> stockVos = r.getData(new TypeReference<List<SkuHasStockVo>>() {
                });
                for (SkuHasStockVo stockVo : stockVos) {
                    map.put(stockVo.getSkuId(), stockVo.getHasStock());
                    log.debug("skuId:{}，stock:{}", stockVo.getSkuId(), stockVo.getHasStock());
                }
            }
            orderConfirmVo.setStocks(map);
        }, executor);


        //查询用户积分
        Integer integration = memberRespVo.getIntegration();
        orderConfirmVo.setIntegration(integration);


        //TODO 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId().toString(), token, 30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);
        try {
            CompletableFuture.allOf(getAddressFuture, getCartFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        log.debug("orderConfirm:{}", orderConfirmVo.toString());
        return orderConfirmVo;
    }

}