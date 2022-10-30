package com.sovava.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {
//     "itemId": 1,  "status": 3,   "reason": ""
    private Long itemId;
    private Integer status;
    private String reason;
}
