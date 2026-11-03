package com.college.placement.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.college.placement.dto.StudentDto.FileUploadResponse;
import com.college.placement.entity.Student;
import com.college.placement.exception.NotFoundException;
import com.college.placement.repository.StudentRepository;

import jakarta.annotation.PostConstruct;

@Service
public class StudentResumeService {

    private static final Logger log = LoggerFactory.getLogger(StudentResumeService.class);

    private final StudentRepository students;
    private final Path root;
    private final List<String> allowedTypes;

    public StudentResumeService(StudentRepository students,
            @Value("${placement.file.upload-dir:uploads}") String uploadDir,
            @Value("#{'${placement.file.allowed-types:pdf,doc,docx}'.split(',')}") List<String> allowedTypes) {
        this.students = students;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedTypes = allowedTypes;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload folder", e);
        }
    }

    @Transactional
    public FileUploadResponse upload(Long studentId, MultipartFile file, String email, boolean staff) {
        Student student = findAuthorized(studentId, email, staff);
        String oldFile = student.getResumePath();
        String storedName = store(studentId, file);
        if (oldFile != null && !oldFile.equals(storedName)) {
            deleteFile(oldFile);
        }
        student.setResumePath(storedName);
        log.info("Resume uploaded for student {}", studentId);
        return new FileUploadResponse(storedName, file.getSize(), "/api/students/" + studentId + "/resume");
    }

    @Transactional(readOnly = true)
    public Resource download(Long studentId, String email, boolean staff) {
        Student student = findAuthorized(studentId, email, staff);
        if (student.getResumePath() == null) {
            throw new NotFoundException("No resume uploaded for this student");
        }
        return load(student.getResumePath());
    }

    @Transactional
    public void delete(Long studentId, String email, boolean staff) {
        Student student = findAuthorized(studentId, email, staff);
        if (student.getResumePath() == null) {
            throw new NotFoundException("No resume uploaded for this student");
        }
        deleteFile(student.getResumePath());
        student.setResumePath(null);
        log.info("Resume deleted for student {}", studentId);
    }

    String store(Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("Please select a file to upload");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new IllegalStateException("File name is not valid");
        }
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!allowedTypes.contains(extension)) {
            throw new IllegalStateException("Only these file types are allowed: " + allowedTypes);
        }
        String storedName = "student-" + studentId + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the file", e);
        }
        return storedName;
    }

    Resource load(String fileName) {
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalStateException("File path is not valid");
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not read the file", e);
        }
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(root.resolve(fileName).normalize());
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete the file", e);
        }
    }

    private Student findAuthorized(Long id, String email, boolean staff) {
        Student student = students.findById(id).orElseThrow(() -> new NotFoundException("Student not found"));
        if (!staff && !student.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can access only your own resume");
        }
        return student;
    }
}
