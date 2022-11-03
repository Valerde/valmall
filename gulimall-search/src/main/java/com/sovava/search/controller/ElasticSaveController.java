package com.sovava.search.controller;

import com.sovava.common.exception.BizCodeEnum;
import com.sovava.common.to.es.SpuEsModel;
import com.sovava.common.utils.R;
import com.sovava.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/search/save")
@Slf4j
public class ElasticSaveController {

    @Resource
    private ProductSaveService productSaveService;


    //上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SpuEsModel> spuEsModels) {
        Boolean b = true;
        try {
            b = productSaveService.productStatusUp(spuEsModels);
        } catch (Exception e) {
            log.error("ElasticSaveController商品上架错误{}", e.getMessage());
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMessage());
        }
        if (b) {
            return R.ok();
        } else {
            R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMessage());
        }
        return R.ok();
    }
}
