package com.sovava.order.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sovava.common.constant.OrderConstant;
import com.sovava.common.exception.NoStockException;
import com.sovava.common.to.OrderTo;
import com.sovava.common.to.SecKillOrderTo;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.order.entity.OrderItemEntity;
import com.sovava.order.entity.PaymentInfoEntity;
import com.sovava.order.enume.OrderStatusEnum;
import com.sovava.order.feign.CartFeignService;
import com.sovava.order.feign.MemberFeignService;
import com.sovava.order.feign.ProductFeignService;
import com.sovava.order.feign.WareFeignService;
import com.sovava.order.interceptor.LoginUserInterceptor;
import com.sovava.order.service.OrderItemService;
import com.sovava.order.service.PaymentInfoService;
import com.sovava.order.to.OrderCreateTo;
import com.sovava.order.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

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
        //????????????????????????
        RequestAttributes oldRequestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //??????????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            //????????????????????????
            R addressesR = memberFeignService.getAddresses(memberRespVo.getId());
            if (addressesR.getCode() == 0) {
                log.debug("????????????????????????????????????");
                List<MemberAddressVo> memberAddressVos = addressesR.getData(new TypeReference<List<MemberAddressVo>>() {
                });
                orderConfirmVo.setAddress(memberAddressVos);
            }
        }, executor);

        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);//?????????????????????????????????
            //???????????????????????????????????????
            List<OrderItemVo> cartItem = cartFeignService.getCurrentUserCartItem();
            orderConfirmVo.setItems(cartItem);
            //feign?????????????????????????????????????????? ??? ?????????????????????????????? ??? ???????????????cookie???session ??? cart??????????????????????????????
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
                    log.debug("skuId:{}???stock:{}", stockVo.getSkuId(), stockVo.getHasStock());
                }
            }
            orderConfirmVo.setStocks(map);
        }, executor);


        //??????????????????
        Integer integration = memberRespVo.getIntegration();
        orderConfirmVo.setIntegration(integration);


        //TODO ?????? ??????
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

    /**
     * ???????????????????????????????????? ??? ????????????????????????????????????????????????????????????
     * ???????????????????????????
     *
     * @param orderSubmitVo
     * @return
     */
    @Transactional
//    @GlobalTransactional //???????????????????????? ??? ??????????????????
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo orderSubmitVo) {
//        Object o = AopContext.currentProxy();
        SubmitOrderRespVo respVo = new SubmitOrderRespVo();
        confirmVoThreadLocal.set(orderSubmitVo);
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        //???????????????????????????
        //?????????????????????token
        String userToken = orderSubmitVo.getOrderToken();
        //??????Redis????????????token
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end\n" +
                "\n";
        //?????????????????????????????????
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), userToken);

