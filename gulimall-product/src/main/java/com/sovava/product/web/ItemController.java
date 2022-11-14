package com.sovava.product.web;

import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.SkuInfoService;
import com.sovava.product.vo.SkuItemVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.List;

@Controller
public class ItemController {
    @Resource
    private SkuInfoService skuInfoService;

    /**
     * @return
     */
    @GetMapping({"/{skuId}.html"})
    public String indexPage(@PathVariable("skuId") Long skuId, Model model) {
        SkuItemVo skuItemVo = skuInfoService.itemInfo(skuId);
        model.addAttribute("item", skuItemVo);
        return "item";
    }
}
