package com.cloudstore.repositories;

import com.cloudstore.models.StorageUnit;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.stream.Collectors;

public class DynamoDbStorageUnitRepository implements StorageUnitRepository {

    private final DynamoDbTable<StorageUnit> storageUnitTable;

    public DynamoDbStorageUnitRepository() {
        DynamoDbClient ddbClient = DynamoDbClient.create();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        String tableName = System.getenv("TABLE_NAME");
        this.storageUnitTable = enhancedClient.table(tableName, TableSchema.fromBean(StorageUnit.class));
    }

    public DynamoDbStorageUnitRepository(DynamoDbTable<StorageUnit> storageUnitTable) {
        this.storageUnitTable = storageUnitTable;
    }

    @Override
    public List<StorageUnit> getAllUnits() {
        return storageUnitTable.scan().items().stream()
                .collect(Collectors.toList());
    }
}
