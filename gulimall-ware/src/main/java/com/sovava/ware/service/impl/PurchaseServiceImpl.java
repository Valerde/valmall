package com.sovava.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.constant.ProductConstant;
import com.sovava.common.constant.WareConstant;
import com.sovava.ware.entity.PurchaseDetailEntity;
import com.sovava.ware.service.PurchaseDetailService;
import com.sovava.ware.service.WareSkuService;
import com.sovava.ware.vo.MergeVo;
import com.sovava.ware.vo.PurchaseDoneVo;
import com.sovava.ware.vo.PurchaseItemDoneVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.PurchaseDao;
import com.sovava.ware.entity.PurchaseEntity;
import com.sovava.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Resource
    private PurchaseDetailService purchaseDetailService;

    @Resource
    private WareSkuService wareSkuService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnReceiveList(Map<String, Object> params) {

        LambdaQueryWrapper<PurchaseEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(PurchaseEntity::getStatus, 0).or().eq(PurchaseEntity::getStatus, 1);
        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void mergePurchase(MergeVo vo) {
        Long purchaseId = vo.getPurchaseId();
        if (purchaseId == null) {
            //新建一个
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATE.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();

        }

        //TODO 确认采购但状态是0或者1才可以合并

        List<Long> items = vo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> detailEntities = items.stream().map((item) -> {
            PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
            purchaseDetail.setId(item);
            purchaseDetail.setPurchaseId(finalPurchaseId);
            purchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetail;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(detailEntities);


        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Override
    @Transactional
    public void received(List<Long> ids) {
        //确认当前采购单是新建或是一分配的状态
        List<PurchaseEntity> purchaseEntities = ids.stream().map((id) -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter((item) -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATE.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        //改变采购单的状态
        List<PurchaseEntity> receivedPurchases = purchaseEntities.stream().peek((item) -> {
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
        }).collect(Collectors.toList());
        this.updateBatchById(receivedPurchases);
        //改变采购项的状态

        receivedPurchases.forEach((item) -> {
            Long id = item.getId();
            List<PurchaseDetailEntity> list = purchaseDetailService.listDetailByPurchaseId(id);
            List<PurchaseDetailEntity> collect = list.stream().map((pd) -> {
                PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
                purchaseDetail.setId(pd.getId());
                purchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return purchaseDetail;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(collect);
        });

    }

    @Override
    @Transactional
    public void done(PurchaseDoneVo doneVo) {


        Long purchaseId = doneVo.getId();


        //2. 改变采购项状态
        boolean flag = false;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates =new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            Integer status = item.getStatus();PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
            if (status==WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = true;
                purchaseDetail.setStatus(item.getStatus());
            }else {

                purchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());

                //3. 采购成功 ， 将成功采购的入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                Long skuId = entity.getSkuId();
                Long wareId = entity.getWareId();
                Integer skuNum = entity.getSkuNum();
                wareSkuService.addStock(skuId,wareId,skuNum);

            }
            purchaseDetail.setId(item.getItemId());

            updates.add(purchaseDetail);
        }

        purchaseDetailService.updateBatchById(updates);

        //1. 改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }

}