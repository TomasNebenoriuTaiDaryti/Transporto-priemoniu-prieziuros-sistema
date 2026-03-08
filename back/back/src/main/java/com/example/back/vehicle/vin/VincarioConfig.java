package com.example.back.vehicle.vin;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vincario")
public record VincarioConfig(
        String baseUrl,
        String apiKey,
        String secretKey,
        String format
) {}