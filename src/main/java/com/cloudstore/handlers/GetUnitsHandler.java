package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.StorageUnit;
import com.cloudstore.repositories.DynamoDbStorageUnitRepository;
import com.cloudstore.repositories.StorageUnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda handler to list all available storage units.
 */
public class GetUnitsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StorageUnitRepository repository;

    /**
     * Default constructor used by AWS Lambda.
     */
    public GetUnitsHandler() {
        this.repository = new DynamoDbStorageUnitRepository();
    }

    /**
     * Constructor for testing.
     */
    public GetUnitsHandler(StorageUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            // Fetch data from Repository
            List<StorageUnit> units = repository.getAllUnits();

            String body = objectMapper.writeValueAsString(units);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(body);

        } catch (Exception e) {
            context.getLogger().log("Error fetching units: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not retrieve units\" }");
        }
    }
}
