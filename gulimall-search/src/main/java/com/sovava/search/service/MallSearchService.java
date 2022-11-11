package com.sovava.search.service;

import com.sovava.search.vo.SearchParam;
import com.sovava.search.vo.SearchResult;

public interface MallSearchService {
    /**
     *
     * @param searchParam 检索的所有参数
     * @return 最终返回所有检索的结果
     */
    SearchResult search(SearchParam searchParam);
}
