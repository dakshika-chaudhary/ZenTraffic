package com.zentraffic.route.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentraffic.common.dto.RoadDto;
import com.zentraffic.common.dto.RouteRequest;
import com.zentraffic.common.dto.RouteResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
public class RouteOptimizer {
    private final CityGraph graph;
    private final RestClient restClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RouteOptimizer(CityGraph graph, RestClient.Builder restClientBuilder, StringRedisTemplate redis, ObjectMapper mapper) {
        this.graph = graph;
        this.restClient = restClientBuilder.baseUrl("http://TRAFFIC-SERVICE").build();
        this.redis = redis;
        this.mapper = mapper;
    }

    public RouteResponse calculate(RouteRequest request) {
        String strategy = normalizeStrategy(request.strategy());
        String key = "route:" + request.source() + ":" + request.destination() + ":" + strategy + ":" + request.emergencyVehicle();
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return mapper.readValue(cached, RouteResponse.class);
            } catch (JsonProcessingException ignored) {
            }
        }
        Map<String, RoadDto> traffic = trafficByRoadName();
        RouteResponse response = switch (strategy) {
            case "BFS" -> bfs(request.source(), request.destination(), traffic, strategy);
            case "ASTAR" -> weightedSearch(request.source(), request.destination(), traffic, strategy, request.emergencyVehicle(), true);
            default -> weightedSearch(request.source(), request.destination(), traffic, "DIJKSTRA", request.emergencyVehicle(), false);
        };
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofMinutes(5));
        } catch (JsonProcessingException ignored) {
        }
        return response;
    }

    public List<RouteResponse> alternatives(String source, String destination) {
        return List.of(
                calculate(new RouteRequest(source, destination, "DIJKSTRA", false)),
                calculate(new RouteRequest(source, destination, "ASTAR", false)),
                calculate(new RouteRequest(source, destination, "BFS", false))
        );
    }

    private RouteResponse weightedSearch(String source, String destination, Map<String, RoadDto> traffic,
                                         String strategy, boolean emergency, boolean astar) {
        validateNode(source);
        validateNode(destination);
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingDouble(node ->
                dist.getOrDefault(node, Double.MAX_VALUE) + (astar ? graph.heuristic(node, destination) : 0)));
        graph.nodes().forEach(node -> dist.put(node, Double.MAX_VALUE));
        dist.put(source, 0.0);
        queue.add(source);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(destination)) {
                break;
            }
            for (CityGraph.Edge edge : graph.edges(current)) {
                double congestion = congestionPenalty(edge.roadName(), traffic, emergency);
                double next = dist.get(current) + edge.distanceKm() + congestion;
                if (next < dist.get(edge.to())) {
                    dist.put(edge.to(), next);
                    parent.put(edge.to(), current);
                    queue.remove(edge.to());
                    queue.add(edge.to());
                }
            }
        }
        return response(source, destination, parent, traffic, strategy);
    }

    private RouteResponse bfs(String source, String destination, Map<String, RoadDto> traffic, String strategy) {
        validateNode(source);
        validateNode(destination);
        Queue<String> queue = new ArrayDeque<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(destination)) {
                break;
            }
            for (CityGraph.Edge edge : graph.edges(current)) {
                if (visited.add(edge.to())) {
                    parent.put(edge.to(), current);
                    queue.add(edge.to());
                }
            }
        }
        return response(source, destination, parent, traffic, strategy);
    }

    private RouteResponse response(String source, String destination, Map<String, String> parent,
                                   Map<String, RoadDto> traffic, String strategy) {
        List<String> path = reconstruct(source, destination, parent);
        double distance = 0;
        int congestion = 0;
        int edgeCount = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            CityGraph.Edge edge = graph.edges(from).stream().filter(e -> e.to().equals(to)).findFirst().orElseThrow();
            distance += edge.distanceKm();
            congestion += Optional.ofNullable(traffic.get(edge.roadName())).map(RoadDto::congestionScore).orElse(10);
            edgeCount++;
        }
        int score = edgeCount == 0 ? 0 : congestion / edgeCount;
        int minutes = (int) Math.ceil((distance / Math.max(12, 55 - score * 0.45)) * 60);
        int baseline = (int) Math.ceil((distance / 28) * 60);
        return new RouteResponse(path, Math.round(distance * 10.0) / 10.0, minutes, score, Math.max(0, baseline - minutes), strategy);
    }

    private List<String> reconstruct(String source, String destination, Map<String, String> parent) {
        if (!source.equals(destination) && !parent.containsKey(destination)) {
            throw new IllegalArgumentException("No route found");
        }
        LinkedList<String> path = new LinkedList<>();
        String cursor = destination;
        path.addFirst(cursor);
        while (!cursor.equals(source)) {
            cursor = parent.get(cursor);
            path.addFirst(cursor);
        }
        return path;
    }

    private double congestionPenalty(String roadName, Map<String, RoadDto> traffic, boolean emergency) {
        int score = Optional.ofNullable(traffic.get(roadName)).map(RoadDto::congestionScore).orElse(10);
        return emergency ? score / 18.0 : score / 10.0;
    }

    private Map<String, RoadDto> trafficByRoadName() {
        RoadDto[] roads = restClient.get().uri("/traffic/live").retrieve().body(RoadDto[].class);
        if (roads == null) {
            return Map.of();
        }
        return Arrays.stream(roads).collect(Collectors.toMap(RoadDto::roadName, road -> road, (a, b) -> a));
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return "DIJKSTRA";
        }
        return strategy.replace("*", "").replace(" ", "").toUpperCase();
    }

    private void validateNode(String node) {
        if (!graph.nodes().contains(node)) {
            throw new IllegalArgumentException("Unknown location: " + node);
        }
    }
}
