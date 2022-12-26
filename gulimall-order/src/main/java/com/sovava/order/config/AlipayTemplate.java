package com.sovava.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;

import com.sovava.order.vo.PayVo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private String app_id = "2021000122604267";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCOfffxRnSjz/TT+kCEDLrQtA/UBEhUmqbgV8mxt4dR75B4H3FM1nRiN66Xwz6K2XGSek9M/8AsKG98YLT65aES7paSZBfddyrp4Srit6IWR4M73++/edfWuEeYPiOP/eQ/QaKVDg1wnYDs5d6c+mstB3DV14uLEwmiTxYBuzcxC91GZ9zcvQ7cgOsEqMPh4k5to6UDaFQk95nyaS0HuByAinbiBNWNpyqg85j0f1EaTIQT4QQRGX7BuEjKjSN29CMsr1AYa0dCUP/UkK+KVO5cKLcYQY+uZBRfQT1oIvJ/WnzhqyM2oP/ixl2RcxSM2iH7e0U8dVmHH4cQfdKQNSfNAgMBAAECggEAbhS3Jbiuh5cyp0jf6qQC9xUEXWDpmCLZ5NstQJRugEg5LfPsjdXnl7E4upnliVMvQGApP20dQ4ABkO8NIf+gjoWv4WxeW4OirYh8nyYKnHem1OzNxPkWXBWFigIHa0hTuuKz/b8bdvLXFS0I6/EyN1lWPH4GmeydNr0uXSwLGWHE9Mv8v+u9BYVb8r522twYnWUG/1UvWzVXQbItj7Zza+5XnodeMUvVv57f2/iipUXnvBR2SrbP4BzTpfNUU3Ae6m5NG02OC6bcX+v51MC2P0/uUI1cUmpaa/QqoYd1Fbtiy2D66IUaPttkwm81GVwZirF4AxVYBErrtUgGFEc/QQKBgQD6yOYJH0JAeb2C+O5o8mLv78g+WmesswZNf9Wd/yImUaI49zG7rZ8nUKcOPSTx8SuglGGx1a/f5J2g615BMxpi085zrAqJss+BWJn2SBomjwunxfzwYmZyo51DwRbS60Qyogi91TUqKIIIm438P5dl40jlQpGWPhks4zCmg9eR1QKBgQCRdI1/05AowWzdmqMJokC5bTF8yOBhoDbXKq3GN+w3W10BBmggZPYHCbCWmB9z5FRtadr9aXMX02Hj1eJ99FfdEwEfXEuotmJWSvoFiMMaZpIvhwmCgehZxTGmRp0DaK52JLc/KFNWnNdUjpCQ87+HkNWzG98I4cKwYCYcQtNCGQKBgB9XB9hP/bmM/S/m5/RZhh+x+XZBWf9tB/abaRirj5r2TK2NxrtLgT4qiBXxcjKCDw+sO2E78RSrvz8i6s5/EmcTTROLhm2a2O+zINp0PEBhU/WNeoSX53GgHMEg2jdVi1pAgZMnl9NC6K0ms2G4mLT06GEuJn9+FytS4PuOMlxZAoGAPW+uSHxymlRAiEVFcc8/aVKOoXczFukV4MHdDikWs7b6THGJT411QTm9hY4RUuDxLBmW7ow4maql4Ra5CJxI9E6PndBdAJCwbmRSwD2osqD6Q+rUgHQgULJyqxmOPh9b+Pi/EYUV1jJ+3O8ubpsVncv2ByXRia3Zjqe90RgEpRkCgYEAroYC8nWuEP8kWIYbDLuUWha2HCeuDF6p/1ngQPS44R7RSxrN/dWSn8J76/iGd6NldqK2sGskGty8IYCiHzPL20oGiwS8g/+Fn+SMTyojF66nOA5opN6228q3RsmH/V0qVD3iLbnau3tc70Kzw7mWqSfYwWXLnhNJVOPf+RwhkoA=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgTt2qJ7Z5lJzWpukcuTpFtqueeWznkNUiliaZgT5tpLqEIyZTj7L3vlC9m4TziPhyj3AtzFXI/LHJ6FhowjV/A9L4Yoke6KsC2va+kf8ZhNk0Us8sdgRvmILGpsglWWSrry94ed39xG08Bzwt5pN55bZHLzT7xpUR6N8oZtrPiXNat9HCl/NT6G+TEQ5W9HhHSKqJ+uN0EAyWy1drCBcUz2pkQ3zKHoSifehrH1eJsKU/BFuv161ViIFLx0tVFJQ05hv58CKea8nJ4hJkV17BrVe7yOsqGv6354jV7x8I/4X7lKeKF1NlBroLGKNFIUCi/2vfrE5LUL6Cur6BGVlnwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    @Value("alipay.notify_url")
    private String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    @Value("alipay.return_url")
    private String return_url;

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应：" + result);

        return result;

    }
}
