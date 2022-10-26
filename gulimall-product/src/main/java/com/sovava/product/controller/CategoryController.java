package com.sovava.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.R;



/**
 * 商品三级分类
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 查处所有分类和子分类 ， 并以树形结构返回列表
     */
    @RequestMapping("/list/tree")
    public R list(){
        List<CategoryEntity> entities = categoryService.listWithTree();

        return R.ok().put("data", entities);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    //@RequiresPermissions("product:category:info")
    public R info(@PathVariable("catId") Long catId){
		CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("category", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:category:save")
    public R save(@RequestBody CategoryEntity category){
		categoryService.save(category);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update/sort")
    //@RequiresPermissions("product:category:update")
    public R update(@RequestBody CategoryEntity[] category){
		categoryService.updateBatchById(Arrays.asList(category));

        return R.ok();
    }

    /**
     * 删除
     * RequestBody 获取请求体必须发送body请求
     * springmvc自动将json数据转为对应的对象
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:category:delete")
    public R delete(@RequestBody Long[] catIds){

        //1 检查当前要删除的菜单是都被别的地方引用
//		categoryService.removeByIds(Arrays.asList(catIds));
        categoryService.removeMenusByIds(Arrays.asList(catIds));
        return R.ok();
    }

}
