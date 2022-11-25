package com.sovava.ware.dao;

import com.sovava.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:53:23
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId,@Param("wareId") Long wareId,@Param("skuNum") Integer skuNum);

    Long getSkuIdStock(@Param("item") Long item);

    List<Long> listWareIdHasStock(@Param("skuId") Long skuId);

    int lockSkuIdStock(@Param("num")int num,@Param("skuId") Long skuId,@Param("wareId") Long wareId);
}
