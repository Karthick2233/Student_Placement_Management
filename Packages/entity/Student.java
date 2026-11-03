package com.college.placement.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@NamedQueries({
        @NamedQuery(name = "Student.findPlacedNamed",
                query = "select distinct a.student from Offer o join o.application a where o.status = :status"),
        @NamedQuery(name = "Student.findByMinCgpaNamed",
                query = "select s from Student s where s.cgpa >= :minCgpa order by s.cgpa desc")
})
@Entity
@Table(name = "students", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Student extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Integer graduationYear;

    @Column(nullable = false)
    private Double cgpa;

    @Column(name = "resume_path")
    private String resumePath;

    @ManyToMany
    @JoinTable(name = "student_skills",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    @OneToMany(mappedBy = "student")
    private List<Application> applications = new ArrayList<>();

    protected Student() {
    }

    public Student(AppUser user, String department, Integer graduationYear, Double cgpa) {
        this.user = user;
        this.department = department;
        this.graduationYear = graduationYear;
        this.cgpa = cgpa;
    }

    public AppUser getUser() {
        return user;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = skills;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public List<Application> getApplications() {
        return applications;
    }
}
