package com.sovava.common.to;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {
    /**
     * 库存工作单的ID
     */
    private Long id;
    /**
     * 工作单的详情的ID
     */
    private StockDetailTo detailTo;
}
