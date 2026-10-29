package com.cloudstore.services;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * AWS SNS implementation of NotificationService.
 */
public class SnsNotificationService implements NotificationService {

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsNotificationService() {
        this(createDefaultSnsClient(), System.getenv("BOOKING_TOPIC_ARN"));
    }

    public SnsNotificationService(SnsClient snsClient, String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    private static SnsClient createDefaultSnsClient() {
        String region = System.getenv("AWS_REGION");
        if (region != null && !region.isBlank()) {
            return SnsClient.builder().region(Region.of(region)).build();
        }
        return SnsClient.create();
    }

    @Override
    public void sendNotification(String subject, String message) {
        if (topicArn == null || topicArn.isBlank()) {
            return;
        }

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .build();

        snsClient.publish(request);
    }
}
