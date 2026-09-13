package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComplaintRequest {
    private Long busId;
    @NotBlank
    private String category;
    @NotBlank
    @Size(max=100)
    private String subject;
    @NotBlank
    @Size(max=1000)
    private String description;

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
