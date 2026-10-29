package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.Booking;
import com.cloudstore.models.StorageUnit;
import com.cloudstore.repositories.BookingRepository;
import com.cloudstore.repositories.StorageUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CancelBookingHandlerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StorageUnitRepository storageUnitRepository;

    @Mock
    private Context mockContext;

    private CancelBookingHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new CancelBookingHandler(bookingRepository, storageUnitRepository);
    }

    @Test
    public void testHandleRequest_Success() {
        String userId = "user-123";
        String bookingId = "booking-456";
        String unitId = "unit-789";
        Booking booking = new Booking(bookingId, userId, unitId, "date", "ACTIVE");
        StorageUnit unit = new StorageUnit(unitId, "Unit A", "Small", 50.0, false);

        when(bookingRepository.getById(bookingId)).thenReturn(Optional.of(booking));
        when(storageUnitRepository.getById(unitId)).thenReturn(Optional.of(unit));

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setPathParameters(Map.of("id", bookingId));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(200, result.getStatusCode());
        assertEquals("CANCELLED", booking.getStatus());
        assertTrue(unit.isAvailable());
        verify(bookingRepository).update(booking);
        verify(storageUnitRepository).update(unit);
    }

    @Test
    public void testHandleRequest_Forbidden() {
        String userId = "user-123";
        String otherUserId = "user-456";
        String bookingId = "booking-456";
        Booking booking = new Booking(bookingId, otherUserId, "unit-789", "date", "ACTIVE");

        when(bookingRepository.getById(bookingId)).thenReturn(Optional.of(booking));

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setPathParameters(Map.of("id", bookingId));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(403, result.getStatusCode());
    }
}
