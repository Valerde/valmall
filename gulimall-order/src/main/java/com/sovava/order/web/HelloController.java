package com.sovava.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HelloController {
    @GetMapping("/{page}")
    public String listPage(@PathVariable("page") String page) {
        return page;
    }
}
