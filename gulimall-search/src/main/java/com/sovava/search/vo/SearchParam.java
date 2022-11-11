package com.sovava.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面传过来的所有可能的检索条件
 */
@Data
public class SearchParam {
    /**
     * 传过来的全文匹配关键字
     */
    private String keyword;

    private Long catalog3Id;

    /**
     * 排序条件：
     * sort=saleCount_asc  saleCount_desc
     * sort=skuPrice_asc skuPrice_desc
     * sort=hotScore_asc hotScore_desc 按热度排序
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock = 1/0 有没有货
     * skuPrice = 100_500 / _500 / 500_  价格区间
     * brandId=1
     * attrs=1_其他:安卓&attrs=2_5寸:6寸
     */

    /**
     * 是否有货
     */
    private Integer hasStock ;

    /**
     * 价格区间
     */
    private String skuPrice;

    /**
     * 品牌Id&brandId=1&brandId=2  可以多选
     */
    private List<Long> brandId;

    /**
     * 按照属性进行筛选
     */
    private List<String> attrs;
    /**
     * 页码
     */
    private Integer pageNum=1;
}
