package com.college.placement.entity;

import java.time.LocalDateTime;

import com.college.placement.enums.InterviewResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private String roundName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewResult result = InterviewResult.SCHEDULED;

    @Column(length = 1000)
    private String feedback;

    protected Interview() {
    }

    public Interview(Application application, LocalDateTime scheduledAt, String roundName) {
        this.application = application;
        this.scheduledAt = scheduledAt;
        this.roundName = roundName;
    }

    public Application getApplication() {
        return application;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public InterviewResult getResult() {
        return result;
    }

    public String getFeedback() {
        return feedback;
    }

    public void updateResult(InterviewResult result, String feedback) {
        this.result = result;
        this.feedback = feedback;
    }
}
