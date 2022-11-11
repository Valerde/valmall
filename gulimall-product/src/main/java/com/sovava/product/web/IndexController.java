package com.sovava.product.web;

import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import com.sovava.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
public class IndexController {

    @Resource
    private CategoryService categoryService;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping({"/", "index.html"})
    public String indexPage(Model model) {

        //TODO 查出所有的一级分类
        List<CategoryEntity> categoryEntityList = categoryService.findLevel1Categories();
        model.addAttribute("categories", categoryEntityList);
        return "index";
    }

    //index/catalog.json
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJSON() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJSON();
        return map;
    }


    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        //1. 获取一把锁，只要锁的名字一样，就是同一把锁
        RLock mylock = redissonClient.getLock("mylock");
        // 一/锁的自动续期，如果业务时间超长，运行期间会自动给锁续期，不用担心出现死锁，锁自动过期被删掉
        // 二/锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁锁默认在30秒后自动删除
        // 三/锁的自动续期为30/3=10秒一续期
        //2.加锁
        mylock.lock();//阻塞式等待，默认加的锁为30秒
//        mylock.lock(10, TimeUnit.SECONDS);
        //如果设置了超时时间超长， 并不会自动续期 ， 那么到达时间后手动删除就会报错，未找到锁因为锁已经被删除了
        //所以要让锁的过期时间大于业务执行时间，自己设置过期时间没有看门狗
        try {
            log.error("加锁成功，执行业务...{}", Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception ex) {
        } finally {
            log.error("释放锁...{}", Thread.currentThread().getId());
            //解锁，假设解锁代码没有运行，redisson不会死锁
            mylock.unlock();
        }
        return "hello";
    }


    /**
     * 读写锁，保证一定能读到最新数据
     * 因为在写锁锁上时，是不能读取的，写锁没释放 ， 读锁必须等待
     * 写锁是一个排他锁（互斥锁）
     * 读锁是一个共享锁
     * <p>
     * 读+读： 不排斥
     * 读+写： 等待读锁释放
     * 写+读： 等待写锁释放
     * 写+写： 阻塞
     *
     * @return
     */
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String s = null;
        RLock rLock = lock.writeLock();
        try {
            //1. 改数据加写锁读数据加读锁
            rLock.lock();
            log.error("写锁加锁成功{}", Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30 * 1000);
            redisTemplate.opsForValue().set("writeUUID", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            log.error("写锁释放成功{}", Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = lock.readLock();
        //加读锁
        rLock.lock();
        log.error("读锁加锁成功{}", Thread.currentThread().getId());
        String writeUUID = "";
        try {
            writeUUID = redisTemplate.opsForValue().get("writeUUID");
            Thread.sleep(30 * 1000);
        } catch (Exception e) {
        } finally {
            rLock.unlock();
            log.error("读锁释放成功{}", Thread.currentThread().getId());
        }

        return writeUUID;
    }


    /**
     * 停车位，三个车位
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
//        park.acquire();//阻塞式获取一个信号（资源）
        boolean b = park.tryAcquire();//如果获取信号量不成功，返回false ， 获取成功返回true可以用作分布式限流
        return "ok" + b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();
        return "ok";
    }


    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();//等待闭锁完成
        return "放假了";
    }

    @GetMapping("/go/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();//计数减一
        return id + "班的人都走光了";
    }
}
