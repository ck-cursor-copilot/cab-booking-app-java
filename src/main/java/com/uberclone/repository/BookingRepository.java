package com.uberclone.repository;

import com.uberclone.model.Booking;
import com.uberclone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status")
    List<Booking> findAllByStatus(@Param("status") String status);
    
    @Query("SELECT b FROM Booking b WHERE b.pickup LIKE %:location% OR b.drop LIKE %:location%")
    List<Booking> findByLocationContaining(@Param("location") String location);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findByDateRange(@Param("startDate") java.time.LocalDateTime startDate, 
                                 @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.user.email = :email AND b.status = :status")
    List<Booking> findByUserEmailAndStatus(@Param("email") String email, 
                                          @Param("status") String status);
} 