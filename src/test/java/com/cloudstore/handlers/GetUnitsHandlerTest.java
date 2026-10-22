package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.StorageUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for GetUnitsHandler.
 */
public class GetUnitsHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testHandleRequest_ReturnsThreeUnits() throws Exception {
        GetUnitsHandler handler = new GetUnitsHandler();
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        
        // Call the handler
        APIGatewayProxyResponseEvent result = handler.handleRequest(input, null);

        // 1. Verify that the handler returns HTTP status 200
        assertEquals(200, result.getStatusCode(), "Status code should be 200");

        // 2. Verify that the response content type is application/json
        assertEquals("application/json", result.getHeaders().get("Content-Type"), "Content-Type should be application/json");

        // 3. Parse the JSON response and verify that it contains three storage units
        List<StorageUnit> units = objectMapper.readValue(result.getBody(), new TypeReference<List<StorageUnit>>() {});
        assertEquals(3, units.size(), "Should return exactly 3 storage units");

        // 4. Verify the sample units' IDs, sizes, prices, and availability
        
        // Unit 1
        StorageUnit unit1 = units.get(0);
        assertEquals("1", unit1.getId());
        assertEquals("Small", unit1.getSize());
        assertEquals(50.0, unit1.getPrice());
        assertTrue(unit1.isAvailable());

        // Unit 2
        StorageUnit unit2 = units.get(1);
        assertEquals("2", unit2.getId());
        assertEquals("Medium", unit2.getSize());
        assertEquals(100.0, unit2.getPrice());
        assertTrue(unit2.isAvailable());

        // Unit 3
        StorageUnit unit3 = units.get(2);
        assertEquals("3", unit3.getId());
        assertEquals("Large", unit3.getSize());
        assertEquals(150.0, unit3.getPrice());
        assertFalse(unit3.isAvailable());
    }
}
