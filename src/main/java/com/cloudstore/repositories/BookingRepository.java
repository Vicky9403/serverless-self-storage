package com.cloudstore.repositories;

import com.cloudstore.models.Booking;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    void save(Booking booking);
    Optional<Booking> getById(String id);
    List<Booking> getByUserId(String userId);
    void update(Booking booking);
}
