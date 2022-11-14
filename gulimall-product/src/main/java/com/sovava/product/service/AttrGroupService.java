package com.sovava.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.product.entity.AttrGroupEntity;
import com.sovava.product.vo.AttrGroupRelationVO;
import com.sovava.product.vo.AttrGroupWithAttrsVo;
import com.sovava.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    /**
     * 删除attr和attrgroup的关联关系
     * @param attrGroupRelationVOs
     */
    void deleteRelation(AttrGroupRelationVO[] attrGroupRelationVOs);

    /**
     * 根据分类id查出所有的分组以及分组属性
     * @param catelogId
     * @return
     */
    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrByCatlog(Long catelogId);

    List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

