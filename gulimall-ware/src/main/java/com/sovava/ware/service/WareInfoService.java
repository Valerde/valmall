package com.sovava.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.ware.entity.WareInfoEntity;
import com.sovava.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:53:22
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * 根据用户的收货地址计算运费
     * @param addrId
     * @return
     */
    FareVo getFare(Integer addrId);
}

