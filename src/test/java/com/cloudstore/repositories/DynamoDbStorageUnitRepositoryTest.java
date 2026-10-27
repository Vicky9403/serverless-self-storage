package com.cloudstore.repositories;

import com.cloudstore.models.StorageUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynamoDbStorageUnitRepositoryTest {

    @Mock
    private DynamoDbTable<StorageUnit> mockTable;

    @Mock
    private PageIterable<StorageUnit> mockPageIterable;

    @Mock
    private SdkIterable<StorageUnit> mockSdkIterable;

    private DynamoDbStorageUnitRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new DynamoDbStorageUnitRepository(mockTable);
    }

    @Test
    public void testGetAllUnits_ReturnsUnitsFromDynamoDB() {
        // Prepare mock data
        StorageUnit unit1 = new StorageUnit("1", "Unit A", "Small", 50.0, true);
        StorageUnit unit2 = new StorageUnit("2", "Unit B", "Medium", 100.0, true);
        List<StorageUnit> mockUnits = Arrays.asList(unit1, unit2);

        // Mock the DynamoDB scan chain: table.scan().items().stream()
        when(mockTable.scan()).thenReturn(mockPageIterable);
        when(mockPageIterable.items()).thenReturn(mockSdkIterable);
        when(mockSdkIterable.stream()).thenReturn(mockUnits.stream());

        // Call the repository
        List<StorageUnit> units = repository.getAllUnits();

        // Verify results
        assertEquals(2, units.size(), "Should return exactly 2 storage units");
        assertEquals("Unit A", units.get(0).getName());
        assertEquals("Unit B", units.get(1).getName());
    }
}
