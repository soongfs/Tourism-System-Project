import java.util.*;

// 测试程序

// 定义图的节点（地点）
class Node {
    String id;
    String name;
    Map<Node, Integer> neighbors; // 邻接节点及权重（距离/时间）

    public Node(String id, String name) {
        this.id = id;
        this.name = name;
        this.neighbors = new HashMap<>();
    }
}

// 图结构（景区/校园）
class Graph {
    Map<String, Node> nodes = new HashMap<>();

    // 添加节点
    public void addNode(String id, String name) {
        nodes.put(id, new Node(id, name));
    }

    // 添加边（双向）
    public void addEdge(String fromId, String toId, int weight) {
        Node from = nodes.get(fromId);
        Node to = nodes.get(toId);
        from.neighbors.put(to, weight);
        to.neighbors.put(from, weight); // 若为无向图
    }
}

// Dijkstra算法实现
class DijkstraAlgorithm {
    public static class PathResult {
        List<String> path;
        int totalDistance;

        public PathResult(List<String> path, int totalDistance) {
            this.path = path;
            this.totalDistance = totalDistance;
        }
    }

    public PathResult shortestPath(Graph graph, String startId, String endId) {
        Node start = graph.nodes.get(startId);
        Node end = graph.nodes.get(endId);

        // 优先队列：按距离排序（距离，当前节点，路径）
        PriorityQueue<Object[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> (int)a[0]));
        Map<Node, Integer> distances = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();

        // 初始化
        for (Node node : graph.nodes.values()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.offer(new Object[]{0, start, new ArrayList<>(List.of(start.id))});

        while (!pq.isEmpty()) {
            Object[] current = pq.poll();
            int currentDist = (int) current[0];
            Node currentNode = (Node) current[1];
            List<String> currentPath = (List<String>) current[2];

            if (currentNode == end) {
                return new PathResult(currentPath, currentDist);
            }

            for (Map.Entry<Node, Integer> neighbor : currentNode.neighbors.entrySet()) {
                Node nextNode = neighbor.getKey();
                int newDist = currentDist + neighbor.getValue();

                if (newDist < distances.get(nextNode)) {
                    distances.put(nextNode, newDist);
                    previous.put(nextNode, currentNode);
                    List<String> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextNode.id);
                    pq.offer(new Object[]{newDist, nextNode, newPath});
                }
            }
        }

        return null; // 无路径
    }
}

public class Main {
    public static void main(String[] args) {
        // 创建图并添加地点
        Graph campus = new Graph();
        campus.addNode("A", "图书馆");
        campus.addNode("B", "教学楼");
        campus.addNode("C", "食堂");
        campus.addNode("D", "宿舍");
        campus.addNode("E", "操场");
        campus.addNode("F", "体育馆");

        // 添加道路（边）
        campus.addEdge("A", "B", 6);
        campus.addEdge("B", "C", 100);
        campus.addEdge("A", "C", 10);
        campus.addEdge("C", "D", 8);
        campus.addEdge("A", "F", 8111);
        campus.addEdge("A", "E", 8);
        campus.addEdge("E", "F", 8);

        // 计算最短路径
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm();
        DijkstraAlgorithm.PathResult result = dijkstra.shortestPath(campus, "A", "F");

        // 输出结果
        if (result != null) {
            System.out.println("最短路径: " + String.join(" → ", result.path));
            System.out.println("总距离: " + result.totalDistance);
        } else {
            System.out.println("路径不存在");
        }
    }
}