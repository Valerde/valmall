package com.sovava.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.WareOrderTaskDetailDao;
import com.sovava.ware.entity.WareOrderTaskDetailEntity;
import com.sovava.ware.service.WareOrderTaskDetailService;


@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareOrderTaskDetailEntity> page = this.page(
                new Query<WareOrderTaskDetailEntity>().getPage(params),
                new QueryWrapper<WareOrderTaskDetailEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<WareOrderTaskDetailEntity> listByTaskId(Long taskId) {

        LambdaQueryWrapper<WareOrderTaskDetailEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WareOrderTaskDetailEntity::getTaskId, taskId);
        lqw.eq(WareOrderTaskDetailEntity::getLockStatus, 1);
        List<WareOrderTaskDetailEntity> list = this.list(lqw);
        return list;
    }

}