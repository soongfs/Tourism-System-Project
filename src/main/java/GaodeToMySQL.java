import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;

public class GaodeToMySQL {

    // 配置区（必须修改！！）↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    private static final String GAODE_API_KEY = "d6284fd52c3f92e8a92234b6a24ad2ce";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tourism_system?useSSL=false";
    private static final String DB_USER = "root";      // 数据库用户名
    private static final String DB_PASSWORD = "1917";// 数据库密码
    // 配置区结束 ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑

    public static void main(String[] args) {
        String city = "北京市";
        String keywords = "景点";
        int pageSize = 50; // 每页最大50条
        int totalPages = 1; // 假设总共获取3页数据（实际应根据API返回的总数计算）

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // 1. 调用高德API获取数据
            for (int page = 1; page <= totalPages; page++) {
                String url = String.format(
                        "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s&offset=25",
                        GAODE_API_KEY, keywords, city, pageSize, page
                );
                String jsonResponse = EntityUtils.toString(httpClient.execute(new HttpGet(url)).getEntity());

                // 2. 解析JSON数据
                ObjectMapper mapper = new ObjectMapper();
                JsonNode pois = mapper.readTree(jsonResponse).get("pois");

                // 3. 批量插入数据库
                String sql = "INSERT INTO attractions (name, address, longitude, latitude) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    conn.setAutoCommit(false); // 开启事务，提升性能

                    for (JsonNode poi : pois) {
                        // 提取数据
                        String name = poi.get("name").asText();
                        String address = poi.get("address").asText();
                        String[] location = poi.get("location").asText().split(",");
                        double longitude = Double.parseDouble(location[0]);
                        double latitude = Double.parseDouble(location[1]);

                        // 设置参数
                        stmt.setString(1, name);
                        stmt.setString(2, address);
                        stmt.setDouble(3, longitude);
                        stmt.setDouble(4, latitude);
                        stmt.addBatch(); // 添加到批处理
                    }

                    int[] results = stmt.executeBatch(); // 执行批量插入
                    conn.commit(); // 提交事务
                    System.out.println("成功插入 " + results.length + " 条数据！");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}