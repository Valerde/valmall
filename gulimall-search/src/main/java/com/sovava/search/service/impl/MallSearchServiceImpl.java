package com.sovava.search.service.impl;

//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.sovava.common.to.es.SpuEsModel;
import com.sovava.common.utils.R;
import com.sovava.search.config.GulimallElasticSearchConfig;
import com.sovava.search.constant.EsConstant;
import com.sovava.search.feign.ProductFeignService;
import com.sovava.search.service.MallSearchService;
import com.sovava.search.vo.AttrRespVO;
import com.sovava.search.vo.BrandEntity;
import com.sovava.search.vo.SearchParam;
import com.sovava.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.bouncycastle.util.encoders.UTF8;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MallSearchServiceImpl implements MallSearchService {
    @Resource
    private RestHighLevelClient client;

    @Resource
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult searchResult = null;
        //1. 动态构建出dsl语句

        //2. 准备检索请求

        SearchRequest searchRequest = new SearchRequest();

        searchRequest = buildSearchRequest(searchParam);
        try {
            //3. 执行检索请求
            SearchResponse search = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            //4. 分析响应数据 ， 封装成我们需要的格式
            searchResult = buildSearchResult(search, searchParam);

        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("查询结果:{}", searchResult.toString());
        return searchResult;

    }

    /**
     * 构建结果格式
     *
     * @param search
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse search, SearchParam param) {
        SearchResult searchResult = new SearchResult();

        SearchHits hits = search.getHits();
        List<SpuEsModel> spuEsModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SpuEsModel spuEsModel = JSON.parseObject(sourceAsString, new TypeReference<SpuEsModel>() {
                });
                //设置高亮
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    spuEsModel.setSkuTitle(hit.getHighlightFields().get("skuTitle").getFragments()[0].string());
                }
                spuEsModels.add(spuEsModel);
            }
        }
        searchResult.setProduct(spuEsModels);

        Aggregations aggregations = search.getAggregations();
        //设置分类
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        List<SearchResult.CatalogVo> catalogs = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            String keyAsString = bucket.getKeyAsString();

            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //从子聚合拿到名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogs.add(catalogVo);

        }
        searchResult.setCatalogs(catalogs);

        //设置品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> brand_aggBuckets = brand_agg.getBuckets();

        for (Terms.Bucket bucket : brand_aggBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //品牌名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            List<? extends Terms.Bucket> buckets1 = brand_name_agg.getBuckets();
            String brandName = "暂无";
            if (buckets1.size() > 0)
                brandName = buckets1.get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //品牌的图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = "暂无";
            List<? extends Terms.Bucket> buckets2 = brand_img_agg.getBuckets();
            if (buckets2.size() > 0)
                brandImg = buckets2.get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);

        //设置属性
        ParsedNested attr_agg = aggregations.get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attr_id_agg.getBuckets();
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        for (Terms.Bucket bucket : attrIdAggBuckets) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            ParsedStringTerms aggr_name_agg = bucket.getAggregations().get("aggr_name_agg");
            String aggrName = aggr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(aggrName);

            ParsedStringTerms aggr_value_agg = bucket.getAggregations().get("aggr_value_agg");
            List<String> attrValue = new ArrayList<>();
            for (Terms.Bucket aggrValueAggBucket : aggr_value_agg.getBuckets()) {
                String value = aggrValueAggBucket.getKeyAsString();
                attrValue.add(value);
            }
            attrVo.setAttrValue(attrValue);


            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);

        //===========以上从hit拿到===============
        searchResult.setTotal(hits.getTotalHits().value);
        int page = (int) (hits.getTotalHits().value % EsConstant.PRODUCT_PAGESIZE);
        searchResult.setTotalPages((int) (page == 0 ? hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE : (hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE + 1)));
        searchResult.setPageNum(param.getPageNum());

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= searchResult.getTotalPages(); i++) {
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);

        //构建面包屑导航功能
        if (param.getAttrs() != null && param.getAttrs().size() != 0) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //分析每一个attrs传过来的参数值
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                navVo.setNavName(s[0]);
                R info = productFeignService.info(Long.parseLong(s[0]));
                searchResult.getAttrIds().add(Long.parseLong(s[0]));
                if (info.getCode() == 0) {
                    AttrRespVO attrRespVO = info.getData("attr", new TypeReference<AttrRespVO>() {
                    });
                    navVo.setNavName(attrRespVO.getAttrName());
                }

                //取消了面包屑以后，我们要跳转的地方 ， 将请求地址的url里面的当前置空

                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.valmall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());
            log.debug("面包屑信息{}", Arrays.toString(navVos.toArray()));
            searchResult.setNavs(navVos);

        }
        //品牌面包屑 ， 分类面包屑
        if (param.getBrandId() != null && param.getBrandId().size() != 0) {
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //查询远程品牌信息
            R infos = productFeignService.brandInfos(param.getBrandId());
            if (infos.getCode() == 0) {
                List<BrandEntity> brands = infos.getData("brand", new TypeReference<List<BrandEntity>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandEntity brand : brands) {
                    buffer.append(brand.getName()).append(":");
                    replace = replaceQueryString(param, brand.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.valmall.com/list.html?" + replace);
            }

            navs.add(navVo);
            searchResult.setNavs(navs);
        }
        //分类



        return searchResult;
    }

    private static String replaceQueryString(SearchParam param, String value, String key) {
        String encode;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String replace = "";
        replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        return replace;
    }

    /**
     * 准备请求
     * 模糊匹配， 过滤（属性，分类，品牌，价格区间，库存），分页，排序，高亮，聚合分析
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //构建dsl语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        /**
         * 模糊匹配， 过滤（属性，分类，品牌，价格区间，库存）
         */
        //1. 构建了boolquery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 构建must模糊匹配
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }
        //1.2 构建filter
        if (searchParam.getCatalog3Id() != null) {//分类
            boolQuery.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() != 0) {//品牌id进行查询
            boolQuery.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() != 0) {//属性
            for (String attr : searchParam.getAttrs()) {
                //attrs=1_5寸&attrs=2_8G:16G
                BoolQueryBuilder nestBoolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];//属性id
                String[] attrValues = s[1].split(":");//属性值
                nestBoolQuery.must(QueryBuilders.termsQuery("attrs.attrId", attrId));
                nestBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //每一个都必须生成一个nested 查询
                boolQuery.filter(QueryBuilders.nestedQuery("attrs", nestBoolQuery, ScoreMode.None));
            }

        }

        //有无库存
        if (searchParam.getHasStock() != null)
            boolQuery.filter(QueryBuilders.termsQuery("hasStock", searchParam.getHasStock() == 1));

        //按照价格区间
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {

            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String skuPrice = searchParam.getSkuPrice();
            //1_500/_500/500_
            String[] s = skuPrice.split("_");
            log.debug("价格区间{}", s.toString());


            if (skuPrice.startsWith("_")) rangeQueryBuilder.lte(s[1]);
            else if (skuPrice.endsWith("_")) rangeQueryBuilder.gte(s[0]);
            else {
                rangeQueryBuilder.gte(s[0]).lte(s[1]);
            }

            boolQuery.filter(rangeQueryBuilder);
        }
//        把以前所有属性都拿来进行封装
        searchSourceBuilder.query(boolQuery);


        /**
         * 分页，排序，高亮
         */
        //2.1 排序 sort=saleCount_asc
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            searchSourceBuilder.sort(s[0], s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        //2.2 分页
        searchSourceBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3 高亮
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        log.debug("sourceBuilder:{}", searchSourceBuilder.toString());
        /**
         * 聚合分析
         */
        //品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        //分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(50);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        //属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg");
        attr_id_agg.field("attrs.attrId").size(50);
        attr_id_agg.subAggregation(AggregationBuilders.terms("aggr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("aggr_value_agg").field("attrs.attrValue").size(1));

        attr_agg.subAggregation(attr_id_agg);
        searchSourceBuilder.aggregation(attr_agg);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);


        return searchRequest;
    }
}
