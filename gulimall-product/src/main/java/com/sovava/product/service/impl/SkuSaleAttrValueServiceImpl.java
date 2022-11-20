package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.service.SkuInfoService;
import com.sovava.product.vo.Attr;
import com.sovava.product.vo.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.SkuSaleAttrValueDao;
import com.sovava.product.entity.SkuSaleAttrValueEntity;
import com.sovava.product.service.SkuSaleAttrValueService;

import javax.annotation.Resource;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemVo.SkuItemSaleAttrsVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemVo.SkuItemSaleAttrsVo> saleAttrsBySpuId = this.baseMapper.getSaleAttrsBySpuId(spuId);


        return saleAttrsBySpuId;
    }

    @Override
    public List<String> getSkuSaleAttrValue(Long skuId) {

        return this.baseMapper.getSkuSaleAttrValue(skuId);
    }

}