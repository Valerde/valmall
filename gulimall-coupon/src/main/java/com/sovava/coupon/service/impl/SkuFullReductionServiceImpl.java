package com.sovava.coupon.service.impl;

import com.sovava.common.to.MemberPrice;
import com.sovava.common.to.SkuReductionTo;
import com.sovava.coupon.entity.MemberPriceEntity;
import com.sovava.coupon.entity.SkuLadderEntity;
import com.sovava.coupon.service.MemberPriceService;
import com.sovava.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.coupon.dao.SkuFullReductionDao;
import com.sovava.coupon.entity.SkuFullReductionEntity;
import com.sovava.coupon.service.SkuFullReductionService;
import org.springframework.util.StringUtils;
import rx.internal.operators.BackpressureUtils;

import javax.annotation.Resource;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Resource
    private SkuLadderService skuLadderService;

    @Resource
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 商品保存
     * TODO： 高级部分优化
     * @param skuReductionTo
     */
    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        //1. 保存满减打折
        //sku的优惠满减信息 gulimall_sms -> sms_sku_ladder /
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        skuLadderEntity.setPrice(skuReductionTo.getFullPrice());
        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        //sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();

        BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuReductionTo.getPriceStatus());
        if (skuFullReductionEntity.getFullPrice().compareTo(new BigDecimal(0)) == 1) {
            this.save(skuFullReductionEntity);
        }

        //sms_member_price
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map((item -> {
            MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
            memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
            memberPriceEntity.setMemberPrice(item.getPrice());
            memberPriceEntity.setMemberLevelId(item.getId());
            memberPriceEntity.setMemberLevelName(item.getName());
            memberPriceEntity.setAddOther(1);
            return memberPriceEntity;
        })).filter((entity -> {
            return entity.getMemberPrice().compareTo(new BigDecimal(0)) == 1;
        })).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);
    }

}