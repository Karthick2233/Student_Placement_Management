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
import org.springframework.security.access.AccessDeniedException;

import com.college.placement.TestData;
import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.dto.ApplicationDto.ApplicationStatusRequest;
import com.college.placement.entity.Application;
import com.college.placement.entity.Interview;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Student;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.mapper.ApplicationMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.JobDriveRepository;
import com.college.placement.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applications;
    @Mock
    private StudentRepository students;
    @Mock
    private JobDriveRepository drives;

    private ApplicationService service;
    private Student student;
    private JobDrive drive;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, students, drives, new ApplicationMapper());
        student = TestData.student("s@example.com", "CSE", 8.0);
        drive = TestData.publishedDrive();
        when(students.findByUserEmail("s@example.com")).thenReturn(Optional.of(student));
        when(drives.findById(1L)).thenReturn(Optional.of(drive));
        when(applications.existsByStudentIdAndJobDriveId(any(), any())).thenReturn(false);
        when(applications.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void applyCreatesApplicationInAppliedStatus() {
        ApplicationResponse response = service.apply(1L, "s@example.com");

        assertEquals(ApplicationStatus.APPLIED, response.status());
        assertEquals("Java Developer", response.driveTitle());
    }

    @Test
    void applyRejectsDuplicateApplication() {
        when(applications.existsByStudentIdAndJobDriveId(any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.apply(1L, "s@example.com"));
        verify(applications, never()).save(any(Application.class));
    }

    @Test
    void applyRejectsIneligibleStudent() {
        student.setCgpa(4.5);

        assertThrows(IllegalStateException.class, () -> service.apply(1L, "s@example.com"));
    }

    @Test
    void applyRejectsDriveThatIsNotPublished() {
        when(drives.findById(2L)).thenReturn(Optional.of(TestData.draftDrive()));

        assertThrows(IllegalStateException.class, () -> service.apply(2L, "s@example.com"));
    }

    @Test
    void shortlistMovesAppliedToShortlisted() {
        Application application = new Application(student, drive);
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertEquals(ApplicationStatus.SHORTLISTED, service.shortlist(5L).status());
        assertThrows(IllegalStateException.class, () -> service.shortlist(5L));
    }

    @Test
    void updateStatusAllowsOnlyRejected() {
        Application application = new Application(student, drive);
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(5L, new ApplicationStatusRequest(ApplicationStatus.OFFERED)));
        assertEquals(ApplicationStatus.REJECTED,
                service.updateStatus(5L, new ApplicationStatusRequest(ApplicationStatus.REJECTED)).status());
    }

    @Test
    void studentCanCancelOwnApplication() {
        Application application = new Application(student, drive);
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertEquals(ApplicationStatus.WITHDRAWN, service.cancel(5L, "s@example.com").status());
    }

    @Test
    void studentCannotCancelAnotherStudentsApplication() {
        Application application = new Application(student, drive);
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertThrows(AccessDeniedException.class, () -> service.cancel(5L, "other@example.com"));
    }

    @Test
    void deleteFailsWhenApplicationHasInterviews() {
        Application application = new Application(student, drive);
        application.setStatus(ApplicationStatus.REJECTED);
        application.getInterviews().add(new Interview(application, LocalDateTime.now().plusDays(1), "Technical"));
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertThrows(ConflictException.class, () -> service.delete(5L));
        verify(applications, never()).delete(any(Application.class));
    }

    @Test
    void deleteFailsForActiveApplication() {
        Application application = new Application(student, drive);
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        assertThrows(IllegalStateException.class, () -> service.delete(5L));
    }
}
