package com.cloudstore.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Represents a booking of a storage unit.
 */
@DynamoDbBean
public class Booking {
    private String id;
    private String userId;
    private String unitId;
    private String bookingDate;
    private String status; // e.g., ACTIVE, CANCELLED

    public Booking() {}

    public Booking(String id, String userId, String unitId, String bookingDate, String status) {
        this.id = id;
        this.userId = userId;
        this.unitId = unitId;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbSecondaryPartitionKey(indexNames = "UserIdIndex")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
