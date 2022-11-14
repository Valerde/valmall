package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.entity.SkuImagesEntity;
import com.sovava.product.entity.SpuInfoDescEntity;
import com.sovava.product.service.*;
import com.sovava.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.SkuInfoDao;
import com.sovava.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("skuInfoService")
@Slf4j
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Resource
    private SkuImagesService skuImagesService;

    @Resource
    private AttrGroupService attrGroupService;

    @Resource
    private SpuInfoDescService spuInfoDescService;

    @Resource
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * key: '华为',//检索关键字
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        LambdaQueryWrapper<SkuInfoEntity> lqw = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        lqw.and(!StringUtils.isEmpty(key), (w) -> {
            w.eq(SkuInfoEntity::getSkuId, key).or().like(SkuInfoEntity::getSkuName, key);
        });

        String catelogId = (String) params.get("catelogId");
        lqw.eq(!StringUtils.isEmpty(catelogId) && !catelogId.equalsIgnoreCase("0"), SkuInfoEntity::getCatalogId, catelogId);

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !brandId.equalsIgnoreCase("0")) {
            lqw.eq(SkuInfoEntity::getBrandId, brandId);
        }
        String min = (String) params.get("min");

        if (!StringUtils.isEmpty(min)) {
            lqw.ge(SkuInfoEntity::getPrice, min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max) && !max.equalsIgnoreCase("0")) {
            lqw.le(SkuInfoEntity::getPrice, max);
        }
        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> selectSkuBySpuId(Long spuId) {

        log.debug("传入的spuId为:{}", spuId);
        LambdaQueryWrapper<SkuInfoEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SkuInfoEntity::getSpuId, spuId);
        List<SkuInfoEntity> skus = this.list(lqw);
        log.debug("返回的skus信息为{}", skus.toString());
        return skus;
    }

    @Override
    public SkuItemVo itemInfo(Long skuId) {
        SkuItemVo skuItemVo = new SkuItemVo();
        //1. 获取sku的基本信息 pms_sku_info
        SkuInfoEntity info = this.getById(skuId);
        skuItemVo.setInfo(info);


        //2. 获取sku的图片信息 pms_sku_images
        List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
        skuItemVo.setImages(images);

        //3. 获取spu的所有销售属性
        List<SkuItemVo.SkuItemSaleAttrsVo> skuItemSaleAttrsVos = skuSaleAttrValueService.getSaleAttrsBySpuId(info.getSpuId());
        skuItemVo.setSaleAttr(skuItemSaleAttrsVos);

        //4. 获取spu的介绍
        Long spuId = info.getSpuId();
        SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
        skuItemVo.setDesc(spuInfoDesc);

        //5. 获取spu规格参数信息
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(info.getSpuId(), info.getCatalogId());
        skuItemVo.setGroupAttrs(attrGroupVos);
        return skuItemVo;
    }

}