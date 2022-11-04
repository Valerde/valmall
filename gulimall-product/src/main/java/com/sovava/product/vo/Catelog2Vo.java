package com.sovava.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catelog2Vo {
    private String catalog1Id;//1级父分类id
    private List<Catalog3Vo> catalog3List;// 3级子分类
    private String id; //二级分类id
    private String name;

    /**
     * 三级分类Vo
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catalog3Vo {
        private String catalog2Id;
        private String id;
        private String name;

    }
}
