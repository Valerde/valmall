package com.sovava.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.entity.BrandEntity;
import com.sovava.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
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
}
