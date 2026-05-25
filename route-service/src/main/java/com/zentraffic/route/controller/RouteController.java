package com.zentraffic.route.controller;

import com.zentraffic.common.dto.RouteRequest;
import com.zentraffic.common.dto.RouteResponse;
import com.zentraffic.route.service.CityGraph;
import com.zentraffic.route.service.RouteOptimizer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/route")
public class RouteController {
    private final RouteOptimizer routeOptimizer;
    private final CityGraph cityGraph;

    public RouteController(RouteOptimizer routeOptimizer, CityGraph cityGraph) {
        this.routeOptimizer = routeOptimizer;
        this.cityGraph = cityGraph;
    }

    @PostMapping("/calculate")
    public RouteResponse calculate(@Valid @RequestBody RouteRequest request) {
        return routeOptimizer.calculate(request);
    }

    @GetMapping("/alternatives")
    public List<RouteResponse> alternatives(@RequestParam String source, @RequestParam String destination) {
        return routeOptimizer.alternatives(source, destination);
    }

    @GetMapping("/locations")
    public Set<String> locations() {
        return cityGraph.nodes();
    }
}
