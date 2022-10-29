package com.sovava.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.product.entity.SpuInfoEntity;
import com.sovava.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo spuSaveVo);
}

