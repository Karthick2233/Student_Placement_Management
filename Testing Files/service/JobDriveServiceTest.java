package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.college.placement.TestData;
import com.college.placement.dto.DriveDto.DriveReportResponse;
import com.college.placement.dto.DriveDto.JobDriveRequest;
import com.college.placement.dto.DriveDto.JobDriveResponse;
import com.college.placement.entity.Company;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Skill;
import com.college.placement.entity.Student;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.DriveStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.JobDriveMapper;
import com.college.placement.mapper.SkillMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.CompanyRepository;
import com.college.placement.repository.JobDriveRepository;
import com.college.placement.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobDriveServiceTest {

    @Mock
    private JobDriveRepository drives;
    @Mock
    private CompanyRepository companies;
    @Mock
    private StudentRepository students;
    @Mock
    private ApplicationRepository applications;
    @Mock
    private SkillService skillService;

    private JobDriveService service;

    @BeforeEach
    void setUp() {
        service = new JobDriveService(drives, companies, students, applications, skillService,
                new JobDriveMapper(new SkillMapper()));
    }

    private JobDriveRequest request(LocalDate deadline, LocalDate driveDate) {
        return new JobDriveRequest(1L, "Java Developer", "Chennai", 6.0, "ALL", deadline, driveDate, "Backend",
                Set.of("Java"));
    }

    @Test
    void createSavesDriveAsDraftWithSkills() {
        when(companies.findById(1L)).thenReturn(Optional.of(TestData.approvedCompany()));
        when(skillService.resolve(any())).thenReturn(Set.of(new Skill("Java")));
        when(drives.save(any(JobDrive.class))).thenAnswer(i -> i.getArgument(0));

        JobDriveResponse response = service
                .create(request(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7)));

        assertEquals(DriveStatus.DRAFT, response.status());
        assertTrue(response.requiredSkills().contains("Java"));
    }

    @Test
    void createRejectsDriveDateBeforeDeadline() {
        when(companies.findById(1L)).thenReturn(Optional.of(TestData.approvedCompany()));

        assertThrows(IllegalStateException.class,
                () -> service.create(request(LocalDate.now().plusDays(7), LocalDate.now().plusDays(3))));
        verify(drives, never()).save(any(JobDrive.class));
    }

    @Test
    void createFailsForUnknownCompany() {
        when(companies.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.create(request(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7))));
    }

    @Test
    void publishMovesDraftToPublished() {
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.draftDrive()));

        assertEquals(DriveStatus.PUBLISHED, service.publish(1L).status());
    }

    @Test
    void publishFailsWhenCompanyIsNotApproved() {
        Company company = new Company("New Co", null, null);
        JobDrive drive = new JobDrive(company, "Tester", "Salem", 6.0, "ALL", LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(5), null);
        when(drives.findById(1L)).thenReturn(Optional.of(drive));

        assertThrows(IllegalStateException.class, () -> service.publish(1L));
    }

    @Test
    void cancelledDriveCannotBePublished() {
        JobDrive drive = TestData.draftDrive();
        drive.cancel();
        when(drives.findById(1L)).thenReturn(Optional.of(drive));

        assertThrows(IllegalStateException.class, () -> service.publish(1L));
    }

    @Test
    void draftDriveIsHiddenFromPublicButVisibleToManager() {
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.draftDrive()));

        assertThrows(NotFoundException.class, () -> service.get(1L, false));
        assertEquals(DriveStatus.DRAFT, service.get(1L, true).status());
    }

    @Test
    void publishedDriveCannotBeEdited() {
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.publishedDrive()));
        when(companies.findById(1L)).thenReturn(Optional.of(TestData.approvedCompany()));

        assertThrows(IllegalStateException.class,
                () -> service.update(1L, request(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7))));
    }

    @Test
    void studentWithRequiredCgpaAndDepartmentIsEligible() {
        Student student = TestData.student("s@example.com", "CSE", 8.0);
        when(students.findByUserEmail("s@example.com")).thenReturn(Optional.of(student));
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.publishedDrive()));

        assertTrue(service.eligibility(1L, "s@example.com").eligible());
    }

    @Test
    void studentWithLowCgpaIsNotEligible() {
        Student student = TestData.student("s@example.com", "CSE", 5.0);
        when(students.findByUserEmail("s@example.com")).thenReturn(Optional.of(student));
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.publishedDrive()));

        assertFalse(service.eligibility(1L, "s@example.com").eligible());
    }

    @Test
    void studentMissingRequiredSkillIsNotEligible() {
        Student student = TestData.student("s@example.com", "CSE", 8.0);
        JobDrive drive = TestData.publishedDrive();
        drive.setRequiredSkills(Set.of(new Skill("Java")));
        when(students.findByUserEmail("s@example.com")).thenReturn(Optional.of(student));
        when(drives.findById(1L)).thenReturn(Optional.of(drive));

        assertFalse(service.eligibility(1L, "s@example.com").eligible());

        student.setSkills(Set.of(new Skill("java")));
        assertTrue(service.eligibility(1L, "s@example.com").eligible());
    }

    @Test
    void reportCountsApplicationsPerStatus() {
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.publishedDrive()));
        when(applications.countByStatus(1L)).thenReturn(List.of(new Object[] { ApplicationStatus.APPLIED, 2L },
                new Object[] { ApplicationStatus.SHORTLISTED, 1L }));

        DriveReportResponse report = service.report(1L);

        assertEquals(3, report.totalApplications());
        assertEquals(2L, report.statusCounts().get(ApplicationStatus.APPLIED));
        assertEquals(0L, report.statusCounts().get(ApplicationStatus.REJECTED));
    }

    @Test
    void deleteFailsWhenDriveHasApplications() {
        when(drives.findById(1L)).thenReturn(Optional.of(TestData.draftDrive()));
        when(applications.existsByJobDriveId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(drives, never()).delete(any(JobDrive.class));
    }

    @Test
    void closeExpiredDrivesReturnsNumberOfClosedDrives() {
        when(drives.closeExpired(any(), any(), any())).thenReturn(2);

        assertEquals(2, service.closeExpiredDrives());
    }
}
