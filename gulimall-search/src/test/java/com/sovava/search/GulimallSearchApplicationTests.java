package com.sovava.search;

import com.alibaba.fastjson.JSON;
import com.sovava.search.config.GulimallElasticSearchConfig;
import com.sovava.search.pojo.Account;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Resource
    private RestHighLevelClient client;

    @Test
    void contextLoads() {
        System.out.println(client);
    }

    @Data
    class User {
        private String userName;
        private int age;
        private String gender;
    }

    /**
     * 测试保存数据
     * 更新也可以
     *
     * @throws IOException
     */
    @Test
    void index() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
//        indexRequest.id("1");
//        indexRequest.source("username","zhangsan","age",18,"gender","男");
        User user = new User();
        user.setUserName("李四");
        user.setAge(20);
        user.setGender("男");
        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);//要保存的内容


        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(index);
    }

    @Test
    public void searchData() throws IOException {
        //1. 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //2. 制定索引
        searchRequest.indices("bank");
        // 3. 指定DSL ， 检索条件
        // SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SearchSourceBuilder builder = new SearchSourceBuilder();
        // 构造检索条件
        builder.query(QueryBuilders.matchQuery("address", "mill"));

//        builder.from(10);
        builder.size(100);

//        builder.aggregation(new )
        //按照年龄的值分布进行聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        builder.aggregation(ageAgg);

        //计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        builder.aggregation(balanceAvg);
        System.out.println(builder.toString());
        searchRequest.source(builder);

        //4. 执行检索
        SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);


        //5.分析结果
//        response.get
        System.out.println(response.toString());
        SearchHits hits = response.getHits();

        SearchHit[] hits1 = hits.getHits();
        for (SearchHit documentFields : hits1) {
            String sourceAsString = documentFields.getSourceAsString();
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
//            System.out.println("sourceAsMap" + sourceAsMap.toString());

            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }

        //获取这次检索得到的聚合信息
        Aggregations aggregations = response.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {

            String keyAsString = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            System.out.println(keyAsString + ":" + docCount);
        }

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println(balanceAvg1.getValue());
    }
}




