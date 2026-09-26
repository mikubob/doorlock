package com.hnkjzyxy.ab.utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.util.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;


/**
 * 从OA系统调用接口获取数据的工具类
 */
public class OaRequestAPIUtils {

    /**
     * 获取 token 的 URL
     */
    private static final String tokenURL = "https://dmp.hnkjxy.net.cn/open_api/authentication/get_access_token";

    /**
     * 获取学生请假数据（当天数据）的 URL
     */
    private static final String hotelURL = "https://dmp.hnkjxy.net.cn/open_api/customization/view_india/full?access_token=";

    /**
     * 获取电子班牌所需数据的 URL
     */
    private static final String classBoardURL ="https://dmp.hnkjxy.net.cn/open_api/customization/view_mike/full?access_token=";

    /**
     * 获取token
     * @return String
     */
    public static String getAccessToken() {
        String accessToken = "";
        try {
            //首先需要获取token，是根据key和secret生成的
            URL targetUrl = new URL(tokenURL);
            //创建一个POST请求，调用上面的API URL信息
            HttpURLConnection httpConnection = (HttpURLConnection) targetUrl.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            //TODO 构建一个json对象，key,secret的值在应用查看中获取(OA系统申请的)
            String input = "{\"key\":\"20250622398919366238010517831682779\",\"secret\":\"e6c226b86065d7a8df88a5e3119ba6d0491b791a\"}";
            //将请求返回的结果写入流中
            OutputStream outputStream = httpConnection.getOutputStream();
            outputStream.write(input.getBytes());
            outputStream.flush();
            if (httpConnection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " +
                        httpConnection.getResponseCode());
            }
            //读取流信息，答应出返回的结果
            BufferedReader responseBuffer = new BufferedReader(new InputStreamReader(
                    (httpConnection.getInputStream()), "UTF-8"));
            String output;
            System.out.println("Output from Server:\n");
            while ((output = responseBuffer.readLine()) != null) {
                // 将字符串解析为JSONObject
                JSONObject dataObj = new JSONObject(output);
                // 获取access_token
                accessToken = dataObj.getJSONObject("result").getString("access_token");
                System.out.println(accessToken);
            }
            //断开HTTP连接
            httpConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    /**
     * 班级名称，要与OA系统中保持一致
     * @param className 班级
     * @return int 请假人数
     */
    public static int queryHotelDataByToday(String className) {
        int result = 0;
        try {
            String accessToken = getAccessToken();
            URL targetUrl = new URL(hotelURL + accessToken);
            //创建一个POST请求，调用上面的API URL信息
            HttpURLConnection httpConnection = (HttpURLConnection) targetUrl.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            //将请求返回的结果写入流中
            OutputStream outputStream = httpConnection.getOutputStream();
            outputStream.flush();
            if (httpConnection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " +
                        httpConnection.getResponseCode());
            }
            //读取流信息，答应出返回的结果
            BufferedReader responseBuffer = new BufferedReader(new InputStreamReader(
                    (httpConnection.getInputStream()), "UTF-8"));
            String output;
            System.out.println("Output from Server:\n");
            while ((output = responseBuffer.readLine()) != null) {
                JSONObject outputObj = new JSONObject(output);
                JSONObject resultObj = outputObj.getJSONObject("result");
                JSONArray dataArray = resultObj.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    if (className.contains(dataArray.getJSONObject(i).get("BJMC").toString()))  //检查班级名称是否匹配上
                    {
                        result++;
                    }
                }
            }
            //断开HTTP连接
            httpConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 使用 HttpURLConnection 调用OA接口,获取返回结果中的 result.data 数据。
     * @param xq 校区
     * @param jzwmc 教学楼
     * @return String
     * 返回字段说明：SKRQ	上课日期， XQ	校区，JZWMC	教学楼，SKDD	上课地点，JSH	教室号
     * JGH	授课教师工号，JSXM	授课教师姓名，SZDWMC	授课教师部门，KCMC	课程名称，
     * KKXND	学年，KKXQM	学期，ZC	周次，XQJ	星期几，SKJC	上课节次
     * BJMC	上课班级名称，FDYXM	辅导员姓名，JXBRS	教学班人数，QJRS	请假人数，SFYQJRS	是否有请假人数
     */
    public static String getClassBoardData(String xq,String jzwmc){
        HttpURLConnection httpConnection = null;
        String dataJson = null;
        try {
            // 1. 构造请求参数（所有参数均为非必填）
            Map<String, String> requestParams = new HashMap<>();
            if(StringUtils.isNotEmpty(xq) && StringUtils.isNotEmpty(jzwmc)){
                requestParams.put("XQ", xq);  //校区
                requestParams.put("JZWMC", jzwmc); //教学楼
            }
            // 2. 将参数转换为 JSON 字符串
            String requestBody = objectMapper.writeValueAsString(requestParams);
            // 3. 创建 URL 对象（如需 accessToken 可拼接）
            String accessToken = getAccessToken();
            //System.out.println("token"+ accessToken);
            URL targetUrl = new URL(classBoardURL + accessToken + "&per_page=1000");  //获取最大记录数为1000
            //创建一个POST请求，调用上面的API URL信息
            httpConnection = (HttpURLConnection) targetUrl.openConnection();
            // 4. 配置连接属性
            httpConnection.setDoOutput(true);      // 允许输出
            httpConnection.setDoInput(true);       // 允许输入
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(30000);
            // 5. 写入请求体
            try (OutputStream outputStream = httpConnection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
                outputStream.flush();
            }
            // 6. 获取响应码
            int responseCode = httpConnection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP 请求失败，错误码：" + responseCode);
            }
            // 7. 读取响应内容
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(httpConnection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            // 8. 解析 JSON 响应，提取 result.data
            JsonNode rootNode = objectMapper.readTree(response.toString());
            int code = rootNode.path("code").asInt();
            String message = rootNode.path("message").asText();
            if (code == 10000) {
                JsonNode dataNode = rootNode.path("result").path("data");
               int count =dataNode.size();
               System.out.println("获取到的总记录数:" + count);
                if (dataNode.isMissingNode()) {
                    System.out.println("响应中未找到 result.data 字段");
                } else {
                    dataJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataNode);
                    System.out.println("成功获取 result.data 中的 JSON 数据：");
                    System.out.println(dataJson);
                }
            } else {
                System.err.println("业务错误，code=" + code + ", message=" + message);
            }
        } catch (Exception e) {
            System.err.println("调用接口或解析数据时发生异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return dataJson;
    }

    /**
     * 测试代码
     *
     * @param args
     */
    public static void main(String[] args) {
        //int i = queryHotelDataByToday("药产2501班");
        //System.out.println("药产2501班的请假人数：");
        //System.out.println(i);
        String dataJson = getClassBoardData("","");
        System.out.println(dataJson);
    }
}
