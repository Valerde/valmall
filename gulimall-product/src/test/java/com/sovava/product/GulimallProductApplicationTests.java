package com.sovava.product;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.entity.BrandEntity;
import com.sovava.product.entity.SkuSaleAttrValueEntity;
import com.sovava.product.service.AttrGroupService;
import com.sovava.product.service.BrandService;
import com.sovava.product.service.SkuSaleAttrValueService;
import com.sovava.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
@Slf4j
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Test
    public void testService() {

        BrandEntity brand = new BrandEntity();

        brand.setName("dauabi");
        brand.setLogo("adfgd");
//        for (int i = 0; i < 10; i++)
        brandService.save(brand);
        System.out.println("保存成功");
    }

    @Test
    public void testBrandUpdate() {

        BrandEntity brand = new BrandEntity();

        brand.setName("dauabi");
        brand.setBrandId(1L);
        brand.setDescript("afbdfb");
        brandService.updateById(brand);
    }

    @Test
    public void testBrandQuery() {

        LambdaQueryWrapper<BrandEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(BrandEntity::getName, "dauabi");
        List<BrandEntity> list = brandService.list(lqw);
        System.out.println(list);


    }

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void testRedissonClient() {
        System.out.println(redissonClient);
    }





    @Autowired
    private AttrGroupService attrGroupService;
    @Test
    public void testGetAttrGroupWithAttrsBySpuId(){
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupService.getAttrGroupWithAttrsBySpuId(7L, 225L);
//        for (SkuItemVo.SpuItemAttrGroupVo spuItemAttrGroupVo : attrGroupWithAttrsBySpuId) {
//            log.debug(spuItemAttrGroupVo);
//        }
        log.debug("{}",attrGroupWithAttrsBySpuId);
    }

    @Resource
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Test
    public void testGetSaleAttrsBySpuId(){
        List<SkuItemVo.SkuItemSaleAttrsVo> vos = skuSaleAttrValueService.getSaleAttrsBySpuId(7L);
        System.out.println(vos);
    }
}
