package com.sovava.common.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

/**
 * http请求工具（调用第三方接口）
 * public static void main(String[] args) {
 * <p>
 * **********无参数和无请求头的情况*******************
 * /创建请求对象
 * ReqData reqData=HttpUtil.createReq();
 * //请求方式
 * reqData.setMethod("POST");
 * //数据请求类型
 * reqData.setContentType("Application/json");
 * //请求地址
 * reqData.setUrl("http://wx.cqzfj.com/xmy-b2b-app-web/index?v=1.0");
 * //调用接口
 * RespData respData=HttpUtil.reqConnection(reqData);
 * //获得返回的json字符串
 * String result=JSON.toJSONString(respData);
 * System.out.println("获得的json:"+result);*
 * <p>
 * **********带参数和请求头的情况*******************
 * //创建请求对象
 * ReqData reqData=HttpUtil.createReq();
 * //请求方式
 * reqData.setMethod("POST");
 * //数据请求类型
 * reqData.setContentType("application/json");
 * //参数
 * reqData.addReqParameter("status",-1);
 * reqData.addReqParameter("v","1.0");
 * //添加header请求头信息
 * reqData.addReqProperty("token","A74DF5F691C24A158A50A2D880284665");
 * //请求地址
 * reqData.setUrl("http://****.com/list");
 * //调用接口
 * RespData respData=HttpUtil.reqConnection(reqData);
 * //获得返回的json字符串
 * String result=JSON.toJSONString(respData);
 * System.out.println("获得的json:"+result);
 * <p>
 * ***********也可以通过这种方式传header**************
 * Map<String, String> header=new HashMap<String, String>();
 * header.put("token","A74DF5F691C24A158A50A2D880284665");
 * ReqData reqData=HttpUtil.createReq(header);
 * }
 */
public final class HttpUtil {

    private final static int REQ_EX_CODE = -99;
    private final static int TIME_OUT = 10 * 10000000; // 超时时间

    private HttpUtil() {
    }


