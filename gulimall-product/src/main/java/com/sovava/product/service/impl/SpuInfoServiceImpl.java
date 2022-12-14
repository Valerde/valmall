package com.sovava.product.service.impl;

//import com.alibaba.fastjson.TypeReference;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.constant.ProductConstant;
import com.sovava.common.to.SkuReductionTo;
import com.sovava.common.to.SpuBoundTo;
import com.sovava.common.to.es.SpuEsModel;
import com.sovava.common.utils.R;
import com.sovava.product.entity.*;
import com.sovava.product.feign.CouponFeignService;
import com.sovava.product.feign.SearchFeignService;
import com.sovava.product.feign.WareFeignService;
import com.sovava.product.service.*;
import com.sovava.product.vo.*;
import com.sun.org.glassfish.external.statistics.annotations.Reset;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Resource
    private SpuInfoDescService spuInfoDescService;

    @Resource
    private AttrService attrService;

    @Resource
    private SpuImagesService spuImagesService;

    @Resource
    private ProductAttrValueService productAttrValueService;

    @Resource
    private SkuInfoService skuInfoService;

    @Resource
    private SkuImagesService skuImagesService;

    @Resource
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Resource
    private CouponFeignService couponFeignService;

    @Resource
    private BrandService brandService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private WareFeignService wareFeignService;

    @Resource
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuSaveVo spuSaveVo) {
        //1. ??????spu???????????? pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.save(spuInfoEntity);
        //2. ??????spu??????????????? pms_spu_info_desc
        List<String> decript = spuSaveVo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(";", decript));

        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);
        //3. ??????spu???????????? pms_spu_images
        List<String> images = spuSaveVo.getImages();

        spuImagesService.saveImages(spuInfoEntity.getId(), images);
        //4. ???????????????????????????pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map((attr) -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setSpuId(spuInfoEntity.getId());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setAttrId(attr.getAttrId());
            valueEntity.setQuickShow(attr.getShowDesc());
            AttrEntity tempAttr = attrService.getById(attr.getAttrId());
            if (tempAttr != null)
                valueEntity.setAttrName(tempAttr.getAttrName());
            return valueEntity;
        }).collect(Collectors.toList());

        productAttrValueService.saveProductAttr(productAttrValueEntities);

        //4.5 ??????spu??????????????? sms_spu_bounds
        Bounds bounds = spuSaveVo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        int code = r.getCode();
        if (code != 0) {
            log.error("??????spu???????????????????????????");
        }

        //5. ????????????spu????????????sku??????
        //5.1 ??????sku??????????????? pms_sku_info

        List<Skus> skus = spuSaveVo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach((item) -> {
                String defaultImage = null;

                for (Images img : item.getImages()) {
                    if (img.getDefaultImg() == 1) {
                        defaultImage = img.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuSaveVo.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImage);

                skuInfoService.save(skuInfoEntity);
                //5.2 ??????sku???????????????pms_sku_images
                Long skuId = skuInfoEntity.getSkuId();


                List<SkuImagesEntity> skuImagesEntities = item.getImages().stream().map(images1 -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setDefaultImg(images1.getDefaultImg());
                    skuImagesEntity.setImgUrl(images1.getImgUrl());

                    return skuImagesEntity;
                }).filter((entity) -> {
                    //??????true?????? ??? false???????????????
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());


                skuImagesService.saveBatch(skuImagesEntities);

                //5.3 sku?????????????????????pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(attr1 -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr1, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);


                //5.4 sku????????????????????? gulimall_sms -> sms_sku_ladder / sms_sku_full_reduction / sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getDiscount().compareTo(new BigDecimal(0)) == 1 || skuReductionTo.getFullCount() > 0) {
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("??????????????????????????????");
                    }
                }

            });
        }


    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SpuInfoEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            lqw.and((w) -> {//?????????????????????and?????????sql??????????????????????????????status=1 and id=? or spu_name=???
                //          ?????????and??? ??? ????????????status=1 and ???id=? or spu_name=??????
                w.eq(SpuInfoEntity::getId, key).or().like(SpuInfoEntity::getSpuName, key);
            });
        }
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            lqw.eq(SpuInfoEntity::getPublishStatus, status);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !brandId.equalsIgnoreCase("0")) {
            lqw.eq(SpuInfoEntity::getBrandId, brandId);
        }

        String catelogId = (String) params.get("catelog");
        if (!StringUtils.isEmpty(catelogId)) {
            lqw.eq(SpuInfoEntity::getCatalogId, catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), lqw);

        return new PageUtils(page);

    }

    @Override
    public void up(Long spuId) {
        List<SpuEsModel> upProducts;


        // 1. ????????????spu?????????sku??????
        List<SkuInfoEntity> skus = skuInfoService.selectSkuBySpuId(spuId);

        //TO DO : ????????????spu????????????????????????????????????????????? ??? ??????????????????????????????????????????
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseListforspuSpuId(spuId);
        List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<Long> searchAttrs = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrs);
        List<SpuEsModel.Attrs> attrs1 = baseAttrs.stream().filter((item) -> {
            return idSet.contains(item.getAttrId());
        }).map((item) -> {
            SpuEsModel.Attrs attrs = new SpuEsModel.Attrs();
            attrs.setAttrId(item.getAttrId());
            attrs.setAttrValue(item.getAttrValue());
            attrs.setAttrName(item.getAttrName());
            return attrs;
        }).collect(Collectors.toList());


        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

