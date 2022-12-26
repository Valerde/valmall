package com.sovava.common.exception;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

public class NoStockException extends RuntimeException {
    @Setter
    @Getter
    private Long skuId;

    public NoStockException(@NotNull Long skuId) {
        super("商品Id" + skuId + "没有足够的库存");
    }

    public NoStockException() {
    }
}
