package com.cloudstore.services;

import com.cloudstore.models.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class SnsNotificationServiceTest {

    private static final String TOPIC_ARN = "arn:aws:sns:af-south-1:123456789012:test-booking-topic";

    @Mock
    private SnsClient mockSnsClient;

    private SnsNotificationService service;

    @BeforeEach
    public void setUp() {
        service = new SnsNotificationService(mockSnsClient, TOPIC_ARN);
    }

    @Test
    public void testSendNotification() {
        String subject = "Test Subject";
        String message = "Hello from CloudStore";

        service.sendNotification(subject, message);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(mockSnsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();

        assertEquals(TOPIC_ARN, request.topicArn());
        assertEquals(subject, request.subject());
        assertEquals(message, request.message());
    }

    @Test
    public void testNotifyBookingCreated() {
        Booking booking = new Booking("b-123", "u-456", "unit-789", "2026-10-29T12:00:00Z", "ACTIVE");

        service.notifyBookingCreated(booking);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(mockSnsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();

        assertEquals(TOPIC_ARN, request.topicArn());
        assertTrue(request.subject().contains("b-123"));
        assertTrue(request.message().contains("b-123"));
        assertTrue(request.message().contains("u-456"));
        assertTrue(request.message().contains("unit-789"));
        assertTrue(request.message().contains("ACTIVE"));
    }

    @Test
    public void testNotifyBookingCancelled() {
        Booking booking = new Booking("b-123", "u-456", "unit-789", "2026-10-29T12:00:00Z", "CANCELLED");

        service.notifyBookingCancelled(booking);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(mockSnsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();

        assertEquals(TOPIC_ARN, request.topicArn());
        assertTrue(request.subject().contains("b-123"));
        assertTrue(request.message().contains("b-123"));
        assertTrue(request.message().contains("CANCELLED"));
    }

    @Test
    public void testSendNotification_WhenTopicArnIsBlank_DoesNotPublish() {
        SnsNotificationService unconfiguredService = new SnsNotificationService(mockSnsClient, "");

        unconfiguredService.sendNotification("Subject", "Message");

        verifyNoInteractions(mockSnsClient);
    }
}
