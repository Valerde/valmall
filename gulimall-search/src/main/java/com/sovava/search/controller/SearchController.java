package com.sovava.search.controller;

import com.sovava.search.service.MallSearchService;
import com.sovava.search.vo.SearchParam;
import com.sovava.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
    @Autowired
    private MallSearchService mallSearchService;

    /**
     * @param searchParam 自动将发来的请求查询条件封装成整个对象
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request) {
        //1. 根据页面传递过来的查询参数，去es中查询结果
        searchParam.set_queryString(request.getQueryString());
        SearchResult result = mallSearchService.search(searchParam);
        model.addAttribute("result",result);
        return "list";
    }
}