//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        if (result == 0L) {

            log.debug("????????????????????????????????????");
            respVo.setCode(1);
            return respVo;
        } else {
            log.debug("??????????????????");
            respVo.setCode(0);
            //????????????
            OrderCreateTo order = createOrder();
            BigDecimal orderPayAmount = order.getOrder().getPayAmount();
            BigDecimal submitPayPrice = orderSubmitVo.getPayPrice();
            log.debug("?????????????????????{}", order.toString());
            if (orderPayAmount.subtract(submitPayPrice).abs().doubleValue() < 200) {
                log.debug("????????????");

                //????????????????????????
                saveOrder(order);
                //????????????,?????????????????????????????????
                //????????????????????????skuId???skuName,num)
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(orderItemVos);
                //???????????????
                R lockR = wareFeignService.orderLock(wareSkuLockVo);
                if (lockR.getCode() == 0) {
                    //?????????
                    respVo.setOrder(order.getOrder());
//                    throw new NoStockException();
//                    int i = 10 / 0;
                    //TODO ?????????????????? ??? ???????????????rabbitMQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    //??????????????????
                    return respVo;
                } else {
                    throw new NoStockException();
//                    respVo.setCode(3);
//                    log.debug("?????????????????????????????????");
//                    return respVo;
                }
            } else {
                log.debug("???????????????????????????");
                respVo.setCode(2);
                return respVo;
            }

        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        LambdaQueryWrapper<OrderEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderEntity::getOrderSn, orderSn);
        OrderEntity one = this.getOne(lqw);
        return one;
    }


    @Override
    public void closeOrder(OrderEntity order) {
        //
        OrderEntity orderFromDb = this.getById(order.getId());
        if (orderFromDb.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity orderToUpdate = new OrderEntity();
            orderToUpdate.setId(order.getId());
            orderToUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderToUpdate);
            //?????????????????????
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderFromDb, orderTo);
            log.debug("?????????MQ???????????????TO??????{}", orderFromDb);
            log.debug("?????????MQ???????????????TO??????{}", orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderFromDb.getOrderSn());

        }


    }

    @Override
    public PayVo getpayVoByOrderSn(String orderSn) {
        OrderEntity orderByOrderSn = getOrderByOrderSn(orderSn);

        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);//?????????
        payVo.setBody(orderByOrderSn.getNote() + "??????");//???????????????
        payVo.setSubject("???????????????");//???????????????
        payVo.setTotal_amount(orderByOrderSn.getTotalAmount().setScale(2, RoundingMode.UP).toString());//??????????????????

        return payVo;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        LambdaQueryWrapper<OrderEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderEntity::getMemberId, memberRespVo.getId());
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), lqw);

        List<OrderEntity> orderEntities = page.getRecords().stream().peek(item -> {
            LambdaQueryWrapper<OrderItemEntity> lqw1 = new LambdaQueryWrapper<>();
            lqw1.eq(OrderItemEntity::getOrderSn, item.getOrderSn());
            List<OrderItemEntity> orderItemEntities = orderItemService.list(lqw1);
            item.setOrderItemEntityList(orderItemEntities);
        }).collect(Collectors.toList());

        page.setRecords(orderEntities);
        return new PageUtils(page);

    }

    @Override
    public String handlePayResult(PayAsyncVo payAsyncVo) {
        //??????????????????
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setOrderSn(payAsyncVo.getOut_trade_no());
        paymentInfoEntity.setTotalAmount(paymentInfoEntity.getTotalAmount());
        paymentInfoEntity.setAlipayTradeNo(paymentInfoEntity.getAlipayTradeNo());
        paymentInfoEntity.setSubject(paymentInfoEntity.getSubject());
        paymentInfoEntity.setCreateTime(paymentInfoEntity.getCreateTime());
        paymentInfoEntity.setCallbackTime(payAsyncVo.getNotify_time());

        paymentInfoService.save(paymentInfoEntity);
        //2 ?????????????????????
        if (payAsyncVo.getTrade_status().equals("TRADE_SUCCESS") || payAsyncVo.getTrade_status().equals("TRADE_FINISH")) {
            //??????????????????
            String outTradeNo = payAsyncVo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    @Override
    public void createSecKillOrder(SecKillOrderTo secKillOrder) {
        if (true) return;
        //TODO??????????????????bug???????????????????????? ??? ????????????
        //TODO: ??????????????????
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(secKillOrder.getOrderSn());
        orderEntity.setMemberId(secKillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = secKillOrder.getSeckillPrice().multiply(new BigDecimal("" + secKillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);
        log.debug("???????????????????????????{}", orderEntity.toString());
        //???????????????
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(secKillOrder.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(secKillOrder.getNum());
        //TODO : ??????????????????
//        productFeignService.getSpuInfoBySkuId(secKillOrder.getSkuId());
        orderItemService.save(orderItemEntity);
        log.debug("??????????????????????????????{}", orderItemEntity.toString());


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
        //1. ???????????????
        String orderSn = IdWorker.getTimeId().substring(20);
        log.debug("???????????????{}", orderSn);
        OrderEntity order = buildOrder(orderCreateTo, orderSubmitVo, orderSn);

        //?????????????????????
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        orderCreateTo.setOrderItems(orderItemEntities);

        //??????
        computePrice(order, orderItemEntities);
        orderCreateTo.setOrder(order);
        return orderCreateTo;

    }

    private void computePrice(OrderEntity order, List<OrderItemEntity> orderItemEntities) {
        //??????????????????
        BigDecimal total = new BigDecimal("0");
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            total = total.add(orderItemEntity.getRealAmount());
            //????????????????????????
        }
        order.setTotalAmount(total);
        //??????????????????
        order.setPayAmount(total.add(order.getFreightAmount()));
        order.setPromotionAmount(new BigDecimal("0.0"));
        order.setIntegrationAmount(new BigDecimal("0"));
        order.setCouponAmount(new BigDecimal("0"));

        order.setIntegration(0);
        order.setGrowth(0);
    }

    /**
     * ????????????
     *
     * @param orderSubmitVo
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(OrderCreateTo orderCreateTo, OrderSubmitVo orderSubmitVo, String orderSn) {
        OrderEntity order = new OrderEntity();
        //??????ID
        order.setMemberId(LoginUserInterceptor.threadLocal.get().getId());

        order.setOrderSn(orderSn);
        R fareR = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fareR.getCode() == 0) {
            FareVo fareVo = fareR.getData(new TypeReference<FareVo>() {
            });
            BigDecimal fare = fareVo.getFare();
            //??????????????????
            order.setFreightAmount(fare);
            orderCreateTo.setFare(fare);
            //?????????????????????
            order.setReceiverCity(fareVo.getAddress().getCity());
            order.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
            order.setReceiverName(fareVo.getAddress().getName());
            order.setReceiverPhone(fareVo.getAddress().getPhone());
            order.setReceiverProvince(fareVo.getAddress().getProvince());
            order.setReceiverRegion(fareVo.getAddress().getRegion());
            order.setReceiverPostCode(fareVo.getAddress().getPostCode());


        }
        //?????????????????????????????????
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        order.setAutoConfirmDay(7);
        order.setDeleteStatus(0);
        return order;
    }

    /**
     * ???????????????????????????
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
                //????????????????????????????????????
                orderItemEntity = buildOrderItem(item, orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return itemEntities;

    }

    /**
     * ????????????????????????
     *
     * @param item
     * @param orderSn
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo item, String orderSn) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //????????????????????????
        orderItemEntity.setOrderSn(orderSn);
        //?????????spu??????
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
        //?????????sku??????
        orderItemEntity.setOrderSn(orderSn);

        orderItemEntity.setSkuId(item.getSkuId());
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuPic(item.getImage());
        String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(item.getCount());
        //???????????? ??? ??????

        //????????????
        orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //????????????????????????
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        //??????????????????????????????
        orderItemEntity.setRealAmount(orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity())));
        return orderItemEntity;
    }

}