//        TO DO??? ?????????????????? ??? ????????????????????? , ????????????????????????
        Map<Long, Boolean> collect = null;
        try {
            R r = wareFeignService.selectHasStock(skuIds);
            Object data = r.get("data");
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
            };
            collect = r.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        } catch (Exception ex) {
            log.debug("???????????????????????? , ??????{}", ex.getMessage());
            ex.printStackTrace();
        }

        //2. ????????????sku?????????
        Map<Long, Boolean> finalCollect = collect;
        upProducts = skus.stream().map((sku) -> {
            //?????????????????????
            SpuEsModel spuEsModel = new SpuEsModel();
            BeanUtils.copyProperties(sku, spuEsModel);
            //skuImage
            spuEsModel.setSkuImg(sku.getSkuDefaultImg());
            //price
            spuEsModel.setSkuPrice(sku.getPrice());
            //hasStock TO DO??? ?????????????????? ??? ????????????????????? , ????????????????????????
            if (finalCollect == null) {
                spuEsModel.setHasStock(true);
            } else {
                spuEsModel.setHasStock(finalCollect.get(sku.getSkuId()));
            }
            //hotScore TODO ??? ?????????????????????????????????????????????????????? ??? ??????????????????????????? ?????????????????????0
            spuEsModel.setHotScore(0L);
            //brandName
            BrandEntity brand = brandService.getById(spuEsModel.getBrandId());
            if (brand != null) {
                spuEsModel.setBrandName(brand.getName());
                //brandImg
                spuEsModel.setBrandImg(brand.getLogo());
            }

            //catalogName
            CategoryEntity category = categoryService.getById(spuEsModel.getCatalogId());
            if (category != null) {
                spuEsModel.setCatalogName(category.getName());
            }

            // ??????????????????
            spuEsModel.setAttrs(attrs1);

            return spuEsModel;
        }).collect(Collectors.toList());

        //TODO ??????????????????es???????????? ??? ??????gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0) {
            log.error("??????????????????");
            //TODO ????????????spu?????????????????????
            this.baseMapper.updateSkuStatus(spuId, ProductConstant.StatusEum.SPU_UP.getCode());
        } else {
            //??????????????????
            //TODO ?????????????????? ??? ???????????????
            //Feign???????????????
            /**
             * 1. ???????????????????????? ???????????????json
             * 2. ???????????????????????????????????????????????????????????????
             * 3. ??????????????????????????????
             */
        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        Long spuId = skuInfo.getSpuId();
        SpuInfoEntity spuInfo = this.getById(spuId);
        return spuInfo;

    }

}