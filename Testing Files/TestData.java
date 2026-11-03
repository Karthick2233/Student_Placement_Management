package com.college.placement;

import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.college.placement.entity.AppUser;
import com.college.placement.entity.Company;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Student;
import com.college.placement.enums.Role;

public final class TestData {

    private TestData() {
    }

    public static Student student(String email, String department, double cgpa) {
        AppUser user = new AppUser("Student", email, "hash", Role.STUDENT);
        return new Student(user, department, 2026, cgpa);
    }

    public static Company approvedCompany() {
        Company company = new Company("Acme", "https://acme.com", "Software company");
        company.setApproved(true);
        return company;
    }

    public static JobDrive draftDrive() {
        return new JobDrive(approvedCompany(), "Java Developer", "Chennai", 6.0, "ALL",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10), "Backend role");
    }

    public static JobDrive publishedDrive() {
        JobDrive drive = draftDrive();
        drive.publish();
        return drive;
    }

    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
