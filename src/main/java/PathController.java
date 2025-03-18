import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class PathController {

    @GetMapping("/api/path")
    public Map<String, Object> getPath(
            @RequestParam String startName,
            @RequestParam String endName) {

        Map<String, Object> response = new HashMap<>();
        try {
            // 1. 加载图数据
            TourismPathPlanner.Graph graph = TourismPathPlanner.loadGraphFromDB();

            // 2. 查找节点
            TourismPathPlanner.Node start = TourismPathPlanner.findNodeByName(graph, startName);
            TourismPathPlanner.Node end = TourismPathPlanner.findNodeByName(graph, endName);

            // 3. 计算路径
            TourismPathPlanner.PathFinder.PathResult result =
                    new TourismPathPlanner.PathFinder().shortestPath(graph, start, end);

            // 4. 构建响应
            response.put("distance", result.distance);
            response.put("path", result.path.stream().map(node -> {
                Map<String, Object> nodeInfo = new HashMap<>();
                nodeInfo.put("name", node.name);
                nodeInfo.put("lat", node.lat);
                nodeInfo.put("lon", node.lon);
                return nodeInfo;
            }).toList());

        } catch (SQLException e) {
            response.put("error", "数据库连接失败：" + e.getMessage());
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
        }
        return response;
    }
}