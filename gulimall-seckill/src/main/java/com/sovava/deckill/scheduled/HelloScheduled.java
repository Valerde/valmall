package com.sovava.deckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 * 1. @EnableScheduling
 * 2.    @Scheduled(cron = "* * * * * 2")
 * 3. TaskSchedulingAutoConfiguration
 * 异步任务：
 * 1. @EnableAsync
 * 2. @Async
 * 3. TaskExecutionAutoConfiguration
 */
@EnableScheduling
@Slf4j
@Component
@EnableAsync
public class HelloScheduled {
    /**
     * spring 中不允许出现年<br>
     * 在周几的位置，1-7表示周一到周日<br>
     * 定时任务不应该阻塞,默认是阻塞的<br>
     * 解决办法：<br>
     * 1) 线程池让业务以异步的方式<br>
     * 2） 设置定时任务线程池<br>
     * task:     *     scheduling:     *       pool:     *         size: 5 不好使
     * 3）让定时任务异步执行
     * <p>
     * 解决的定时任务阻塞的方法： 定时任务异步执行
     */
    @Scheduled(cron = "* * * * * 2")
    @Async
    public void hello() throws InterruptedException {
        log.debug("这是一个定时任务");
        Thread.sleep(3000);
    }
}
