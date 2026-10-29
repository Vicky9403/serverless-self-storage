package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.models.Booking;
import com.cloudstore.models.StorageUnit;
import com.cloudstore.repositories.BookingRepository;
import com.cloudstore.repositories.StorageUnitRepository;
import com.cloudstore.services.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateBookingHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StorageUnitRepository storageUnitRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private CreateBookingHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new CreateBookingHandler(bookingRepository, storageUnitRepository, notificationService);
    }

    @Test
    public void testHandleRequest_Success() throws Exception {
        String userId = "user-123";
        String unitId = "unit-456";
        StorageUnit unit = new StorageUnit(unitId, "Unit A", "Small", 50.0, true);

        when(storageUnitRepository.getById(unitId)).thenReturn(Optional.of(unit));

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{\"unitId\":\"" + unitId + "\"}");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(201, result.getStatusCode());
        Booking booking = objectMapper.readValue(result.getBody(), Booking.class);
        assertEquals(userId, booking.getUserId());
        assertEquals(unitId, booking.getUnitId());
        assertEquals("ACTIVE", booking.getStatus());

        assertFalse(unit.isAvailable());
        verify(storageUnitRepository).update(unit);
        verify(bookingRepository).save(any(Booking.class));
        verify(notificationService).notifyBookingCreated(any(Booking.class));
    }

    @Test
    public void testHandleRequest_SuccessEvenWhenNotificationFails() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        String userId = "user-123";
        String unitId = "unit-456";
        StorageUnit unit = new StorageUnit(unitId, "Unit A", "Small", 50.0, true);

        when(storageUnitRepository.getById(unitId)).thenReturn(Optional.of(unit));
        doThrow(new RuntimeException("SNS failure")).when(notificationService).notifyBookingCreated(any(Booking.class));

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{\"unitId\":\"" + unitId + "\"}");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        // Booking creation must still succeed despite notification failure
        assertEquals(201, result.getStatusCode());
        verify(bookingRepository).save(any(Booking.class));
        verify(mockLogger).log(contains("Error sending booking creation notification"));
    }

    @Test
    public void testHandleRequest_UnitNotAvailable() throws Exception {
        String userId = "user-123";
        String unitId = "unit-456";
        StorageUnit unit = new StorageUnit(unitId, "Unit A", "Small", 50.0, false);

        when(storageUnitRepository.getById(unitId)).thenReturn(Optional.of(unit));

        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{\"unitId\":\"" + unitId + "\"}");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(authorizer);
        input.setRequestContext(context);

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(400, result.getStatusCode());
        assertTrue(result.getBody().contains("Storage unit is not available"));
        verifyNoInteractions(notificationService);
    }

    @Test
    public void testHandleRequest_Unauthorized() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{\"unitId\":\"unit-456\"}");

        APIGatewayProxyResponseEvent result = handler.handleRequest(input, mockContext);

        assertEquals(401, result.getStatusCode());
        verifyNoInteractions(notificationService);
    }
}
