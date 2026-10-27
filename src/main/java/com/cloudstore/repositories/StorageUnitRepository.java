package com.cloudstore.repositories;

import com.cloudstore.models.StorageUnit;
import java.util.List;

public interface StorageUnitRepository {
    List<StorageUnit> getAllUnits();
}
