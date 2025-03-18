import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GaodeAPIDemo {
    public static void main(String[] args) {
        String apiKey = "d6284fd52c3f92e8a92234b6a24ad2ce"; // 你的高德Key
        String city = "北京市";
        String keywords = "景点";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 构造请求URL
            String url = String.format(
                    "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s",
                    apiKey, keywords, city
            );

            // 发送HTTP GET请求
            HttpGet request = new HttpGet(url);
            String response = EntityUtils.toString(httpClient.execute(request).getEntity());

            // 解析JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode pois = root.get("pois");

            // 打印景点名称和地址
            for (JsonNode poi : pois) {
                String name = poi.get("name").asText();
                String address = poi.get("address").asText();
                System.out.println("名称：" + name + "，地址：" + address);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}