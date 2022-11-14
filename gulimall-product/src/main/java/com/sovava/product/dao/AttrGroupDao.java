package com.sovava.product.dao;

import com.sovava.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sovava.product.vo.SkuItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {



    List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(@Param("spuId") Long spuId,@Param("catalogId") Long catalogId);
}
