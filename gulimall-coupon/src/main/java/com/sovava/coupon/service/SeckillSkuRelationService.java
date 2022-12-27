package com.sovava.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.coupon.entity.SeckillSkuRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动商品关联
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:24:16
 */
public interface SeckillSkuRelationService extends IService<SeckillSkuRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);


    List<SeckillSkuRelationEntity> listBySessionId(Long sessionId);
}

