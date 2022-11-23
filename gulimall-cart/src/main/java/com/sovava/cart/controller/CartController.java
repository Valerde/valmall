package com.sovava.cart.controller;

import com.sovava.cart.interceptor.CartInterceptor;
import com.sovava.cart.service.CartService;
import com.sovava.cart.vo.CartItemVo;
import com.sovava.cart.vo.CartVo;
import com.sovava.cart.vo.UserInfoTo;
import com.sovava.common.constant.AuthServerConstant;
import com.sovava.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@Slf4j
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 浏览器有一个cookie： user-key ， 标致用户身份 ， 一个月后过期
     * 如果用户第一次使用jd ， 就会给一个临时用户身份，
     * 浏览器保存 ， 以后访问都携带这个cookie
     * <p>
     * 登录：session有
     * 没登录按照cookie里面的临时用户信息来做
     * 没有临时用户 ，用拦截器 创建一个临时用户
     *
     * @return
     */
    @GetMapping("cart.html")
    public String list(Model model) {

        CartVo cart = cartService.cartService();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     *
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/addCartItem")
    public String addToCart(@RequestParam("skuId") Long skuId
            , @RequestParam("num") Integer num
            , RedirectAttributes redirectAttributes) {
        CartItemVo cartItemVo = cartService.addToCart(skuId, num);
//        model.addAttribute("cartItem", cartItemVo);
        redirectAttributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.valmall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到添加成功页，可以有效用来防止重复提交
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId, Model model) {
        //重定向到成功页面 ， 再次查询购物车即可
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItemVo);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId
            , @RequestParam("checked") Integer checked) {
        cartService.updateChecked(skuId, checked);
        return "redirect:http://cart.valmall.com/cart.html";
    }

    //http://cart.valmall.com/countItem?skuId=10&num=2
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.updateCount(skuId, num);
        return "redirect:http://cart.valmall.com/cart.html";
    }

    //http://cart.valmall.com/deleteItem?skuId=10
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.updateCount(skuId, 0);
        return "redirect:http://cart.valmall.com/cart.html";
    }

    @GetMapping("/currentUserCartItem")
    @ResponseBody
    public List<CartItemVo> getCurrentUserCartItem() {

        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        log.debug("userInfo = {}", userInfoTo);
        Long userId = userInfoTo.getUserId();
        if (userId == null) return null;
        log.debug("userId == {}", userId);
        List<CartItemVo> userCartItems = cartService.getUserCartItems(userId.toString());
        log.debug("userCartItems = {}", userCartItems);
        return userCartItems;
    }

}
