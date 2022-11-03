package com.sovava.search.service;

import com.sovava.common.to.es.SpuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {
    /**
     * 保存sku的检索数据
     * @param spuEsModels
     */
    Boolean productStatusUp(List<SpuEsModel> spuEsModels) throws IOException;
}
