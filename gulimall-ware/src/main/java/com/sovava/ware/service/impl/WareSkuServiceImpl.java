package com.sovava.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.utils.R;
import com.sovava.ware.exception.NoStockException;
import com.sovava.ware.feign.ProductFeignService;
import com.sovava.ware.vo.LockStockVo;
import com.sovava.ware.vo.OrderItemVo;
import com.sovava.ware.vo.SkuHasStockVo;
import com.sovava.ware.vo.WareSkuLockVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLock(WareSkuLockVo vo) throws NoStockException {
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
            for (Long wareId : wareIds) {
                int i = this.baseMapper.lockSkuIdStock(hasStock.getNum(), skuId, wareId);
                if (i == 1) {
                    skuLocked = true;
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

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private int num;
        private List<Long> wareId;
    }
}