package com.sovava.product.exception;


import com.sovava.common.exception.BizCodeEnum;
import com.sovava.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 集中处理所有异常
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.sovava.product.controller")/*@ControllerAdvice @ResponseBody 的结合体*/
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException ex){
        Map<String,String> map = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((item)->{
            String field = item.getField();
            String defaultMessage = item.getDefaultMessage();
            map.put(field,defaultMessage);
        });
        log.error("数据校验出现问题{},异常类型为{}",ex.getMessage(),ex.getClass());

        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(), BizCodeEnum.VALID_EXCEPTION.getMessage()).put("data",map);
    }


    @ExceptionHandler(value = Exception.class)
    public R handleException(Exception ex){
        log.error("出现问题{},异常类型为{}",ex.getStackTrace(),ex.getClass());
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(),BizCodeEnum.UNKNOWN_EXCEPTION.getMessage());
    }

}
