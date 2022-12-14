package com.sovava.ware.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.ware.feign.MemberFeignService;
import com.sovava.ware.vo.FareVo;
import com.sovava.ware.vo.MemberAddressVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.ware.dao.WareInfoDao;
import com.sovava.ware.entity.WareInfoEntity;
import com.sovava.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
@Slf4j
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                new QueryWrapper<WareInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<WareInfoEntity> lqw = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        lqw.and(!StringUtils.isEmpty(key), (w) -> {
            w.eq(WareInfoEntity::getId, key)
                    .or().like(WareInfoEntity::getName, key)
                    .or().like(WareInfoEntity::getAddress, key)
                    .or().like(WareInfoEntity::getAreacode, key);
        });


        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params), lqw);

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        R info = memberFeignService.info(addrId);
        MemberAddressVo memberAddressInfo = new MemberAddressVo();
        if (info.getCode() == 0) {
            log.debug("??????????????????????????????");
            memberAddressInfo = info.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
            });
            log.debug("??????????????????{}", memberAddressInfo.toString());
        }
        FareVo fareVo = new FareVo();
        fareVo.setAddress(memberAddressInfo);
        //???????????????????????????????????? ??? ????????????????????????????????????????????????
        BigDecimal fare = new BigDecimal((new Random().nextInt() + 100) % 100);
        fareVo.setFare(fare);
        return fareVo;
    }

}