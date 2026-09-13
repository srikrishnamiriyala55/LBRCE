package com.web.sms.dto.response;

import java.util.HashMap;
import java.util.Map;

public class DashboardResponse {
    private Map<String, Object> data = new HashMap<>();

    public DashboardResponse() {}

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public void put(String key, Object value) {
        data.put(key, value);
    }
}
