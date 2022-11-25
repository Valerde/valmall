package com.sovava.order.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sovava.common.constant.OrderConstant;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.order.dao.OrderItemDao;
import com.sovava.order.entity.OrderItemEntity;
import com.sovava.order.enume.OrderStatusEnum;
import com.sovava.order.feign.CartFeignService;
import com.sovava.order.feign.MemberFeignService;
import com.sovava.order.feign.ProductFeignService;
import com.sovava.order.feign.WareFeignService;
import com.sovava.order.interceptor.LoginUserInterceptor;
import com.sovava.order.service.OrderItemService;
import com.sovava.order.to.OrderCreateTo;
import com.sovava.order.vo.*;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemService orderItemService;

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
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

    @Autowired
    private ProductFeignService productFeignService;

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


        //TODO 防重 令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().setIfAbsent(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId().toString(), token, 30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);
        try {
            CompletableFuture.allOf(getAddressFuture, getCartFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        log.debug("orderConfirm:{}", orderConfirmVo.toString());
        return orderConfirmVo;
    }

    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo orderSubmitVo) {
        SubmitOrderRespVo respVo = new SubmitOrderRespVo();
        confirmVoThreadLocal.set(orderSubmitVo);
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        //令牌验证保证原子性
        //获取用户携带的token
        String userToken = orderSubmitVo.getOrderToken();
        //获取Redis中存储的token
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end\n" +
                "\n";
        //院子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), userToken);

//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        if (result == 0L) {

            log.debug("错误信息：令牌验证不通过");
            respVo.setCode(1);
            return respVo;
        } else {
            log.debug("令牌验证通过");
            respVo.setCode(0);
            //创建订单
            OrderCreateTo order = createOrder();
            BigDecimal orderPayAmount = order.getOrder().getPayAmount();
            BigDecimal submitPayPrice = orderSubmitVo.getPayPrice();
            log.debug("创建的订单为：{}", order.toString());
            if (orderPayAmount.subtract(submitPayPrice).abs().doubleValue() < 200) {
                log.debug("验价成功");

                //保存订单到数据库
                saveOrder(order);
                //库存锁定,只要有异常回滚订单数据
                //订单号，订单项（skuId，skuName,num)
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(orderItemVos);
                R lockR = wareFeignService.orderLock(wareSkuLockVo);
                if (lockR.getCode() == 0) {
                    respVo.setOrder(order.getOrder());
                    return respVo;
                } else {
                    respVo.setCode(3);
                    log.debug("错误信息：库存锁定错误");
                    return respVo;
                }
            } else {
                log.debug("错误信息：验价错误");
                respVo.setCode(2);
                return respVo;
            }

        }
    }

    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.baseMapper.insert(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        //1. 生成订单号
        String orderSn = IdWorker.getTimeId().substring(20);
        log.debug("订单号为：{}", orderSn);
        OrderEntity order = buildOrder(orderCreateTo, orderSubmitVo, orderSn);

        //获取订单项信息
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        orderCreateTo.setOrderItems(orderItemEntities);

        //验价
        computePrice(order, orderItemEntities);
        orderCreateTo.setOrder(order);
        return orderCreateTo;

    }

    private void computePrice(OrderEntity order, List<OrderItemEntity> orderItemEntities) {
        //订单价格相关
        BigDecimal total = new BigDecimal("0");
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            total = total.add(orderItemEntity.getRealAmount());
            //省略价格优惠相关
        }
        order.setTotalAmount(total);
        //设置应付总额
        order.setPayAmount(total.add(order.getFreightAmount()));
        order.setPromotionAmount(new BigDecimal("0.0"));
        order.setIntegrationAmount(new BigDecimal("0"));
        order.setCouponAmount(new BigDecimal("0"));

        order.setIntegration(0);
        order.setGrowth(0);
    }

    /**
     * 构建订单
     *
     * @param orderSubmitVo
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(OrderCreateTo orderCreateTo, OrderSubmitVo orderSubmitVo, String orderSn) {
        OrderEntity order = new OrderEntity();
        //会员ID
        order.setMemberId(LoginUserInterceptor.threadLocal.get().getId());

        order.setOrderSn(orderSn);
        R fareR = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fareR.getCode() == 0) {
            FareVo fareVo = fareR.getData(new TypeReference<FareVo>() {
            });
            BigDecimal fare = fareVo.getFare();
            //设置运费信息
            order.setFreightAmount(fare);
            orderCreateTo.setFare(fare);
            //设置收货人信息
            order.setReceiverCity(fareVo.getAddress().getCity());
            order.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
            order.setReceiverName(fareVo.getAddress().getName());
            order.setReceiverPhone(fareVo.getAddress().getPhone());
            order.setReceiverProvince(fareVo.getAddress().getProvince());
            order.setReceiverRegion(fareVo.getAddress().getRegion());
            order.setReceiverPostCode(fareVo.getAddress().getPostCode());


        }
        //设置订单的相关状态信息
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        order.setAutoConfirmDay(7);
        order.setDeleteStatus(0);
        return order;
    }

    /**
     * 构建所有订单项数据
     *
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItem = cartFeignService.getCurrentUserCartItem();
        List<OrderItemEntity> itemEntities = new ArrayList<>();
        if (currentUserCartItem != null && currentUserCartItem.size() > 0) {
            itemEntities = currentUserCartItem.stream().map((item) -> {
                OrderItemEntity orderItemEntity = new OrderItemEntity();
                //需要确定每个购物项的价格
                orderItemEntity = buildOrderItem(item, orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return itemEntities;

    }

    /**
     * 构建缪一个订单项
     *
     * @param item
     * @param orderSn
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo item, String orderSn) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //订单信息，订单号
        orderItemEntity.setOrderSn(orderSn);
        //商品的spu信息
        Long skuId = item.getSkuId();
        R skuInfoR = productFeignService.getSpuInfoBySkuId(skuId);
        if (skuInfoR.getCode() == 0) {
            SpuInfoVo spuInfoVo = skuInfoR.getData(new TypeReference<SpuInfoVo>() {
            });
            orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
            orderItemEntity.setSpuId(spuInfoVo.getId());
            orderItemEntity.setSpuName(spuInfoVo.getSpuName());
            orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
        }
        //商品的sku信息
        orderItemEntity.setOrderSn(orderSn);

        orderItemEntity.setSkuId(item.getSkuId());
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuPic(item.getImage());
        String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(item.getCount());
        //优惠信息 ， 忽略

        //积分信息
        orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //订单项的价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        //当前订单项的实际金额
        orderItemEntity.setRealAmount(orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity())));
        return orderItemEntity;
    }

}