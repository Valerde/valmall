package com.sovava.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.coupon.entity.SeckillSkuRelationEntity;
import com.sovava.coupon.service.SeckillSkuRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.coupon.dao.SeckillSessionDao;
import com.sovava.coupon.entity.SeckillSessionEntity;
import com.sovava.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
@Slf4j
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLasted3DaysSession() {
        //计算最近三天的时间  2020-12-27 23:31:11    加三天
        LocalDate localDateTime = LocalDate.now();
        LocalDate localDateTimeAfter3Days = localDateTime.plusDays(3);

        log.debug("现在的时间：{}", localDateTime.toString());
        log.debug("三天后的时间：{}", localDateTimeAfter3Days);

        LocalDateTime startTime = LocalDateTime.of(localDateTime, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(localDateTimeAfter3Days, LocalTime.MAX);
        log.debug("起始时间：{}", startTime);
        log.debug("结束时间：{}", endTime);

        String start = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String end = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//        select * from `sms_seckill_session`where start_time between '' and ' ';
        LambdaQueryWrapper<SeckillSessionEntity> lqw = new LambdaQueryWrapper<>();
        lqw.between(SeckillSessionEntity::getStartTime, start, end);
        List<SeckillSessionEntity> list = this.list(lqw);
        log.debug("查询到的时间为：{}", list.toString());


        //把活动查出来之后应该顺便把商品页查出来
        if (list != null && list.size() > 0) {
            list = list.stream().map(session -> {
                Long sessionId = session.getId();
                List<SeckillSkuRelationEntity> seckillSkuRelationEntities = seckillSkuRelationService.listBySessionId(sessionId);

                session.setRelationSkus(seckillSkuRelationEntities);

                return session;
            }).collect(Collectors.toList());
        }
        return list;

    }

}