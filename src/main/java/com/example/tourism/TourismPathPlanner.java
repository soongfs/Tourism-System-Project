package com.example.tourism;

import java.sql.*;
import java.util.*;

public class TourismPathPlanner {

    // ================== 配置区（必须修改！！）==================
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tourism_system?useSSL=false&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1917";
    private static final String GAODE_API_KEY = "d6284fd52c3f92e8a92234b6a24ad2ce";
    // =======================================================

    // 定义节点类
    public static class Node {
        String id;
        String name;
        double lon;
        double lat;
        Map<Node, Double> neighbors = new HashMap<>();

        Node(String id, String name, double lon, double lat) {
            this.id = id;
            this.name = name;
            this.lon = lon;
            this.lat = lat;
        }
    }

    // 定义图结构
    public static class Graph {
        Map<String, Node> nodes = new HashMap<>();

        void addNode(Node node) {
            nodes.put(node.id, node);
        }

        // 添加边（使用Haversine计算实际距离）
        void addEdge(Node n1, Node n2) {
            double distance = haversine(n1.lat, n1.lon, n2.lat, n2.lon);
            n1.neighbors.put(n2, distance);
            n2.neighbors.put(n1, distance); // 无向图
        }

        // Haversine距离公式
        private double haversine(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371; // 地球半径（千米）
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return R * c;
        }
    }

    // 路径规划器（Dijkstra算法）
    public static class PathFinder {
        static class PathResult {
            double distance;
            List<Node> path;

            PathResult(double distance, List<Node> path) {
                this.distance = distance;
                this.path = path;
            }
        }

        public PathResult shortestPath(Graph graph, Node start, Node end) {
            Map<Node, Double> distances = new HashMap<>();
            Map<Node, Node> predecessors = new HashMap<>();
            PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

            // 初始化
            for (Node node : graph.nodes.values()) {
                distances.put(node, Double.MAX_VALUE);
            }
            distances.put(start, 0.0);
            queue.add(start);

            // Dijkstra算法
            while (!queue.isEmpty()) {
                Node current = queue.poll();
                if (current == end) break;

                for (Map.Entry<Node, Double> entry : current.neighbors.entrySet()) {
                    Node neighbor = entry.getKey();
                    double newDist = distances.get(current) + entry.getValue();
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        predecessors.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }

            // 重建路径
            List<Node> path = new ArrayList<>();
            for (Node at = end; at != null; at = predecessors.get(at)) {
                path.add(at);
            }
            Collections.reverse(path);
            return new PathResult(distances.get(end), path);
        }
    }

    public static void main(String[] args) {
        try {
            // 1. 从数据库加载景点数据
            Graph graph = loadGraphFromDB();

            // 2. 查找起点和终点
            Node start = findNodeByName(graph, "南锣鼓巷");
            Node end = findNodeByName(graph, "中山公园");

            // 3. 计算最短路径
            PathFinder.PathResult result = new PathFinder().shortestPath(graph, start, end);

            // 4. 输出结果
            System.out.printf("从【%s】到【%s】的最短距离：%.2f公里\n路径：",
                    start.name, end.name, result.distance);
            for (Node node : result.path) {
                System.out.print(node.name + " → ");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 从数据库加载数据并建图
    public static Graph loadGraphFromDB() throws SQLException {
        Graph graph = new Graph();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // 加载所有景点
            ResultSet rs = stmt.executeQuery("SELECT id, name, longitude, latitude FROM attractions");
            List<Node> nodes = new ArrayList<>();
            while (rs.next()) {
                Node node = new Node(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("longitude"),
                        rs.getDouble("latitude")
                );
                nodes.add(node);
                graph.addNode(node);
            }

            // 为每个节点连接最近的5个邻居
            for (Node node : nodes) {
                nodes.stream()
                        .filter(n -> !n.id.equals(node.id))
                        .sorted(Comparator.comparingDouble(n ->
                                graph.haversine(node.lat, node.lon, n.lat, n.lon)))
                        .limit(5)
                        .forEach(neighbor -> graph.addEdge(node, neighbor));
            }
        }
        return graph;
    }

    // 通过名称查找节点
    public static Node findNodeByName(Graph graph, String name) {
        return graph.nodes.values().stream()
                .filter(node -> node.name.contains(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到景点：" + name));
    }
}