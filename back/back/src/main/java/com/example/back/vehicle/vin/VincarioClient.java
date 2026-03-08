package com.example.back.vehicle.vin;

import com.example.back.vehicle.vin.dto.VincarioDecodeResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VincarioClient {

    private final RestClient restClient;
    private final VincarioConfig cfg;

    public VincarioClient(VincarioConfig cfg) {
        this.cfg = cfg;
        this.restClient = RestClient.builder()
                .baseUrl(cfg.baseUrl())
                .build();
    }

    public VincarioDecodeResponse decode(String vinUpper) {
        String controlSum = VincarioControlSum.forVinDecode(vinUpper, "decode", cfg.apiKey(), cfg.secretKey());
        String path = "/3.2/%s/%s/decode/%s.%s".formatted(cfg.apiKey(), controlSum, vinUpper, cfg.format());

        return restClient.get()
                .uri(path)
                .retrieve()
                .body(VincarioDecodeResponse.class);
    }
}