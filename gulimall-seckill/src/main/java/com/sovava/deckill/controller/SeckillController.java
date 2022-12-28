package com.sovava.deckill.controller;

import com.sovava.common.utils.R;
import com.sovava.deckill.service.SeckillService;
import com.sovava.deckill.to.SeckKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 返回当前时间参与的秒杀商品信息
     *
     * @return
     */
    @GetMapping("/currentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus() {
        List<SeckKillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getskuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SeckKillSkuRedisTo to = seckillService.getskuSeckillInfo(skuId);
        return R.ok().setData(to);

    }

    /**
     * http://seckill.gulimall.com/kill?killId=1-9&key=901a9f1d58c041329f19ce77359c5457&num=1
     *
     * @return
     */
    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId,
                     @RequestParam("key") String key,
                     @RequestParam("num") Integer num,
                     Model model
    ) {
        //已经在拦截器里判断了是否登录

        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
