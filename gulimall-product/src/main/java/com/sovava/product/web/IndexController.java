package com.sovava.product.web;

import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import com.sovava.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class IndexController {

    @Resource
    private CategoryService categoryService;

    @Resource
    private RedissonClient redissonClient;

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
        //2.加锁
        mylock.lock();//阻塞式等待，默认加的锁为30秒
        try {
            log.error("加锁成功，执行业务...{}",Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception ex) {
        } finally {
            log.error("释放锁...{}",Thread.currentThread().getId());
            //解锁，假设解锁代码没有运行，redisson不会死锁
            mylock.unlock();
        }
        return "hello";
    }

}
