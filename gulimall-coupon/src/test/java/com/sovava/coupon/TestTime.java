package com.sovava.coupon;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
public class TestTime {
    @Test
    public void testTimeAdd3Days() {
        LocalDate localDateTime = LocalDate.now();
        LocalDate localDateTimeAfter3Days = localDateTime.plusDays(3);
        LocalDate localDateTimeAfter5Days = localDateTime.plusDays(5);

        log.debug("现在的时间：{}", localDateTime.toString());
        log.debug("三天后的时间：{}", localDateTimeAfter3Days);
        log.debug("三天后的时间：{}", localDateTimeAfter5Days);

        LocalTime min = LocalTime.MIN;
        LocalTime max = LocalTime.MAX;
        log.debug("最小的时间为:{}", min);
        log.debug("最大的时间为:{}", max);

        LocalDateTime startTime = LocalDateTime.of(localDateTime, min);
        LocalDateTime endTime = LocalDateTime.of(localDateTimeAfter3Days, max);
        log.debug("起始时间：{}",startTime);
        log.debug("结束时间：{}",endTime);
    }
}
