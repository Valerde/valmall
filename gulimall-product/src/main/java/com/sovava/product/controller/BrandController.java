package com.sovava.product.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.injector.methods.Update;
import com.sovava.common.valid.AddGroup;
import com.sovava.common.valid.UpdateGroup;
import com.sovava.common.valid.UpdateStatus;
import com.sovava.product.feign.CouponFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sovava.product.entity.BrandEntity;
import com.sovava.product.service.BrandService;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.R;

import javax.validation.Valid;


/**
 * 品牌
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@RestController
@RequestMapping("product/brand")
@RefreshScope//刷新配置
public class BrandController {
    @Autowired
    private BrandService brandService;

    @Value("${test.prod.user}")
    private String username;
    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/test")
    public R testNacos() {

        R list = couponFeignService.list(new HashMap<>());
        return R.ok().put("user", username).put("listfeing", list);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId) {
        BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated(value = AddGroup.class) @RequestBody BrandEntity brand/*, BindingResult result*/) {
//        if (result.hasErrors()) {
//            Map<String, String> map = new HashMap<>();
//            result.getFieldErrors().forEach((item) -> {
//                //FieldError 获取到的错误提示
//                String defaultMessage = item.getDefaultMessage();
//                String field = item.getField();
//                map.put(field, defaultMessage);
//            });
//
//            return R.error(400, "提交的数据不合法").put("data", map);
//        }
        brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated(value = UpdateGroup.class) @RequestBody BrandEntity brand) {
        brandService.updateDetail(brand);

        return R.ok();
    }

    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(UpdateStatus.class) @RequestBody BrandEntity brand) {
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
