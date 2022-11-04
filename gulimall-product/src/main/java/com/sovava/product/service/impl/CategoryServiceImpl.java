package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.service.CategoryBrandRelationService;
import com.sovava.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.CategoryDao;
import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("categoryService")
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        //装成树形结构
        // 1.找到所有一级分类
        List<CategoryEntity> firstCategory = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu -> {
                    menu.setChildren(getChildren(menu, categoryEntities));
                    return menu;
                })).sorted((item1, item2) -> {
                    return ((item1.getSort() == null ? 0 : item1.getSort()) -
                            (item2.getSort() == null ? 0 : item2.getSort()));
                }).collect(Collectors.toList());


        return firstCategory;
    }

    @Override
    public void removeMenusByIds(List<Long> ids) {

        //TODO:检查当前要删除的菜单是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();

        CategoryEntity categoryEntity = baseMapper.selectById(catelogId);
        Long secondPath = categoryEntity.getParentCid();
        CategoryEntity categoryEntity1 = baseMapper.selectById(secondPath);
        Long firstPath = categoryEntity1.getParentCid();
        path.add(firstPath);
        path.add(secondPath);
        path.add(catelogId);
        log.debug("查询的路径为{}", path.toString());
        return path.toArray(new Long[0]);
    }

    @Override
    @Transactional
    public void updateCascat(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> findLevel1Categories() {
        LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryEntity::getParentCid, 0);
        List<CategoryEntity> categoryEntities = this.list(lqw);
        return categoryEntities;

    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJSON() {
        //查出所有一级分类
        List<CategoryEntity> level1Categories = this.findLevel1Categories();
        //封装数据
        Map<String, List<Catelog2Vo>> parant_cid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个的一级分类 ， 查到这个以及分类的二级分类
            LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(CategoryEntity::getParentCid, v.getCatId());
            //当前一级id的二级分类
            List<CategoryEntity> categoryEntities = this.baseMapper.selectList(lqw);

            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {

                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //赵二级分类的三级分类
                    List<CategoryEntity> level3Cates = this.baseMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getParentCid, l2.getCatId()));
                    if (level3Cates != null) {
                        List<Catelog2Vo.Catalog3Vo> catalog3Vos = level3Cates.stream().map(l3 -> {
                            Catelog2Vo.Catalog3Vo catalog3Vo = new Catelog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return parant_cid;
    }

    private List<CategoryEntity> getChildren(CategoryEntity entity, List<CategoryEntity> all) {
        List<CategoryEntity> children = new ArrayList<>();
        children = all.stream().filter((item) -> {
            return item.getParentCid().equals(entity.getCatId());
        }).map(item -> {
            //找到子菜单
            item.setChildren(getChildren(item, all));
            return item;
        }).sorted((menu1, menu2) -> {
            return ((menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()));
        }).collect(Collectors.toList());
        return children;
    }

}