    public static RespData reqConnection(ReqData reqData) {
        URL url = null;
        BufferedWriter out = null;
        BufferedReader reader = null;
        HttpURLConnection urlConnection = null;
        RespData respData = null;
        try {
            //设置请求参数
            StringBuilder arg = null;
            String para = "";
            Map<String, Object> parameter = reqData.getParameter();
            if (null != parameter && !parameter.isEmpty()) {
                arg = new StringBuilder();
                for (Map.Entry<String, Object> entry : parameter.entrySet()) {
                    arg.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                para = "?" + arg.substring(0, arg.length() - 1);
            }
            url = new URL(reqData.getUrl() + para);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            if (0 != reqData.getTimeOut()) {
                urlConnection.setReadTimeout(reqData.getTimeOut());
                urlConnection.setConnectTimeout(reqData.getTimeOut());
            }
            urlConnection.setRequestMethod(reqData.getMethod().toUpperCase());
            urlConnection.setRequestProperty("Content-Type", reqData.getContentType());
            //设置请求头
            Map<String, String> hMap = reqData.getHeader();
            if (null != hMap && !hMap.isEmpty()) {
                for (Map.Entry<String, String> entry : hMap.entrySet()) {
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            urlConnection.connect();
            if (ReqData.ReqType.POST.get().equals(reqData.getMethod()) || ReqData.ReqType.PUT.get().equals(reqData.getMethod())) {
                out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), Charset.forName("utf-8")));
            }
            //设置请求内容(json字符串的请求)
            String content = reqData.getReqContent();
            if (!isEmpty(content)) {
                if (null == out) {
                    out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), Charset.forName("utf-8")));
                }
                out.write(content);
                out.flush();
            }
            respData = getRespData(reader, urlConnection);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            respData = createRespData(REQ_EX_CODE, "URL不正确。");
        } catch (ProtocolException e) {
            e.printStackTrace();
            respData = createRespData(REQ_EX_CODE, "协议不正确。");
        } catch (IOException e) {
            e.printStackTrace();
            respData = createRespData(REQ_EX_CODE, "数据流读写失败。");
        } finally {
            close(out);
            close(reader);
            if (null != urlConnection) {
                urlConnection.disconnect();
                urlConnection = null;
            }
        }
        return respData;
    }

    private static RespData getRespData(BufferedReader reader, HttpURLConnection urlConnection) throws IOException {
        RespData respData = null;
        int code = urlConnection.getResponseCode();
        switch (code) {
            case 200:
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), Charset.forName("utf-8")));
                String data = null;
                StringBuilder result = new StringBuilder();
                while (null != (data = reader.readLine())) {
                    result.append(data);
                }
                String resultStr = result.toString();
                try {
                    respData = createRespData(JSON.parse(resultStr));
                } catch (Exception e) {
                    //返回string:可能是html,xml...
                    respData = createRespData(200, RespData.RespType.STRING, resultStr);
                }
                result.delete(0, result.length());
                result = null;
                break;
            case 302:
                respData = createRespData(302, "编码302：请求定向失败。");
                break;
            case 400:
                respData = createRespData(400, "编码400：请求出现语法错误。");
                break;
            case 403:
                respData = createRespData(403, "编码403：资源不可用。服务器理解客户的请求，但拒绝处理它。通常由于服务器上文件或目录的权限设置导致。");
                break;
            case 404:
                respData = createRespData(404, "编码404：无法找到指定位置的资源。");
                break;
            case 405:
                respData = createRespData(405, "编码405：请求方法（GET、POST、HEAD、DELETE、PUT、TRACE等）对指定的资源不适用。");
                break;
            case 500:
                respData = createRespData(500, "编码500：服务器遇到了意料不到的情况，不能完成客户的请求。");
                break;
            default:
                respData = createRespData(code, "请求发生错误，错误编码：" + code);
                break;
        }
        return respData;
    }

    /**
     * 获取请求参数
     *
     * @return
     */
    public static ReqData createReq() {
        return createReq(null, null);
    }

    /**
     * 获取请求参数
     *
     * @param header 请求头
     * @return
     */
    public static ReqData createReq(Map<String, String> header) {
        return createReq(header, null);
    }

    /**
     * 获取请求参数
     *
     * @param header    请求头
     * @param parameter 请求参数
     * @return
     */
    public static ReqData createReq(Map<String, String> header, Map<String, Object> parameter) {
        return ReqData.create(header, parameter);
    }

    /**
     * @param data 返回的数据结果
     * @return
     */
    private static RespData createRespData(Object data) {
        return createRespData(200, data);
    }

    /**
     * @param code Http返回的状态
     * @param data 请求返回的数据结果
     * @return
     */
    private static RespData createRespData(int code, Object data) {
        return RespData.create(code, data);
    }

    private static RespData createRespData(int code, RespData.RespType respType, Object data) {
        return RespData.create(code, respType, data);
    }

    /**
     * 网络请求对象
     */

    @Data
    public static class ReqData {

        private ReqData() {
        }

        /**
         * 请求的url
         */
        private String url;
        /**
         * 请求的方式:默认为GET
         */
        private String method = ReqType.POST.get();
        /**
         * 数据请求类型:默认为application/x-www-form-urlencoded
         */
        private String contentType = "application/x-www-form-urlencoded";
        /**
         * 数据请求内容：json字符串
         */
        private String reqContent;
        /**
         * 超时时间设置(单位：毫秒)
         */
        private int timeOut;
        /**
         * 请求参数信息
         */
        private Map<String, Object> parameter;
        /**
         * 请求头信息
         */
        private Map<String, String> header;

        public ReqData addReqProperty(String key, String value) {
            if (null == this.header) {
                this.header = new HashMap<String, String>();
            }
            this.header.put(key, value);
            return this;
        }

        public ReqData addReqParameter(String key, Object value) {
            if (null == this.parameter) {
                this.parameter = new HashMap<String, Object>();
            }
            this.parameter.put(key, value);
            return this;
        }

        public static ReqData create() {
            ReqData reqData = new ReqData();
            return reqData;
        }

        public static ReqData create(Map<String, String> header) {
            ReqData reqData = create();
            if (null != header && !header.isEmpty()) {
                reqData.header = header;
            }
            return reqData;
        }

        public static ReqData create(Map<String, String> header, Map<String, Object> parameter) {
            ReqData reqData = create(header);
            if (null != parameter && !parameter.isEmpty()) {
                reqData.parameter = parameter;
            }
            return reqData;
        }

        public static enum ReqType {
            GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

            private String type;

            private ReqType() {
            }

            private ReqType(String type) {
                this.type = type;
            }

            public String get() {
                return this.type;
            }
        }
    }

    /**
     * 请求返回对象
     */
    @Data
    public static class RespData {
        /**
         * 请求返回的编码
         */
        private int code;
        /**
         * 返回类型：json,string...
         */
        private RespType type;
        /**
         * 请求返回的数据结果
         */
        private Object data;

        private RespData() {
        }

        public static RespData create() {
            return create(200, null);
        }

        public static RespData create(Object data) {
            return create(200, data);
        }

        public static RespData create(int code, Object data) {
            RespData respData = new RespData();
            respData.setCode(code);
            respData.setData(data);
            respData.setType(RespType.JSON);
            return respData;
        }

        public static RespData create(int code, RespType respType, Object data) {
            RespData respData = new RespData();
            respData.setCode(code);
            respData.setData(data);
            respData.setType(respType);
            return respData;
        }

        public static enum RespType {
            JSON, STRING;

            private String type;

            private RespType() {
            }

            private RespType(String type) {
                this.type = type;
            }

            public String get() {
                return this.type;
            }
        }
    }

    private static boolean isEmpty(String str) {
        return null == str || "".equals(str.trim()) || "null".equals(str.trim());
    }

    private static void close(Closeable stream) {
        try {
            if (null != stream) {
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stream = null;
    }


}

