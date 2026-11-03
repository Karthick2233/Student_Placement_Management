package com.college.placement.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.college.placement.entity.Offer;
import com.college.placement.enums.OfferStatus;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    boolean existsByApplicationId(Long applicationId);

    Page<Offer> findByStatus(OfferStatus status, Pageable pageable);

    Page<Offer> findByApplicationStudentUserEmail(String email, Pageable pageable);

    @Modifying
    @Query("update Offer o set o.status = :expired where o.status = :issued and o.expiryDate < :today")
    int expireOffers(@Param("expired") OfferStatus expired, @Param("issued") OfferStatus issued,
            @Param("today") LocalDate today);
}
