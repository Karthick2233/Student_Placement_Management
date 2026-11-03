package com.college.placement.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "companies", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String website;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean approved = false;

    @OneToMany(mappedBy = "company")
    private List<JobDrive> jobDrives = new ArrayList<>();

    protected Company() {
    }

    public Company(String name, String website, String description) {
        this.name = name;
        this.website = website;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public List<JobDrive> getJobDrives() {
        return jobDrives;
    }
}
