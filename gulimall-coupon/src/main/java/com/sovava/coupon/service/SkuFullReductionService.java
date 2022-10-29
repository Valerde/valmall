package com.sovava.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.to.SkuReductionTo;
import com.sovava.common.utils.PageUtils;
import com.sovava.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:24:16
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

