package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.dao.AttrAttrgroupRelationDao;
import com.sovava.product.entity.AttrAttrgroupRelationEntity;
import com.sovava.product.vo.AttrGroupRelationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.AttrGroupDao;
import com.sovava.product.entity.AttrGroupEntity;
import com.sovava.product.service.AttrGroupService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrGroupService")
@Slf4j
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Resource
    private AttrAttrgroupRelationDao relationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        IPage<AttrGroupEntity> page = new Query<AttrGroupEntity>().getPage(params);
        LambdaQueryWrapper<AttrGroupEntity> lqw = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(key)) {
            log.debug("key{}", key);
            lqw.and((obj) -> {
                obj.eq(AttrGroupEntity::getAttrGroupId, key).or().like(AttrGroupEntity::getAttrGroupName, key);
            });
        }
        if (catelogId == 0) {


            IPage<AttrGroupEntity> page1 = this.page(page, lqw);
            return new PageUtils(page1);
        }

//       select * from pms_attr_group where catelog_id = ? and(attr_catelog_id = key or attr_group_name like %name%);

        lqw.eq(AttrGroupEntity::getCatelogId, catelogId);
        IPage<AttrGroupEntity> page1 = this.page(page, lqw);

        return new PageUtils(page1);

    }

    @Override
    public void deleteRelation(AttrGroupRelationVO[] attrGroupRelationVOs) {
        /*
         * delete from `pms_attr_attrgroup_relation` where (attr_id = 1 and attr_group_id = 1) and (attr_id = ？ and attr_group_id = ?);
         **/

        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(attrGroupRelationVOs).stream().map((item) -> {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item,relation);
            return relation;
        }).collect(Collectors.toList());

        relationDao.deleteBatchRelation(entities);

    }

}