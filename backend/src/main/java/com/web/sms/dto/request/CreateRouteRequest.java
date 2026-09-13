package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateRouteRequest {
    @NotBlank
    private String routeName;
    @NotBlank
    private String startingPoint;
    @NotBlank
    private String endingPoint;
    private String description;

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getStartingPoint() { return startingPoint; }
    public void setStartingPoint(String startingPoint) { this.startingPoint = startingPoint; }
    public String getEndingPoint() { return endingPoint; }
    public void setEndingPoint(String endingPoint) { this.endingPoint = endingPoint; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
