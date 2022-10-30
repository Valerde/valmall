package com.sovava.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.utils.R;
import com.sovava.ware.feign.ProductFeignService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.WareSkuDao;
import com.sovava.ware.entity.WareSkuEntity;
import com.sovava.ware.service.WareSkuService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Resource
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * wareId: 123,//仓库id
     * skuId: 123//商品id
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();

        String wareId = (String) params.get("wareId");
        lqw.eq(!StringUtils.isEmpty(wareId), WareSkuEntity::getWareId, wareId);

        String skuId = (String) params.get("skuId");
        lqw.eq(!StringUtils.isEmpty(skuId), WareSkuEntity::getSkuId, skuId);

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    /**
     * 采购成功后向仓库中添加
     *
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果还没有这个库存记录
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WareSkuEntity::getWareId, wareId);
        lqw.eq(WareSkuEntity::getSkuId, skuId);

        List<WareSkuEntity> list = this.list(lqw);
        if (list == null || list.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStockLocked(0);
            String skuName = "";
            //远程查询sku的name ， 如果失败时， 不回滚
            R info = productFeignService.info(skuId);
            try {
                if (info.getCode() == 0) {
                    Map<String, Object> map = (Map<String, Object>) info.get("skuInfo");
                    skuName = (String) map.get("skuName");
                    wareSkuEntity.setSkuName(skuName);
                }
            } catch (Exception ex) {

            }

            this.save(wareSkuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

}