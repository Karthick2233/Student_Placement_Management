package com.college.placement.entity;

import java.util.ArrayList;
import java.util.List;

import com.college.placement.enums.ApplicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "job_drive_id" }))
public class Application extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "job_drive_id", nullable = false)
    private JobDrive jobDrive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @OneToMany(mappedBy = "application")
    private List<Interview> interviews = new ArrayList<>();

    protected Application() {
    }

    public Application(Student student, JobDrive jobDrive) {
        this.student = student;
        this.jobDrive = jobDrive;
    }

    public Student getStudent() {
        return student;
    }

    public JobDrive getJobDrive() {
        return jobDrive;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public List<Interview> getInterviews() {
        return interviews;
    }

    public void shortlist() {
        if (status != ApplicationStatus.APPLIED) {
            throw new IllegalStateException("Only APPLIED applications can be shortlisted");
        }
        status = ApplicationStatus.SHORTLISTED;
    }
}
