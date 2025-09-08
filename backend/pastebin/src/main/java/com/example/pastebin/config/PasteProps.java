package com.example.pastebin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "paste")
@Component
public class PasteProps {
    private long expirationDefaultMinutes = 60;
    private long notifyWindowMinutes = 15;
    private long schedulerFixedRateMs = 30000;
    private String corsAllowedOrigins = "http://localhost:5173";

    public long getExpirationDefaultMinutes() { return expirationDefaultMinutes; }
    public void setExpirationDefaultMinutes(long v) { this.expirationDefaultMinutes = v; }
    public long getNotifyWindowMinutes() { return notifyWindowMinutes; }
    public void setNotifyWindowMinutes(long v) { this.notifyWindowMinutes = v; }
    public long getSchedulerFixedRateMs() { return schedulerFixedRateMs; }
    public void setSchedulerFixedRateMs(long v) { this.schedulerFixedRateMs = v; }
    public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(String v) { this.corsAllowedOrigins = v; }
}
