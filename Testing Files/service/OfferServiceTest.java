package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.college.placement.TestData;
import com.college.placement.dto.ApplicationDto.OfferRequest;
import com.college.placement.dto.ApplicationDto.OfferResponse;
import com.college.placement.entity.Application;
import com.college.placement.entity.Offer;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.InterviewResult;
import com.college.placement.enums.OfferStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.mapper.OfferMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.InterviewRepository;
import com.college.placement.repository.OfferRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfferServiceTest {

    @Mock
    private OfferRepository offers;
    @Mock
    private ApplicationRepository applications;
    @Mock
    private InterviewRepository interviews;

    private OfferService service;
    private Application application;

    @BeforeEach
    void setUp() {
        service = new OfferService(offers, applications, interviews, new OfferMapper());
        application = TestData.withId(
                new Application(TestData.student("s@example.com", "CSE", 8.0), TestData.publishedDrive()), 1L);
        application.setStatus(ApplicationStatus.INTERVIEWING);
        when(applications.findById(1L)).thenReturn(Optional.of(application));
        when(offers.existsByApplicationId(1L)).thenReturn(false);
        when(interviews.existsByApplicationIdAndResult(1L, InterviewResult.PASS)).thenReturn(true);
        when(interviews.existsByApplicationIdAndResult(1L, InterviewResult.SCHEDULED)).thenReturn(false);
        when(offers.save(any(Offer.class))).thenAnswer(i -> i.getArgument(0));
    }

    private OfferRequest request() {
        return new OfferRequest(1L, 600000.0, LocalDate.now().plusDays(10));
    }

    @Test
    void issueCreatesOfferAndMarksApplicationOffered() {
        OfferResponse response = service.issue(request());

        assertEquals(OfferStatus.ISSUED, response.status());
        assertEquals(ApplicationStatus.OFFERED, application.getStatus());
    }

    @Test
    void issueFailsWhenOfferAlreadyExists() {
        when(offers.existsByApplicationId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.issue(request()));
    }

    @Test
    void issueFailsWhenApplicationIsNotInterviewing() {
        application.setStatus(ApplicationStatus.SHORTLISTED);

        assertThrows(IllegalStateException.class, () -> service.issue(request()));
        verify(offers, never()).save(any(Offer.class));
    }

    @Test
    void issueFailsWithoutPassedInterview() {
        when(interviews.existsByApplicationIdAndResult(1L, InterviewResult.PASS)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.issue(request()));
    }

    @Test
    void issueFailsWhenAnInterviewResultIsPending() {
        when(interviews.existsByApplicationIdAndResult(1L, InterviewResult.SCHEDULED)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.issue(request()));
    }

    @Test
    void issueFailsForPastExpiryDate() {
        assertThrows(IllegalStateException.class,
                () -> service.issue(new OfferRequest(1L, 600000.0, LocalDate.now().minusDays(1))));
    }

    @Test
    void studentCanAcceptOwnOffer() {
        Offer offer = new Offer(application, 600000.0, LocalDate.now().plusDays(5));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));

        assertEquals(OfferStatus.ACCEPTED, service.respond(7L, "s@example.com", true).status());
    }

    @Test
    void studentCannotRespondToAnotherStudentsOffer() {
        Offer offer = new Offer(application, 600000.0, LocalDate.now().plusDays(5));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));

        assertThrows(AccessDeniedException.class, () -> service.respond(7L, "other@example.com", true));
    }

    @Test
    void expiredOfferCannotBeAccepted() {
        Offer offer = new Offer(application, 600000.0, LocalDate.now().minusDays(1));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalStateException.class, () -> service.respond(7L, "s@example.com", true));
    }

    @Test
    void offerCanBeAnsweredOnlyOnce() {
        Offer offer = new Offer(application, 600000.0, LocalDate.now().plusDays(5));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));
        service.respond(7L, "s@example.com", false);

        assertThrows(IllegalStateException.class, () -> service.respond(7L, "s@example.com", true));
    }

    @Test
    void withdrawingOfferReturnsApplicationToInterviewing() {
        application.setStatus(ApplicationStatus.OFFERED);
        Offer offer = new Offer(application, 600000.0, LocalDate.now().plusDays(5));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));

        service.delete(7L);

        assertEquals(ApplicationStatus.INTERVIEWING, application.getStatus());
        verify(offers).delete(eq(offer));
    }

    @Test
    void expireOffersReturnsNumberOfExpiredOffers() {
        when(offers.expireOffers(any(), any(), any())).thenReturn(3);

        assertEquals(3, service.expireOffers());
    }
}
