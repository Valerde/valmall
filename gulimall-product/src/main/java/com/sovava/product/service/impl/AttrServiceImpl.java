package com.sovava.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.constant.ProductConstant;
import com.sovava.product.dao.AttrGroupDao;
import com.sovava.product.dao.CategoryDao;
import com.sovava.product.entity.AttrAttrgroupRelationEntity;
import com.sovava.product.entity.AttrGroupEntity;
import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.AttrAttrgroupRelationService;
import com.sovava.product.service.CategoryService;
import com.sovava.product.vo.AttrRespVO;
import com.sovava.product.vo.AttrVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.AttrDao;
import com.sovava.product.entity.AttrEntity;
import com.sovava.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrService")
@Slf4j
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {


    @Resource
    private AttrGroupDao attrGroupDao;

    @Resource
    private CategoryDao categoryDao;
    @Resource
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Resource
    private CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveAttr(AttrVo attr) {
        log.debug("传入的属性信息为：{}", attr.toString());
        //保存基本数据
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.save(attrEntity);

        //保存关联关系
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrGroupId(attr.getAttrGroupId());
            log.debug("attrId{}", attrEntity.getAttrId());
            relation.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationService.save(relation);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        LambdaQueryWrapper<AttrEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");

        lqw.eq(AttrEntity::getAttrType
                , "base".equalsIgnoreCase(type) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                        : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        lqw.eq(catelogId != 0, AttrEntity::getCatelogId, catelogId);

        lqw.and(!StringUtils.isEmpty(key), (obj) -> {
            obj.eq(AttrEntity::getAttrId, key).or().like(AttrEntity::getAttrName, key);
        });


        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), lqw);
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVO> attrRespVOList = records.stream().map(item -> {
            AttrRespVO attrRespVO = new AttrRespVO();
            BeanUtils.copyProperties(item, attrRespVO);


            Long attrId = item.getAttrId();
            AttrAttrgroupRelationEntity one = attrAttrgroupRelationService.getOne(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId, attrId));
            if (one != null && one.getAttrGroupId() != null) {
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(one.getAttrGroupId());
                attrRespVO.setGroupName(attrGroupEntity.getAttrGroupName());
            }
            CategoryEntity categoryEntity = categoryDao.selectById(item.getCatelogId());
            if (categoryEntity != null) {
                attrRespVO.setCatelogName(categoryEntity.getName());
            }

            return attrRespVO;
        }).collect(Collectors.toList());
        pageUtils.setList(attrRespVOList);
        return pageUtils;

    }

    @Override
    @Cacheable(value = "attr",key = "'attrinfo'+#root.args[0]")
    public AttrRespVO getAttrInfo(Long attrId) {
        AttrRespVO attrRespVO = new AttrRespVO();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVO);

        //1. 设置分组 , 先进行判断 ， 如果是销售属性就没有分组信息了
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(AttrAttrgroupRelationEntity::getAttrId, attrId);

            AttrAttrgroupRelationEntity one = attrAttrgroupRelationService.getOne(lqw);
            if (one != null) {
                attrRespVO.setAttrGroupId(one.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(one.getAttrGroupId());
                if (attrGroupEntity != null)
                    attrRespVO.setGroupName(attrGroupEntity.getAttrGroupName());
            }
        }

        //2. 设置分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        attrRespVO.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (null != categoryEntity)
            attrRespVO.setCatelogName(categoryEntity.getName());


        return attrRespVO;
    }

    @Override
    @Transactional
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            //修改分组关联
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrGroupId(attr.getAttrGroupId());
            relation.setAttrId(attr.getAttrId());
            LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId());
            long count = attrAttrgroupRelationService.count(lqw);
            //log.debug("{}",one.toString());
            if (count == 0) {
                log.debug("本来就没有分组信息 ， 那么其实是一个新增操作");
                //如果本来就没有分组信息 ， 那么其实是一个新增操作
                attrAttrgroupRelationService.save(relation);
            } else {
                attrAttrgroupRelationService.update(relation, lqw);
            }
        }

    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AttrAttrgroupRelationEntity::getAttrGroupId, attrgroupId);
        List<AttrAttrgroupRelationEntity> list = attrAttrgroupRelationService.list(lqw);
        List<Long> attrIds = list.stream().map((AttrAttrgroupRelationEntity::getAttrId)).collect(Collectors.toList());

        if (attrIds.size() == 0 || attrIds == null) {
            return null;
        }
        List<AttrEntity> attrEntities = this.listByIds(attrIds);
        return attrEntities;
    }

    @Override
    public PageUtils getNoattrRelation(Long attrgroupId, Map<String, Object> params) {
        // 1. 当前分组只能关联自己所属的分类里的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        // 2. 当前分组只能关联别的分组没有引用的属性
        // 2.1 当前分类下的其他分组
        LambdaQueryWrapper<AttrGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AttrGroupEntity::getCatelogId, catelogId);
        lqw.ne(AttrGroupEntity::getAttrGroupId, attrgroupId);
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(lqw);

        // 2.2 这些分组关联的属性
        List<Long> groupIds = attrGroupEntities.stream().map((AttrGroupEntity::getAttrGroupId)).collect(Collectors.toList());
        log.debug("groupIds{}", groupIds);
        LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw3 = new LambdaQueryWrapper<>();
        if (groupIds.size() != 0) {
            lqw3.notIn(AttrAttrgroupRelationEntity::getAttrGroupId, groupIds);
        }
        List<AttrAttrgroupRelationEntity> list = attrAttrgroupRelationService.list(lqw3);


        List<Long> attrIds = list.stream().map((AttrAttrgroupRelationEntity::getAttrId)).collect(Collectors.toList());
        log.debug("attrIds" + attrIds.toString());

        // 2.3 从当前分类的所有属性中移除这些属性
        LambdaQueryWrapper<AttrEntity> lqw2 = new LambdaQueryWrapper<AttrEntity>();
        lqw2.eq(AttrEntity::getCatelogId, catelogId);
        if (attrIds.size() != 0) lqw2.notIn(AttrEntity::getAttrId, attrIds);
        lqw2.eq(AttrEntity::getAttrType, ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        String key = (String) params.get("key");

        lqw2.and(!StringUtils.isEmpty(key), (w) -> {
            w.like(AttrEntity::getAttrId, key).or().like(AttrEntity::getAttrName, key);
        });


        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), lqw2);

        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {

//        select * from `pms_attr` where attr_id in attrIds and search_type = 1
        List<Long> attrEntities = this.baseMapper.selectSearchAttrIds(attrIds);


        return attrEntities;
    }

}