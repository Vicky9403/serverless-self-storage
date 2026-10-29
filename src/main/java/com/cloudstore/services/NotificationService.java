package com.cloudstore.services;

import com.cloudstore.models.Booking;

/**
 * Service interface for publishing notifications across the system.
 */
public interface NotificationService {

    /**
     * Publishes a message with a subject to the notification system.
     *
     * @param subject the notification subject
     * @param message the notification message body
     */
    void sendNotification(String subject, String message);

    /**
     * Publishes a notification when a booking is created.
     *
     * @param booking the newly created booking
     */
    default void notifyBookingCreated(Booking booking) {
        if (booking == null) return;
        String subject = "Booking Confirmed: " + booking.getId();
        String message = String.format("A new booking has been confirmed.\nBooking ID: %s\nUser ID: %s\nUnit ID: %s\nDate: %s\nStatus: %s",
                booking.getId(), booking.getUserId(), booking.getUnitId(), booking.getBookingDate(), booking.getStatus());
        sendNotification(subject, message);
    }

    /**
     * Publishes a notification when a booking is cancelled.
     *
     * @param booking the cancelled booking
     */
    default void notifyBookingCancelled(Booking booking) {
        if (booking == null) return;
        String subject = "Booking Cancelled: " + booking.getId();
        String message = String.format("A booking has been cancelled.\nBooking ID: %s\nUser ID: %s\nUnit ID: %s\nStatus: %s",
                booking.getId(), booking.getUserId(), booking.getUnitId(), booking.getStatus());
        sendNotification(subject, message);
    }
}
