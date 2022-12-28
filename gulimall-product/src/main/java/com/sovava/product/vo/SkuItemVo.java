package com.sovava.product.vo;

import com.sovava.product.entity.SkuImagesEntity;
import com.sovava.product.entity.SkuInfoEntity;
import com.sovava.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {
    //1. 获取sku的基本信息 pms_sku_info
    SkuInfoEntity info;
    //有货无货
    Boolean hasStock = true;
    //2. 获取sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    //3. 获取spu的所有销售属性
    List<SkuItemSaleAttrsVo> saleAttr;
    //4. 获取spu的介绍
    SpuInfoDescEntity desc;

    //5. 获取spu规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;
    //当前商品的秒杀信息
    SeckKillSkuRedisVo seckillInfo;

    @Data
    @ToString
    public static class SkuItemSaleAttrsVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @Data
    @ToString
    public static class SpuItemAttrGroupVo {
        private String groupName;
        private List<SpuBaseAttrVo> attrValues;
    }

    @Data
    @ToString
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }

    @Data
    public static class AttrValueWithSkuIdVo{
        private String attrValue;
        private String skuIds;
    }
}
