package com.college.placement.entity;

import java.time.LocalDate;

import com.college.placement.enums.OfferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "offers", uniqueConstraints = @UniqueConstraint(columnNames = "application_id"))
public class Offer extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(nullable = false)
    private Double salary;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status = OfferStatus.ISSUED;

    protected Offer() {
    }

    public Offer(Application application, Double salary, LocalDate expiryDate) {
        this.application = application;
        this.salary = salary;
        this.expiryDate = expiryDate;
    }

    public Application getApplication() {
        return application;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void respond(boolean accept) {
        if (status != OfferStatus.ISSUED) {
            throw new IllegalStateException("Offer already responded to");
        }
        status = accept ? OfferStatus.ACCEPTED : OfferStatus.REJECTED;
    }
}
