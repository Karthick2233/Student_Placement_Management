package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.college.placement.TestData;
import com.college.placement.dto.ApplicationDto.InterviewRequest;
import com.college.placement.dto.ApplicationDto.InterviewResponse;
import com.college.placement.dto.ApplicationDto.InterviewResultRequest;
import com.college.placement.dto.ApplicationDto.InterviewUpdateRequest;
import com.college.placement.entity.Application;
import com.college.placement.entity.Interview;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.InterviewResult;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.InterviewMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.InterviewRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviews;
    @Mock
    private ApplicationRepository applications;

    private InterviewService service;
    private Application application;

    @BeforeEach
    void setUp() {
        service = new InterviewService(interviews, applications, new InterviewMapper());
        application = new Application(TestData.student("s@example.com", "CSE", 8.0), TestData.publishedDrive());
        when(applications.findById(1L)).thenReturn(Optional.of(application));
        when(interviews.save(any(Interview.class))).thenAnswer(i -> i.getArgument(0));
    }

    private InterviewRequest request() {
        return new InterviewRequest(1L, LocalDateTime.now().plusDays(2), "Technical Round");
    }

    @Test
    void scheduleMovesShortlistedApplicationToInterviewing() {
        application.setStatus(ApplicationStatus.SHORTLISTED);

        InterviewResponse response = service.schedule(request());

        assertEquals(InterviewResult.SCHEDULED, response.result());
        assertEquals(ApplicationStatus.INTERVIEWING, application.getStatus());
    }

    @Test
    void scheduleFailsWhenApplicationIsNotShortlisted() {
        assertThrows(IllegalStateException.class, () -> service.schedule(request()));
        verify(interviews, never()).save(any(Interview.class));
    }

    @Test
    void scheduleFailsForUnknownApplication() {
        when(applications.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.schedule(new InterviewRequest(9L, LocalDateTime.now().plusDays(1), "HR")));
    }

    @Test
    void failResultRejectsTheApplication() {
        application.setStatus(ApplicationStatus.INTERVIEWING);
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));

        service.updateResult(3L, new InterviewResultRequest(InterviewResult.FAIL, "Weak basics"));

        assertEquals(InterviewResult.FAIL, interview.getResult());
        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
    }

    @Test
    void passResultKeepsApplicationInterviewing() {
        application.setStatus(ApplicationStatus.INTERVIEWING);
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));

        service.updateResult(3L, new InterviewResultRequest(InterviewResult.PASS, "Good"));

        assertEquals(InterviewResult.PASS, interview.getResult());
        assertEquals(ApplicationStatus.INTERVIEWING, application.getStatus());
    }

    @Test
    void resultCannotBeRecordedTwice() {
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        interview.updateResult(InterviewResult.PASS, "Good");
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));

        assertThrows(IllegalStateException.class,
                () -> service.updateResult(3L, new InterviewResultRequest(InterviewResult.FAIL, null)));
    }

    @Test
    void resultCannotBeSetBackToScheduled() {
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));

        assertThrows(IllegalStateException.class,
                () -> service.updateResult(3L, new InterviewResultRequest(InterviewResult.SCHEDULED, null)));
    }

    @Test
    void rescheduleChangesTimeAndRound() {
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));
        LocalDateTime newTime = LocalDateTime.now().plusDays(4);

        InterviewResponse response = service.update(3L, new InterviewUpdateRequest(newTime, "HR Round"));

        assertEquals(newTime, response.scheduledAt());
        assertEquals("HR Round", response.roundName());
    }

    @Test
    void deleteFailsAfterResultIsRecorded() {
        Interview interview = new Interview(application, LocalDateTime.now().plusDays(1), "Technical");
        interview.updateResult(InterviewResult.NO_SHOW, null);
        when(interviews.findById(3L)).thenReturn(Optional.of(interview));

        assertThrows(IllegalStateException.class, () -> service.delete(3L));
        verify(interviews, never()).delete(any(Interview.class));
    }
}
