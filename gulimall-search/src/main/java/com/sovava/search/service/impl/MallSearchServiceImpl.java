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
        //1. ???????????????dsl??????

        //2. ??????????????????

        SearchRequest searchRequest = new SearchRequest();

        searchRequest = buildSearchRequest(searchParam);
        try {
            //3. ??????????????????
            SearchResponse search = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            //4. ?????????????????? ??? ??????????????????????????????
            searchResult = buildSearchResult(search, searchParam);

        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("????????????:{}", searchResult.toString());
        return searchResult;

    }

    /**
     * ??????????????????
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
                //????????????
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    spuEsModel.setSkuTitle(hit.getHighlightFields().get("skuTitle").getFragments()[0].string());
                }
                spuEsModels.add(spuEsModel);
            }
        }
        searchResult.setProduct(spuEsModels);

        Aggregations aggregations = search.getAggregations();
        //????????????
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        List<SearchResult.CatalogVo> catalogs = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            String keyAsString = bucket.getKeyAsString();

            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //????????????????????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogs.add(catalogVo);

        }
        searchResult.setCatalogs(catalogs);

        //????????????
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> brand_aggBuckets = brand_agg.getBuckets();

        for (Terms.Bucket bucket : brand_aggBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //??????id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //????????????
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            List<? extends Terms.Bucket> buckets1 = brand_name_agg.getBuckets();
            String brandName = "??????";
            if (buckets1.size() > 0)
                brandName = buckets1.get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //???????????????
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = "??????";
            List<? extends Terms.Bucket> buckets2 = brand_img_agg.getBuckets();
            if (buckets2.size() > 0)
                brandImg = buckets2.get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);

        //????????????
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

        //===========?????????hit??????===============
        searchResult.setTotal(hits.getTotalHits().value);
        int page = (int) (hits.getTotalHits().value % EsConstant.PRODUCT_PAGESIZE);
        searchResult.setTotalPages((int) (page == 0 ? hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE : (hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE + 1)));
        searchResult.setPageNum(param.getPageNum());

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= searchResult.getTotalPages(); i++) {
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);

        //???????????????????????????
        if (param.getAttrs() != null && param.getAttrs().size() != 0) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //???????????????attrs?????????????????????
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

                //??????????????????????????????????????????????????? ??? ??????????????????url?????????????????????

                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.valmall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());
            log.debug("???????????????{}", Arrays.toString(navVos.toArray()));
            searchResult.setNavs(navVos);

        }
        //??????????????? ??? ???????????????
        if (param.getBrandId() != null && param.getBrandId().size() != 0) {
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("??????");
            //????????????????????????
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
        //??????



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
     * ????????????
     * ??????????????? ??????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //??????dsl??????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        /**
         * ??????????????? ????????????????????????????????????????????????????????????
         */
        //1. ?????????boolquery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 ??????must????????????
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }
        //1.2 ??????filter
        if (searchParam.getCatalog3Id() != null) {//??????
            boolQuery.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() != 0) {//??????id????????????
            boolQuery.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() != 0) {//??????
            for (String attr : searchParam.getAttrs()) {
                //attrs=1_5???&attrs=2_8G:16G
                BoolQueryBuilder nestBoolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];//??????id
                String[] attrValues = s[1].split(":");//?????????
                nestBoolQuery.must(QueryBuilders.termsQuery("attrs.attrId", attrId));
                nestBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //??????????????????????????????nested ??????
                boolQuery.filter(QueryBuilders.nestedQuery("attrs", nestBoolQuery, ScoreMode.None));
            }

        }

        //????????????
        if (searchParam.getHasStock() != null)
            boolQuery.filter(QueryBuilders.termsQuery("hasStock", searchParam.getHasStock() == 1));

        //??????????????????
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {

            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String skuPrice = searchParam.getSkuPrice();
            //1_500/_500/500_
            String[] s = skuPrice.split("_");
            log.debug("????????????{}", s.toString());


            if (skuPrice.startsWith("_")) rangeQueryBuilder.lte(s[1]);
            else if (skuPrice.endsWith("_")) rangeQueryBuilder.gte(s[0]);
            else {
                rangeQueryBuilder.gte(s[0]).lte(s[1]);
            }

            boolQuery.filter(rangeQueryBuilder);
        }
//        ??????????????????????????????????????????
        searchSourceBuilder.query(boolQuery);


        /**
         * ????????????????????????
         */
        //2.1 ?????? sort=saleCount_asc
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            searchSourceBuilder.sort(s[0], s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        //2.2 ??????
        searchSourceBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3 ??????
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        log.debug("sourceBuilder:{}", searchSourceBuilder.toString());
        /**
         * ????????????
         */
        //????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        //????????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(50);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        //????????????
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
