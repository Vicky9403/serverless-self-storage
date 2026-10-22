package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelloHandlerTest {

    @Test
    public void testHandleRequest() {
        HelloHandler handler = new HelloHandler();
        APIGatewayProxyResponseEvent result = handler.handleRequest(null, null);

        assertEquals(200, result.getStatusCode());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        assertTrue(result.getBody().contains("Hello from CloudStore Java 21 Lambda!"));
    }
}
