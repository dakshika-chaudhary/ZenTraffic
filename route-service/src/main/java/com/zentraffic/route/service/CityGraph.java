package com.zentraffic.route.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CityGraph {
    private final Map<String, List<Edge>> graph = new HashMap<>();
    private final Map<String, double[]> coordinates = new HashMap<>();

    public CityGraph() {
        addNode("Sector 18", 28.5708, 77.3261);
        addNode("Botanical Garden", 28.5640, 77.3343);
        addNode("DND Flyway", 28.5946, 77.2957);
        addNode("Akshardham", 28.6127, 77.2773);
        addNode("Kalindi Kunj", 28.5452, 77.3108);
        addNode("Greater Noida", 28.4744, 77.5040);
        connect("Sector 18", "Botanical Garden", 3.0, "Sector 18 Main Road");
        connect("Botanical Garden", "Kalindi Kunj", 4.5, "Botanical Garden Road");
        connect("Sector 18", "DND Flyway", 8.0, "DND Flyway");
        connect("DND Flyway", "Akshardham", 7.0, "Akshardham Road");
        connect("Kalindi Kunj", "Akshardham", 10.0, "Kalindi Kunj Road");
        connect("Botanical Garden", "Greater Noida", 22.0, "Noida-Greater Noida Expressway");
        connect("Sector 18", "Greater Noida", 26.0, "Noida-Greater Noida Expressway");
    }

    public Set<String> nodes() {
        return graph.keySet();
    }

    public List<Edge> edges(String node) {
        return graph.getOrDefault(node, List.of());
    }

    public double heuristic(String a, String b) {
        double[] x = coordinates.getOrDefault(a, new double[]{0, 0});
        double[] y = coordinates.getOrDefault(b, new double[]{0, 0});
        return Math.hypot(x[0] - y[0], x[1] - y[1]) * 111;
    }

    private void addNode(String node, double lat, double lon) {
        graph.putIfAbsent(node, new ArrayList<>());
        coordinates.put(node, new double[]{lat, lon});
    }

    private void connect(String from, String to, double distanceKm, String roadName) {
        graph.get(from).add(new Edge(to, distanceKm, roadName));
        graph.get(to).add(new Edge(from, distanceKm, roadName));
    }

    public record Edge(String to, double distanceKm, String roadName) {
    }
}
