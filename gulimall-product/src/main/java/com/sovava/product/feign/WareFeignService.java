package com.sovava.product.feign;

import com.sovava.common.utils.R;
import com.sovava.product.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasstock")
    public R selectHasStock(@RequestBody List<Long> skuIds) ;
}
