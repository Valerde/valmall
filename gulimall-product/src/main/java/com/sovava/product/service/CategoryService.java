package com.sovava.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();



    void removeMenusByIds(List<Long> ids);

    /**
     * 找到catelogid的完整路径 父路径,子路径 【2，25，225】
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    /**
     *
     * 级联更新关联表中的所有数据
     * @param category
     */
    void updateCascat(CategoryEntity category);
}

