package com.sovava.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.ware.entity.PurchaseEntity;
import com.sovava.ware.vo.MergeVo;
import com.sovava.ware.vo.PurchaseDoneVo;
import com.sovava.ware.vo.PurchaseItemDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:53:22
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnReceiveList(Map<String, Object> params);

    void mergePurchase(MergeVo vo);

    void received(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

