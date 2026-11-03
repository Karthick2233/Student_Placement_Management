package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.college.placement.exception.NotFoundException;
import com.college.placement.repository.StudentRepository;

class StudentResumeServiceTest {

    @TempDir
    Path tempDir;

    private StudentResumeService service;

    @BeforeEach
    void setUp() {
        service = new StudentResumeService(mock(StudentRepository.class), tempDir.toString(),
                List.of("pdf", "doc", "docx"));
        service.init();
    }

    @Test
    void storesAllowedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "data".getBytes());

        String name = service.store(1L, file);

        assertEquals("student-1.pdf", name);
        assertTrue(Files.exists(tempDir.toAbsolutePath().normalize().resolve(name)));
        assertTrue(service.load(name).exists());
    }

    @Test
    void rejectsNotAllowedType() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "data".getBytes());

        assertThrows(IllegalStateException.class, () -> service.store(1L, file));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalStateException.class, () -> service.store(1L, file));
    }

    @Test
    void missingFileIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.load("student-99.pdf"));
    }
}
