package com.smartambulance.routing;

public class GraphNode {
    private final String id;
    private final double latitude;
    private final double longitude;

    public GraphNode(String id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
