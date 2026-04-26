package com.example.back.Unit.vehicle.vin;

import com.example.back.vehicle.vin.VincarioClient;
import com.example.back.vehicle.vin.VincarioConfig;
import com.example.back.vehicle.vin.VincarioControlSum;
import com.example.back.vehicle.vin.dto.VincarioDecodeResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class VincarioClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("VIN dekodavimas kviečia Vincario API su teisingu keliu")
    void decodeCallsVincarioApiWithCorrectPath() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());

            String body = """
                    {
                      "decode": [
                        { "label": "Make", "value": "Volkswagen" },
                        { "label": "Model", "value": "Golf" },
                        { "label": "Model Year", "value": 2000 }
                      ]
                    }
                    """;

            byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        VincarioConfig config = new VincarioConfig(baseUrl, "api-key", "secret-key", "json");
        VincarioClient client = new VincarioClient(config);

        VincarioDecodeResponse result = client.decode("WVWZZZ1JZXW000001");
        String expectedControlSum = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");

        assertThat(requestedPath.get()).isEqualTo("/3.2/api-key/" + expectedControlSum + "/decode/WVWZZZ1JZXW000001.json");
        assertThat(result).isNotNull();
        assertThat(result.make()).isEqualTo("Volkswagen");
        assertThat(result.model()).isEqualTo("Golf");
        assertThat(result.modelYear()).isEqualTo(2000);
    }
}