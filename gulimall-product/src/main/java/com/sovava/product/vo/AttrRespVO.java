package com.sovava.product.vo;

import lombok.Data;

/**
 * 响应属性vo
 */
@Data
public class AttrRespVO extends AttrVo {

    /**
     * "catelogName": "手机/数码/手机", //所属分类名字
     */
    private String catelogName;


    /**
     * "groupName": "主体", //所属分组名字
     */
    private String groupName;

    private  Long[] catelogPath;
}
