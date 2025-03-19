import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;

public class GaodeToMySQL {

    // 配置区（必须修改！！）
    private static final String GAODE_API_KEY = "d6284fd52c3f92e8a92234b6a24ad2ce";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tourism_system?useSSL=false&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1917";
    private static final int MAX_RETRY = 3;  // 失败重试次数
    private static final int PAGE_SIZE = 25; // 高德API每页最大25条

    public static void main(String[] args) {
        String city = "北京市";
        String keywords = "景点";

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // 1. 获取总数据量并计算总页数
            int totalCount = getTotalCount(httpClient, city, keywords);
            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;

            // 2. 分页获取数据并插入数据库
            for (int page = 1; page <= totalPages; page++) {
                processPage(httpClient, conn, city, keywords, page);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getTotalCount(CloseableHttpClient httpClient, String city, String keywords) throws Exception {
        String url = String.format(
                "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s&offset=1&page=1&extensions=base",
                GAODE_API_KEY, keywords, city
        );

        JsonNode root = executeRequestWithRetry(httpClient, url);
        if (!"1".equals(root.get("status").asText())) {
            throw new RuntimeException("获取总数据量失败: " + root.get("infocode").asText());
        }
        return root.get("count").asInt();
    }

    private static void processPage(CloseableHttpClient httpClient, Connection conn,
                                    String city, String keywords, int page) {
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                String url = String.format(
                        "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s&offset=%d&page=%d&extensions=all",
                        GAODE_API_KEY, keywords, city, PAGE_SIZE, page
                );

                JsonNode root = executeRequestWithRetry(httpClient, url);
                JsonNode pois = root.get("pois");

                // 批量插入数据
                batchInsert(conn, pois);
                System.out.printf("第 %d 页数据插入完成，共 %d 条%n", page, pois.size());
                return;

            } catch (Exception e) {
                if (retry == MAX_RETRY - 1) {
                    System.err.printf("第 %d 页数据获取失败，已达最大重试次数%n", page);
                    e.printStackTrace();
                } else {
                    System.out.printf("第 %d 页数据获取失败，正在重试(%d/%d)...%n", page, retry+1, MAX_RETRY);
                }
            }
        }
    }

    private static JsonNode executeRequestWithRetry(CloseableHttpClient httpClient, String url) throws Exception {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                HttpGet request = new HttpGet(url);
                String jsonResponse = EntityUtils.toString(httpClient.execute(request).getEntity());
                JsonNode root = new ObjectMapper().readTree(jsonResponse);

                if ("1".equals(root.get("status").asText())) {
                    return root;
                }
                throw new RuntimeException("API错误: " + root.get("infocode").asText());

            } catch (Exception e) {
                if (i == MAX_RETRY - 1) throw e;
                Thread.sleep(2000); // 失败后等待2秒重试
            }
        }
        throw new IllegalStateException("无法到达此处");
    }

    private static void batchInsert(Connection conn, JsonNode pois) throws SQLException {
        String sql = "INSERT INTO attractions (name, address, longitude, latitude) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE address=VALUES(address)"; // 处理重复数据

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            for (JsonNode poi : pois) {
                String name = poi.get("name").asText();
                String address = poi.get("address").asText();
                String[] location = poi.get("location").asText().split(",");

                stmt.setString(1, name);
                stmt.setString(2, address);
                stmt.setDouble(3, Double.parseDouble(location[0]));
                stmt.setDouble(4, Double.parseDouble(location[1]));
                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}