package com.sovava.search.vo;

import com.sovava.common.to.es.SpuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    /**
     * 查询到的所有商品信息
     */
    private List<SpuEsModel> product;
    /**
     * 当前页码
     */
    private Integer pageNum;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 总页码
     */
    private Integer totalPages;
    /**
     * 当前查询到的结果所有涉及到的品牌
     */
    private List<BrandVo> brands;
    /***
     * 查询到的结果的所有属性
     */
    private List<AttrVo> attrs;
    /**
     * 当前查询到的结果所涉及到的所有分类
     */
    private List<CatalogVo> catalogs;

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }
}
