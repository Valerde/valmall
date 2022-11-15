package com.sovava.search.service.impl;

//import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSON;
import com.sovava.common.to.es.SpuEsModel;
import com.sovava.search.config.GulimallElasticSearchConfig;
import com.sovava.search.constant.EsConstant;
import com.sovava.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Override
    public Boolean productStatusUp(List<SpuEsModel> spuEsModels) throws IOException {

        //将数据直接保存进es中
        //1. 给es中建立索引 product ， 建立好映射关系 （已建立好）

        //2. 给es中保存数据
        //BulkResponse bulk(BulkRequest bulkRequest, RequestOptions options)
        BulkRequest bulkRequest = new BulkRequest();
        spuEsModels.forEach(item -> {
            //1. 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(item.getSkuId().toString());
            String jsonString = JSON.toJSONString(item);
            indexRequest.source(jsonString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        });

        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        boolean b = bulk.hasFailures();
        //TODO： 错误处理
        if (b)
            log.error("商品上架错误{}", Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList()));

        return !b;
    }
}
