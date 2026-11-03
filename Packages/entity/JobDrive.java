package com.college.placement.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.college.placement.enums.DriveStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_drives")
public class JobDrive extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double minimumCgpa;

    @Column(nullable = false)
    private String eligibleDepartment;

    @Column(nullable = false)
    private LocalDate applicationDeadline;

    @Column(nullable = false)
    private LocalDate driveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriveStatus status = DriveStatus.DRAFT;

    @Column(length = 3000)
    private String description;

    @ManyToMany
    @JoinTable(name = "drive_skills",
            joinColumns = @JoinColumn(name = "drive_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> requiredSkills = new HashSet<>();

    @OneToMany(mappedBy = "jobDrive")
    private List<Application> applications = new ArrayList<>();

    protected JobDrive() {
    }

    public JobDrive(Company company, String title, String location, Double minimumCgpa, String eligibleDepartment,
            LocalDate applicationDeadline, LocalDate driveDate, String description) {
        this.company = company;
        this.title = title;
        this.location = location;
        this.minimumCgpa = minimumCgpa;
        this.eligibleDepartment = eligibleDepartment;
        this.applicationDeadline = applicationDeadline;
        this.driveDate = driveDate;
        this.description = description;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getMinimumCgpa() {
        return minimumCgpa;
    }

    public void setMinimumCgpa(Double minimumCgpa) {
        this.minimumCgpa = minimumCgpa;
    }

    public String getEligibleDepartment() {
        return eligibleDepartment;
    }

    public void setEligibleDepartment(String eligibleDepartment) {
        this.eligibleDepartment = eligibleDepartment;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public LocalDate getDriveDate() {
        return driveDate;
    }

    public void setDriveDate(LocalDate driveDate) {
        this.driveDate = driveDate;
    }

    public DriveStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Skill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(Set<Skill> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void publish() {
        if (status != DriveStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT drives can be published");
        }
        if (!company.isApproved()) {
            throw new IllegalStateException("Company must be approved before publishing");
        }
        if (applicationDeadline.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Deadline must be today or in the future");
        }
        status = DriveStatus.PUBLISHED;
    }

    public void cancel() {
        if (status == DriveStatus.CANCELLED || status == DriveStatus.CLOSED) {
            throw new IllegalStateException("Drive is already " + status);
        }
        status = DriveStatus.CANCELLED;
    }

    public boolean isEligible(Student student) {
        boolean cgpaOk = student.getCgpa() >= minimumCgpa;
        boolean departmentOk = "ALL".equalsIgnoreCase(eligibleDepartment)
                || eligibleDepartment.equalsIgnoreCase(student.getDepartment());
        boolean deadlineOk = !applicationDeadline.isBefore(LocalDate.now());
        boolean skillsOk = student.getSkills().containsAll(requiredSkills);
        return cgpaOk && departmentOk && deadlineOk && skillsOk;
    }
}
