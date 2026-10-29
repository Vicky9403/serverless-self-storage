package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.Booking;
import com.cloudstore.repositories.BookingRepository;
import com.cloudstore.repositories.DynamoDbBookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetBookingsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BookingRepository repository;

    public GetBookingsHandler() {
        this.repository = new DynamoDbBookingRepository();
    }

    public GetBookingsHandler(BookingRepository repository) {
        this.repository = repository;
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

            List<Booking> bookings = repository.getByUserId(userId);
            String body = objectMapper.writeValueAsString(bookings);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(body);

        } catch (Exception e) {
            context.getLogger().log("Error fetching bookings: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not retrieve bookings\" }");
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
