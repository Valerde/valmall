package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.ProductAttrValueDao;
import com.sovava.product.entity.ProductAttrValueEntity;
import com.sovava.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveProductAttr(List<ProductAttrValueEntity> productAttrValueEntities) {
        saveBatch(productAttrValueEntities);
    }

    @Override
    public List<ProductAttrValueEntity> baseListforspuSpuId(Long spuId) {
        LambdaQueryWrapper<ProductAttrValueEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ProductAttrValueEntity::getSpuId, spuId);
        List<ProductAttrValueEntity> list = this.list(lqw);
        return list;
    }

    @Override
    @Transactional
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> list) {
        //1. 删除spuId对应的Attr属性
        LambdaQueryWrapper<ProductAttrValueEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ProductAttrValueEntity::getSpuId, spuId);
        this.baseMapper.delete(lqw);

        List<ProductAttrValueEntity> collect = list.stream().map((item) -> {
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }

}