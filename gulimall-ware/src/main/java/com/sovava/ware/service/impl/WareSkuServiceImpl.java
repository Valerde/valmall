package com.sovava.ware.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.to.OrderTo;
import com.sovava.common.to.StockDetailTo;
import com.sovava.common.to.StockLockedTo;
import com.sovava.common.utils.R;
import com.sovava.ware.entity.WareOrderTaskDetailEntity;
import com.sovava.ware.entity.WareOrderTaskEntity;
import com.sovava.ware.exception.NoStockException;
import com.sovava.ware.feign.OrderFeignService;
import com.sovava.ware.feign.ProductFeignService;
import com.sovava.ware.service.WareOrderTaskDetailService;
import com.sovava.ware.service.WareOrderTaskService;
import com.sovava.ware.vo.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.WareSkuDao;
import com.sovava.ware.entity.WareSkuEntity;
import com.sovava.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
@Slf4j
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Resource
    ProductFeignService productFeignService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private WareOrderTaskService orderTaskService;
    @Resource
    private WareOrderTaskDetailService orderTaskDetailService;
    @Resource
    private OrderFeignService orderFeignService;
    @Resource
    private WareSkuDao wareSkuDao;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * wareId: 123,//仓库id
     * skuId: 123//商品id
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();

        String wareId = (String) params.get("wareId");
        lqw.eq(!StringUtils.isEmpty(wareId), WareSkuEntity::getWareId, wareId);

        String skuId = (String) params.get("skuId");
        lqw.eq(!StringUtils.isEmpty(skuId), WareSkuEntity::getSkuId, skuId);

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    /**
     * 采购成功后向仓库中添加
     *
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果还没有这个库存记录
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WareSkuEntity::getWareId, wareId);
        lqw.eq(WareSkuEntity::getSkuId, skuId);

        List<WareSkuEntity> list = this.list(lqw);
        if (list == null || list.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStockLocked(0);
            String skuName = "";
            //远程查询sku的name ， 如果失败时， 不回滚
            R info = productFeignService.info(skuId);
            try {
                if (info.getCode() == 0) {
                    Map<String, Object> map = (Map<String, Object>) info.get("skuInfo");
                    skuName = (String) map.get("skuName");
                    wareSkuEntity.setSkuName(skuName);
                }
            } catch (Exception ex) {

            }

            this.save(wareSkuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStockBySkuIds(List<Long> skuIds) {
        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map((item) -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前sku总库存量
            Long stockNum = this.baseMapper.getSkuIdStock(item);
            if (stockNum == null) {
                log.debug("getSkusHasStockBySkuIds方法体：stockNum == null");
                stockNum = 0L;
            }
            vo.setSkuId(item);
            vo.setHasStock(stockNum > 0);
            log.debug("getSkusHasStockBySkuIds方法体：{}", vo.getHasStock());
            return vo;

        }).collect(Collectors.toList());

        return skuHasStockVos;
    }

    /**
     * 库存解锁的场景：
     * <br>
     * 1. 下订单成功，没有按时支付成功或者被用户手动取消 ， 解锁库存<br>
     * 2. 下订单成功，锁定库存也成功 ， 但是接下来的业务失败,导致订单回滚，之前锁定的库存自动解锁（分布式事务）<br>
     *
     * @param vo
     * @return
     * @throws NoStockException
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLock(WareSkuLockVo vo) throws NoStockException {
        //保存库存工作单的详情，为了追溯
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(wareOrderTaskEntity);

        //本来应该按照就近的仓库， 锁定库存
        //找到每个商品在那个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> hasStocks = locks.stream().map(item -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            //查询这个商品在哪里有库存
            List<Long> skuIds = this.baseMapper.listWareIdHasStock(skuId);
            skuWareHasStock.setWareId(skuIds);
            skuWareHasStock.setNum(item.getCount());
            skuWareHasStock.setSkuId(skuId);
            return skuWareHasStock;
        }).collect(Collectors.toList());
        //2.锁定库存
        boolean allLocked = true;
        for (SkuWareHasStock hasStock : hasStocks) {
            boolean skuLocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            //如果每一个商品都锁成功，讲当前商品锁定了几件发送给MQ
            //如果库存锁定失败，前面保存的工作单就回滚了，发送出去的消息也没有问题 ， 因为在数据库中查不到指定的ID，所以不用解锁。
            //   其实这种方案是不合理的 ，
            for (Long wareId : wareIds) {
                int i = this.baseMapper.lockSkuIdStock(hasStock.getNum(), skuId, wareId);
                if (i == 1) {
                    skuLocked = true;
                    //TO DO: 告诉MQ库存锁定成功

                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity
                            = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), wareOrderTaskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(wareOrderTaskDetailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                    //防止回滚以后 ， 找不到数据
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                }
            }
            if (!skuLocked) {
                log.debug("当前商品没库存");
                throw new NoStockException(skuId);
            }
            allLocked = skuLocked;
        }

        return allLocked;
    }

    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {
        log.debug("库存释放信息为：{}", stockLockedTo.toString());

        StockDetailTo detailTo = stockLockedTo.getDetailTo();
        Long detailId = detailTo.getId();
        //1. 查询数据库中有没有这个detail数据 ， 如果没有就没有必要解锁了
        WareOrderTaskDetailEntity ifDetailId = orderTaskDetailService.getById(detailId);
        if (ifDetailId != null) {
            // 数据库中有这一个记录 ， 证明库存解锁成功了
            //还需要查询订单情况
            //      1. 没有这一订单，必须解锁
            //      2. 有这个订单 ， 如果1）已取消，解锁库存 2）未取消不能解锁库存
            Long orderTaskId = stockLockedTo.getId();
            WareOrderTaskEntity ifOrderTask = orderTaskService.getById(orderTaskId);
            String orderSn = ifOrderTask.getOrderSn();
            //根据订单号查询有无这一订单
            R orderStatusR = orderFeignService.getOrderStatus(orderSn);
            if (orderStatusR.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderVo = orderStatusR.getData(new TypeReference<OrderVo>() {
                });
                if (orderVo == null || orderVo.getStatus() == 4) {
                    //订单已经被取消了
                    log.debug("/订单已经被取消了");
                    if (ifDetailId.getLockStatus() == 1) {
                        log.debug("当前库存工作单已锁定但未解锁");
                        unLock(detailTo.getSkuId(), detailTo.getWareId(), detailTo.getSkuNum(), detailId);
                    }
                }
            } else {
                //远程查询失败 TO DO throw Exception
                throw new RuntimeException("远程服务失败");
            }
        } else {
            //数据库中没有这一个记录 ， 不需要解锁
            log.debug("数据库中没有这一个记录 ， 不需要解锁");
        }
    }

    //防止订单服务卡顿 ， 导致库存状态一直改不了 ， 库存消息优先到期 ，那么库存方法优先到期，库存服务什么都不做就走了，导致库存一直不能释放
    @Override
    public void unlockStock(String orderSn) {
//        log.debug("接收到的订单为：{}", order.toString());
        log.debug("接收到的订单为：{}", orderSn);
//        String orderSn = order.getOrderSn();
        WareOrderTaskEntity wareOrderTaskEntity = orderTaskService.getOrderByTaskOrderSn(orderSn);
        Long taskId = wareOrderTaskEntity.getId();
        List<WareOrderTaskDetailEntity> wareOrderTaskDetailEntities = orderTaskDetailService.listByTaskId(taskId);

        wareOrderTaskDetailEntities.forEach(this::unLock);
    }

    private void unLock(WareOrderTaskDetailEntity wareOrderTaskDetailEntity) {
        unLock(wareOrderTaskDetailEntity.getSkuId(), wareOrderTaskDetailEntity.getWareId(), wareOrderTaskDetailEntity.getSkuNum(), wareOrderTaskDetailEntity.getId());
    }

    private void unLock(Long skuId, Long wareId, Integer num, Long detailId) {
        //update `wms_ware_sku` set stock_locked=stock_locked-num where sku_id = skuId and ware_id = wareId
        wareSkuDao.unLock(skuId, wareId, num);
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
        wareOrderTaskDetailEntity.setId(detailId);
        wareOrderTaskDetailEntity.setLockStatus(2);
        //更新库存工作单的状态
        orderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private int num;
        private List<Long> wareId;
    }
}