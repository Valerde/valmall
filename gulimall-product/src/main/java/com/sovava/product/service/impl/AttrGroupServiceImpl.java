package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.AttrGroupDao;
import com.sovava.product.entity.AttrGroupEntity;
import com.sovava.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
@Slf4j
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

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

        IPage<AttrGroupEntity> page = new Query<AttrGroupEntity>().getPage(params);
        if (catelogId == 0) {

            LambdaQueryWrapper<AttrGroupEntity> lqw = new LambdaQueryWrapper<>();
            IPage<AttrGroupEntity> page1 = this.page(page, lqw);
            return new PageUtils(page1);
        }
        String key = (String) params.get("key");

//       select * from pms_attr_group where catelog_id = ? and(attr_catelog_id = key or attr_group_name like %name%);

        LambdaQueryWrapper<AttrGroupEntity> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(AttrGroupEntity::getCatelogId, catelogId);
        if (!StringUtils.isEmpty(key)) {
            log.debug("key{}",key);
            lqw2.and((obj) -> {
                obj.eq(AttrGroupEntity::getAttrGroupId, key).or().like(AttrGroupEntity::getAttrGroupName, key);
            });
        }


        IPage<AttrGroupEntity> page1 = this.page(page, lqw2);

        return new PageUtils(page1);

    }

}