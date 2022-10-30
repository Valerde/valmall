package com.sovava.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.PurchaseDetailDao;
import com.sovava.ware.entity.PurchaseDetailEntity;
import com.sovava.ware.service.PurchaseDetailService;
import org.springframework.util.StringUtils;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                new QueryWrapper<PurchaseDetailEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * key: '华为',//检索关键字
     * status: 0,//状态
     * wareId: 1,//仓库id
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        LambdaQueryWrapper<PurchaseDetailEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        lqw.and(!StringUtils.isEmpty(key), (w) -> {
            w.eq(PurchaseDetailEntity::getId, key).or().eq(PurchaseDetailEntity::getSkuNum, key);
        });

        String status = (String) params.get("status");
        lqw.eq(!StringUtils.isEmpty(status), PurchaseDetailEntity::getStatus, status);

        String wareId = (String) params.get("wareId");
        lqw.eq(!StringUtils.isEmpty(wareId), PurchaseDetailEntity::getWareId, wareId);

        IPage<PurchaseDetailEntity> page = this.page(new Query<PurchaseDetailEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    @Override
    public List<PurchaseDetailEntity> listDetailByPurchaseId(Long id) {

        LambdaQueryWrapper<PurchaseDetailEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(PurchaseDetailEntity::getPurchaseId,id);

        List<PurchaseDetailEntity> list = this.list(lqw);
        return list;
    }

}