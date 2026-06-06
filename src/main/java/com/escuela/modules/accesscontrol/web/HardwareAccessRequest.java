package com.escuela.modules.accesscontrol.web;

import com.escuela.modules.accesscontrol.domain.AccessLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class HardwareAccessRequest {
    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("userId")
    private Long userId;

    private AccessLog.Direction direction;
    private LocalDateTime timestamp;

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    
    @JsonProperty("deviceID")
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public Long getUserId() { return userId; }
    
    @JsonProperty("userID")
    public void setUserId(Long userId) { this.userId = userId; }
    
    public AccessLog.Direction getDirection() { return direction; }
    public void setDirection(AccessLog.Direction direction) { this.direction = direction; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
