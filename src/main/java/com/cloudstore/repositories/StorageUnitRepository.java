package com.cloudstore.repositories;

import com.cloudstore.models.StorageUnit;
import java.util.List;
import java.util.Optional;

public interface StorageUnitRepository {
    List<StorageUnit> getAllUnits();
    Optional<StorageUnit> getById(String id);
    void update(StorageUnit unit);
}
