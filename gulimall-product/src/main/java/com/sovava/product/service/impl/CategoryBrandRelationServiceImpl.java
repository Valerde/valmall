package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.dao.BrandDao;
import com.sovava.product.dao.CategoryDao;
import com.sovava.product.entity.BrandEntity;
import com.sovava.product.service.BrandService;
import com.sovava.product.service.CategoryService;
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

import com.sovava.product.dao.CategoryBrandRelationDao;
import com.sovava.product.entity.CategoryBrandRelationEntity;
import com.sovava.product.service.CategoryBrandRelationService;

import javax.annotation.Resource;


@Service("categoryBrandRelationService")
@Slf4j
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {


    @Resource
    private BrandDao brandDao;
    @Resource
    private CategoryDao categoryDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {

        String brandName = brandDao.selectById(categoryBrandRelation.getBrandId()).getName();
        String catelogName = categoryDao.selectById(categoryBrandRelation.getCatelogId()).getName();
        categoryBrandRelation.setBrandName(brandName);
        categoryBrandRelation.setCatelogName(catelogName);
        this.save(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity categoryBrandRelation = new CategoryBrandRelationEntity();
        categoryBrandRelation.setBrandId(brandId);
        categoryBrandRelation.setBrandName(name);

        LambdaQueryWrapper<CategoryBrandRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryBrandRelationEntity::getBrandId,brandId);
        this.update(categoryBrandRelation,lqw);
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCagory(catId,name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        LambdaQueryWrapper<CategoryBrandRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryBrandRelationEntity::getCatelogId,catId);
        List<CategoryBrandRelationEntity> relationList = this.baseMapper.selectList(lqw);
        List<Long> brandList = relationList.stream().map(CategoryBrandRelationEntity::getBrandId).collect(Collectors.toList());

        List<BrandEntity> brandEntities = brandDao.selectBatchIds(brandList);
        return brandEntities;
    }

}