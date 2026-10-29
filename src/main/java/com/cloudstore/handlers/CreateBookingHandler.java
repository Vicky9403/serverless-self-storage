package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.Booking;
import com.cloudstore.models.StorageUnit;
import com.cloudstore.repositories.BookingRepository;
import com.cloudstore.repositories.DynamoDbBookingRepository;
import com.cloudstore.repositories.DynamoDbStorageUnitRepository;
import com.cloudstore.repositories.StorageUnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CreateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BookingRepository bookingRepository;
    private final StorageUnitRepository storageUnitRepository;

    public CreateBookingHandler() {
        this.bookingRepository = new DynamoDbBookingRepository();
        this.storageUnitRepository = new DynamoDbStorageUnitRepository();
    }

    public CreateBookingHandler(BookingRepository bookingRepository, StorageUnitRepository storageUnitRepository) {
        this.bookingRepository = bookingRepository;
        this.storageUnitRepository = storageUnitRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            String userId = getUserId(input);
            if (userId == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Unauthorized\" }");
            }

            Map<String, String> bodyMap = objectMapper.readValue(input.getBody(), Map.class);
            String unitId = bodyMap.get("unitId");

            if (unitId == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"unitId is required\" }");
            }

            Optional<StorageUnit> unitOpt = storageUnitRepository.getById(unitId);
            if (unitOpt.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Storage unit not found\" }");
            }

            StorageUnit unit = unitOpt.get();
            if (!unit.isAvailable()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Storage unit is not available\" }");
            }

            // Create booking
            Booking booking = new Booking(
                    UUID.randomUUID().toString(),
                    userId,
                    unitId,
                    Instant.now().toString(),
                    "ACTIVE"
            );

            // Update unit availability
            unit.setAvailable(false);
            storageUnitRepository.update(unit);

            // Save booking
            bookingRepository.save(booking);

            String responseBody = objectMapper.writeValueAsString(booking);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withHeaders(headers)
                    .withBody(responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error creating booking: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not create booking\" }");
        }
    }

    private String getUserId(APIGatewayProxyRequestEvent input) {
        try {
            Map<String, Object> authorizer = input.getRequestContext().getAuthorizer();
            if (authorizer == null) return null;
            Map<String, String> claims = (Map<String, String>) authorizer.get("claims");
            if (claims == null) return null;
            return claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }
}
