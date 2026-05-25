package com.zentraffic.traffic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roads")
public class Road {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String roadName;
    @Column(nullable = false)
    private String city;
    private int currentDensity;
    private double avgSpeed;
    @Column(nullable = false)
    private String status = "CLEAR";

    public Long getId() { return id; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getCurrentDensity() { return currentDensity; }
    public void setCurrentDensity(int currentDensity) { this.currentDensity = currentDensity; }
    public double getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(double avgSpeed) { this.avgSpeed = avgSpeed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
