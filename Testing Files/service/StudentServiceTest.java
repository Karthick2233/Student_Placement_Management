package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.college.placement.TestData;
import com.college.placement.dto.StudentDto.StudentPatchRequest;
import com.college.placement.dto.StudentDto.StudentRequest;
import com.college.placement.dto.StudentDto.StudentResponse;
import com.college.placement.entity.AppUser;
import com.college.placement.entity.Application;
import com.college.placement.entity.Skill;
import com.college.placement.entity.Student;
import com.college.placement.enums.Role;
import com.college.placement.exception.ConflictException;
import com.college.placement.mapper.ApplicationMapper;
import com.college.placement.mapper.SkillMapper;
import com.college.placement.mapper.StudentMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.StudentRepository;
import com.college.placement.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentServiceTest {

    @Mock
    private StudentRepository students;
    @Mock
    private UserRepository users;
    @Mock
    private ApplicationRepository applications;
    @Mock
    private SkillService skillService;

    private StudentService service;

    @BeforeEach
    void setUp() {
        service = new StudentService(students, users, applications, skillService,
                new StudentMapper(new SkillMapper()), new ApplicationMapper());
    }

    @Test
    void createSavesProfileWithSkills() {
        AppUser user = new AppUser("Ravi", "ravi@example.com", "hash", Role.STUDENT);
        when(users.findByEmail("ravi@example.com")).thenReturn(Optional.of(user));
        when(students.findByUserEmail("ravi@example.com")).thenReturn(Optional.empty());
        when(skillService.resolve(any())).thenReturn(Set.of(new Skill("Java")));
        when(students.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse response = service.create("ravi@example.com",
                new StudentRequest("CSE", 2026, 8.2, Set.of("Java")));

        assertEquals("CSE", response.department());
        assertEquals(8.2, response.cgpa());
        assertTrue(response.skills().contains("Java"));
    }

    @Test
    void createFailsForNonStudentAccount() {
        AppUser admin = new AppUser("Admin", "admin@example.com", "hash", Role.ADMIN);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class,
                () -> service.create("admin@example.com", new StudentRequest("CSE", 2026, 8.0, null)));
        verify(students, never()).save(any(Student.class));
    }

    @Test
    void createFailsWhenProfileAlreadyExists() {
        Student existing = TestData.student("ravi@example.com", "CSE", 8.0);
        when(users.findByEmail("ravi@example.com")).thenReturn(Optional.of(existing.getUser()));
        when(students.findByUserEmail("ravi@example.com")).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> service.create("ravi@example.com", new StudentRequest("CSE", 2026, 8.0, null)));
    }

    @Test
    void studentCannotReadAnotherStudentsProfile() {
        Student student = TestData.student("owner@example.com", "CSE", 8.0);
        when(students.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(AccessDeniedException.class, () -> service.get(1L, "other@example.com", false));
        assertEquals("owner@example.com", service.get(1L, "owner@example.com", false).email());
        assertEquals("owner@example.com", service.get(1L, "admin@example.com", true).email());
    }

    @Test
    void patchChangesOnlyProvidedFields() {
        Student student = TestData.student("ravi@example.com", "CSE", 7.5);
        when(students.findById(1L)).thenReturn(Optional.of(student));

        StudentResponse response = service.patch(1L, new StudentPatchRequest(null, null, 9.0, null),
                "ravi@example.com", false);

        assertEquals("CSE", response.department());
        assertEquals(9.0, response.cgpa());
    }

    @Test
    void deleteFailsWhenStudentHasApplications() {
        Student student = TestData.student("ravi@example.com", "CSE", 8.0);
        student.getApplications().add(new Application(student, TestData.publishedDrive()));
        when(students.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(students, never()).delete(any(Student.class));
    }
}
