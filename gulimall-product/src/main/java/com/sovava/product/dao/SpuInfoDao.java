package com.sovava.product.dao;

import com.sovava.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * spu信息
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updateSkuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
