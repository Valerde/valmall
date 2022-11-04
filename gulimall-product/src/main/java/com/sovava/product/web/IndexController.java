package com.sovava.product.web;

import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import com.sovava.product.vo.Catelog2Vo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Resource
    private CategoryService categoryService;
    @GetMapping({"/","index.html"})
    public String indexPage(Model model){

        //TODO 查出所有的一级分类
        List<CategoryEntity> categoryEntityList =  categoryService.findLevel1Categories();
        model.addAttribute("categories",categoryEntityList);
        return "index";
    }

    //index/catalog.json
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String,List<Catelog2Vo>> getCatalogJSON(){
        Map<String,List<Catelog2Vo>> map =  categoryService.getCatalogJSON();
        return map;
    }



}
