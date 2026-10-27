package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.StorageUnit;
import com.cloudstore.repositories.StorageUnitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for GetUnitsHandler using Mockito to mock Repository.
 */
@ExtendWith(MockitoExtension.class)
public class GetUnitsHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StorageUnitRepository mockRepository;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private GetUnitsHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GetUnitsHandler(mockRepository);
    }

    @Test
    public void testHandleRequest_ReturnsUnitsFromRepository() throws Exception {
        // Prepare mock data
        StorageUnit unit1 = new StorageUnit("1", "Unit A", "Small", 50.0, true);
        StorageUnit unit2 = new StorageUnit("2", "Unit B", "Medium", 100.0, true);
        List<StorageUnit> mockUnits = Arrays.asList(unit1, unit2);

        // Mock the repository
        when(mockRepository.getAllUnits()).thenReturn(mockUnits);

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        
        // Call the handler
        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        // 1. Verify that the handler returns HTTP status 200
        assertEquals(200, result.getStatusCode(), "Status code should be 200");

        // 2. Verify that the response content type is application/json
        assertEquals("application/json", result.getHeaders().get("Content-Type"), "Content-Type should be application/json");

        // 3. Parse the JSON response and verify that it contains our mock units
        List<StorageUnit> units = objectMapper.readValue(result.getBody(), new TypeReference<List<StorageUnit>>() {});
        assertEquals(2, units.size(), "Should return exactly 2 storage units");
        assertEquals("Unit A", units.get(0).getName());
        assertEquals("Unit B", units.get(1).getName());
    }

    @Test
    public void testHandleRequest_HandlesException() {
        // Simulate a database error
        when(mockRepository.getAllUnits()).thenThrow(new RuntimeException("DB Error"));
        when(mockContext.getLogger()).thenReturn(mockLogger);

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(500, result.getStatusCode());
        assertTrue(result.getBody().contains("error"));
        verify(mockLogger).log(contains("Error fetching units: DB Error"));
    }
}
