package com.sovava.product.dao;

import com.sovava.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {


    List<Long> selectSearchAttrIds(@Param("attrIds") List<Long> attrIds);
}
