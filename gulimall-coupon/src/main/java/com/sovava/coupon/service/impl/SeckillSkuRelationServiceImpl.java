package com.sovava.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.coupon.dao.SeckillSkuRelationDao;
import com.sovava.coupon.entity.SeckillSkuRelationEntity;
import com.sovava.coupon.service.SeckillSkuRelationService;
import org.springframework.util.StringUtils;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> lqw = new LambdaQueryWrapper<>();
        String promotionSessionId = (String) params.get("promotionSessionId");
        if (!StringUtils.isEmpty(promotionSessionId)) {
            lqw.eq(SeckillSkuRelationEntity::getPromotionSessionId, promotionSessionId);

        }

        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                new QueryWrapper<SeckillSkuRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSkuRelationEntity> listBySessionId(Long sessionId) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SeckillSkuRelationEntity::getPromotionSessionId, sessionId);
        List<SeckillSkuRelationEntity> list = this.list(lqw);
        return list;
    }


}