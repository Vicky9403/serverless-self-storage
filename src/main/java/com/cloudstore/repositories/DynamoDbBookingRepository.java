package com.cloudstore.repositories;

import com.cloudstore.models.Booking;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DynamoDbBookingRepository implements BookingRepository {

    private final DynamoDbTable<Booking> bookingTable;

    public DynamoDbBookingRepository() {
        DynamoDbClient ddbClient = DynamoDbClient.create();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        String tableName = System.getenv("BOOKINGS_TABLE_NAME");
        this.bookingTable = enhancedClient.table(tableName, TableSchema.fromBean(Booking.class));
    }

    public DynamoDbBookingRepository(DynamoDbTable<Booking> bookingTable) {
        this.bookingTable = bookingTable;
    }

    @Override
    public void save(Booking booking) {
        bookingTable.putItem(booking);
    }

    @Override
    public Optional<Booking> getById(String id) {
        return Optional.ofNullable(bookingTable.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public List<Booking> getByUserId(String userId) {
        DynamoDbIndex<Booking> userIdIndex = bookingTable.index("UserIdIndex");
        return userIdIndex.query(QueryConditional.keyEqualTo(k -> k.partitionValue(userId)))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void update(Booking booking) {
        bookingTable.updateItem(booking);
    }
}
