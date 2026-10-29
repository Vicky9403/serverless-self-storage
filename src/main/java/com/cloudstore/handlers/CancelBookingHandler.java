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
import com.cloudstore.services.NotificationService;
import com.cloudstore.services.SnsNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CancelBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BookingRepository bookingRepository;
    private final StorageUnitRepository storageUnitRepository;
    private final NotificationService notificationService;

    public CancelBookingHandler() {
        this(new DynamoDbBookingRepository(), new DynamoDbStorageUnitRepository(), new SnsNotificationService());
    }

    public CancelBookingHandler(BookingRepository bookingRepository, StorageUnitRepository storageUnitRepository) {
        this(bookingRepository, storageUnitRepository, new SnsNotificationService());
    }

    public CancelBookingHandler(BookingRepository bookingRepository, StorageUnitRepository storageUnitRepository, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.storageUnitRepository = storageUnitRepository;
        this.notificationService = notificationService;
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

            String bookingId = input.getPathParameters().get("id");
            if (bookingId == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Booking ID is required\" }");
            }

            Optional<Booking> bookingOpt = bookingRepository.getById(bookingId);
            if (bookingOpt.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Booking not found\" }");
            }

            Booking booking = bookingOpt.get();
            if (!booking.getUserId().equals(userId)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(403)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Forbidden\" }");
            }

            if ("CANCELLED".equals(booking.getStatus())) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Booking is already cancelled\" }");
            }

            // Cancel booking
            booking.setStatus("CANCELLED");
            bookingRepository.update(booking);

            // Make unit available again
            Optional<StorageUnit> unitOpt = storageUnitRepository.getById(booking.getUnitId());
            if (unitOpt.isPresent()) {
                StorageUnit unit = unitOpt.get();
                unit.setAvailable(true);
                storageUnitRepository.update(unit);
            }

            // Send notification
            try {
                if (notificationService != null) {
                    notificationService.notifyBookingCancelled(booking);
                }
            } catch (Exception e) {
                if (context != null && context.getLogger() != null) {
                    context.getLogger().log("Error sending booking cancellation notification: " + e.getMessage());
                }
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody("{ \"message\": \"Booking cancelled successfully\" }");

        } catch (Exception e) {
            context.getLogger().log("Error cancelling booking: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not cancel booking\" }");
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
