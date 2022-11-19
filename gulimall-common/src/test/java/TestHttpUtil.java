import com.alibaba.fastjson2.JSON;
import com.sovava.common.utils.HttpUtil;

public class TestHttpUtil {
    //https://api.weibo.com/oauth2/access_token?client_id=3979802358&client_secret=e7a1e09c4246bcd8d673319fce4f4802&grant_type=authorization_code&redirect_uri=http://valmall.com/success&code=04116c7b2c9b60ac06c039c2fe56e647
    public static void main(String[] args) {
        HttpUtil.ReqData reqData = HttpUtil.createReq();
        //请求方式
        reqData.setMethod("POST");
        //数据请求类型
        reqData.setContentType("application/json");
        //参数
//        reqData.addReqParameter("status", -1);
//        reqData.addReqParameter("v", "1.0");
        //添加header请求头信息
//        reqData.addReqProperty("token", "A74DF5F691C24A158A50A2D880284665");
        reqData.addReqParameter("client_id","3979802358");
        reqData.addReqParameter("client_secret","e7a1e09c4246bcd8d673319fce4f4802");
        reqData.addReqParameter("grant_type","authorization_code");
        reqData.addReqParameter("redirect_uri","http://valmall.com/oauth2.0/weibo/success");
        reqData.addReqParameter("code","1ebc63a073a32821b8442799e8f55489");
        //请求地址
        reqData.setUrl("https://api.weibo.com/oauth2/access_token");
        //调用接口
        HttpUtil.RespData respData = HttpUtil.reqConnection(reqData);
        //获得返回的json字符串
        String result = JSON.toJSONString(respData);
        System.out.println("获得的json:" + result);
    }
}
//        reqData.addReqParameter("access_token", "2.00UY8BWIc5q12E4dbdd16fb9ImFORE");
//        reqData.addReqParameter("uid", "7802249662");
