package com.sovava.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.sovava.product.entity.AttrEntity;
import com.sovava.product.service.AttrAttrgroupRelationService;
import com.sovava.product.service.AttrService;
import com.sovava.product.service.CategoryService;
import com.sovava.product.vo.AttrGroupRelationVO;
import com.sovava.product.vo.AttrGroupWithAttrsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sovava.product.entity.AttrGroupEntity;
import com.sovava.product.service.AttrGroupService;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.R;

import javax.annotation.Resource;


/**
 * 属性分组
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@RestController
@RequestMapping("product/attrgroup")
@Slf4j
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Resource
    private AttrService attrService;

    @Resource
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    ///product/attrgroup/{catelogId}/withattr
    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttr(@PathVariable("catelogId") Long catelogId) {

        //1. 查处当前分类下的所有属性分组
        //2. 查出每个属性分组的所有属性
        //3.
        List<AttrGroupWithAttrsVo> attrGroupWithAttrsList = attrGroupService.getAttrGroupWithAttrByCatlog(catelogId);
        return R.ok().put("data",attrGroupWithAttrsList);
    }

    ///product/attrgroup/attr/relation
    @PostMapping("attr/relation")
    public R addrelation(@RequestBody List<AttrGroupRelationVO> relationVOS) {
        attrAttrgroupRelationService.saveBatchList(relationVOS);

        return R.ok();
    }


    // /product/attrgroup/{attrgroupId}/noattr/relation
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R getNoattrRelation(@PathVariable("attrgroupId") Long attrgroupId, @RequestParam Map<String, Object> params) {
        PageUtils pageUtils = attrService.getNoattrRelation(attrgroupId, params);

        return R.ok().put("page", pageUtils);
    }


    // /product/attrgroup/attr/relation/delete
    @PostMapping("/attr/relation/delete")
    public R deleteattrRelation(@RequestBody AttrGroupRelationVO[] attrGroupRelationVOs) {

        attrGroupService.deleteRelation(attrGroupRelationVOs);
        return R.ok();
    }

    ///product/attrgroup/{attrgroupId}/attr/relation
    @GetMapping("/{attrgroupId}/attr/relation")
    public R getAttrAttrgroupRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> attrEntities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", attrEntities);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);
        log.debug("查询信息为{}", Arrays.toString(page.getList().toArray()));
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long path[] = categoryService.findCatelogPath(attrGroup.getCatelogId());

        attrGroup.setCatelogPath(path);

        log.debug("查询到的信息为{}", attrGroup.toString());
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
