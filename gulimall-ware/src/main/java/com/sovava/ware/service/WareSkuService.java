package com.sovava.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.to.OrderTo;
import com.sovava.common.to.StockLockedTo;
import com.sovava.common.utils.PageUtils;
import com.sovava.ware.entity.WareSkuEntity;
import com.sovava.ware.exception.NoStockException;
import com.sovava.ware.vo.LockStockVo;
import com.sovava.ware.vo.SkuHasStockVo;
import com.sovava.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:53:23
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStockBySkuIds(List<Long> skuIds);

    Boolean orderLock(WareSkuLockVo vo) throws NoStockException;

    void unlockStock(StockLockedTo stockLockedTo);

    void unlockStock(String orderSn);
}

