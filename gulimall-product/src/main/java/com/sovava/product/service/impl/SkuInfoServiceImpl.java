package com.sovava.product.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.utils.R;
import com.sovava.product.entity.SkuImagesEntity;
import com.sovava.product.entity.SpuInfoDescEntity;
import com.sovava.product.feign.SeckillFeignService;
import com.sovava.product.service.*;
import com.sovava.product.vo.SeckKillSkuRedisVo;
import com.sovava.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

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

    @Resource
    private ThreadPoolExecutor executor;

    @Resource
    private SeckillFeignService seckillFeignService;

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

        //异步编排优化
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1. 获取sku的基本信息 pms_sku_info
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //3. 获取spu的所有销售属性
            List<SkuItemVo.SkuItemSaleAttrsVo> skuItemSaleAttrsVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(skuItemSaleAttrsVos);
        }, executor);

        CompletableFuture<Void> spuDescFuture = infoFuture.thenAcceptAsync((res) -> {
            //4. 获取spu的介绍
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //5. 获取spu规格参数信息
            List<SkuItemVo.SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            //2. 获取sku的图片信息 pms_sku_images
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);

        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            //获取sku基本信息
            R r = seckillFeignService.getskuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckKillSkuRedisVo killSkuRedisVo = r.getData(new TypeReference<SeckKillSkuRedisVo>() {
                });

                log.debug("秒杀信息为：{}", killSkuRedisVo.toString());
                skuItemVo.setSeckillInfo(killSkuRedisVo);
            }
        }, executor);


        //等待所有线程执行完成
        CompletableFuture<Void> all = CompletableFuture.allOf(infoFuture, saleAttrFuture, baseAttrFuture, spuDescFuture, imageFuture, seckillFuture);
        try {
            all.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return skuItemVo;

    }
}