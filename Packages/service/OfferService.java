package com.college.placement.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.dto.ApplicationDto.OfferRequest;
import com.college.placement.dto.ApplicationDto.OfferResponse;
import com.college.placement.dto.ApplicationDto.OfferUpdateRequest;
import com.college.placement.entity.Application;
import com.college.placement.entity.Offer;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.InterviewResult;
import com.college.placement.enums.OfferStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.OfferMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.InterviewRepository;
import com.college.placement.repository.OfferRepository;

@Service
public class OfferService {

    private static final Logger log = LoggerFactory.getLogger(OfferService.class);

    private final OfferRepository offers;
    private final ApplicationRepository applications;
    private final InterviewRepository interviews;
    private final OfferMapper mapper;

    public OfferService(OfferRepository offers, ApplicationRepository applications, InterviewRepository interviews,
            OfferMapper mapper) {
        this.offers = offers;
        this.applications = applications;
        this.interviews = interviews;
        this.mapper = mapper;
    }

    @Transactional
    public OfferResponse issue(OfferRequest request) {
        Application application = applications.findById(request.applicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (offers.existsByApplicationId(application.getId())) {
            throw new ConflictException("Offer already exists for this application");
        }
        if (application.getStatus() != ApplicationStatus.INTERVIEWING) {
            throw new IllegalStateException("Offer can be issued only while application is INTERVIEWING");
        }
        if (!interviews.existsByApplicationIdAndResult(application.getId(), InterviewResult.PASS)) {
            throw new IllegalStateException("At least one interview must be passed before issuing an offer");
        }
        if (interviews.existsByApplicationIdAndResult(application.getId(), InterviewResult.SCHEDULED)) {
            throw new IllegalStateException("An interview is still waiting for its result");
        }
        checkExpiry(request.expiryDate());
        Offer offer = offers.save(new Offer(application, request.salary(), request.expiryDate()));
        application.setStatus(ApplicationStatus.OFFERED);
        log.info("Offer issued for application {}", application.getId());
        return mapper.toResponse(offer);
    }

    @Transactional(readOnly = true)
    public Page<OfferResponse> list(OfferStatus status, Pageable pageable) {
        Page<Offer> page = status == null ? offers.findAll(pageable) : offers.findByStatus(status, pageable);
        return page.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OfferResponse> mine(String email, Pageable pageable) {
        return offers.findByApplicationStudentUserEmail(email, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OfferResponse get(Long id, String email, boolean staff) {
        Offer offer = find(id);
        if (!staff && !ownerEmail(offer).equals(email)) {
            throw new AccessDeniedException("You can access only your own offers");
        }
        return mapper.toResponse(offer);
    }

    @Transactional
    public OfferResponse update(Long id, OfferUpdateRequest request) {
        Offer offer = find(id);
        requireIssued(offer);
        checkExpiry(request.expiryDate());
        offer.setSalary(request.salary());
        offer.setExpiryDate(request.expiryDate());
        log.info("Offer {} updated", id);
        return mapper.toResponse(offer);
    }

    @Transactional
    public OfferResponse respond(Long id, String email, boolean accept) {
        Offer offer = find(id);
        if (!ownerEmail(offer).equals(email)) {
            throw new AccessDeniedException("Offer belongs to another student");
        }
        if (offer.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Offer has expired");
        }
        offer.respond(accept);
        log.info("Offer {} {}", id, accept ? "accepted" : "rejected");
        return mapper.toResponse(offer);
    }

    @Transactional
    public void delete(Long id) {
        Offer offer = find(id);
        requireIssued(offer);
        offer.getApplication().setStatus(ApplicationStatus.INTERVIEWING);
        offers.delete(offer);
        log.info("Offer {} withdrawn", id);
    }

    @Transactional
    public int expireOffers() {
        return offers.expireOffers(OfferStatus.EXPIRED, OfferStatus.ISSUED, LocalDate.now());
    }

    private Offer find(Long id) {
        return offers.findById(id).orElseThrow(() -> new NotFoundException("Offer not found"));
    }

    private String ownerEmail(Offer offer) {
        return offer.getApplication().getStudent().getUser().getEmail();
    }

    private void requireIssued(Offer offer) {
        if (offer.getStatus() != OfferStatus.ISSUED) {
            throw new IllegalStateException("Only ISSUED offers can be changed");
        }
    }

    private void checkExpiry(LocalDate expiryDate) {
        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Offer expiry must be today or in the future");
        }
    }
}
