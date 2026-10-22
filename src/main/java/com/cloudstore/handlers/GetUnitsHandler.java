package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.StorageUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda handler to list all available storage units.
 */
public class GetUnitsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            // Hardcoded data for Step 1
            List<StorageUnit> units = new ArrayList<>();
            units.add(new StorageUnit("1", "Unit A", "Small", 50.0, true));
            units.add(new StorageUnit("2", "Unit B", "Medium", 100.0, true));
            units.add(new StorageUnit("3", "Unit C", "Large", 150.0, false));

            String body = objectMapper.writeValueAsString(units);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(body);

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not retrieve units\" }");
        }
    }
}
