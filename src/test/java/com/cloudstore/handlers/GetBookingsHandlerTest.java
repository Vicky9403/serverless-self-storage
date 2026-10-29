package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.Booking;
import com.cloudstore.repositories.BookingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBookingsHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private Context mockContext;

    private GetBookingsHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GetBookingsHandler(bookingRepository);
    }

    @Test
    public void testHandleRequest_Success() throws Exception {
        String userId = "user-123";
        Booking b1 = new Booking("b1", userId, "u1", "2023-10-27T10:00:00Z", "ACTIVE");
        Booking b2 = new Booking("b2", userId, "u2", "2023-10-27T11:00:00Z", "CANCELLED");
        List<Booking> mockBookings = Arrays.asList(b1, b2);

        when(bookingRepository.getByUserId(userId)).thenReturn(mockBookings);

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(200, result.getStatusCode());
        List<Booking> bookings = objectMapper.readValue(result.getBody(), new TypeReference<List<Booking>>() {});
        assertEquals(2, bookings.size());
        assertEquals("b1", bookings.get(0).getId());
        assertEquals("b2", bookings.get(1).getId());
    }

    @Test
    public void testHandleRequest_Unauthorized() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(401, result.getStatusCode());
    }
}
