package com.college.placement.mapper;

import org.springframework.stereotype.Component;

import com.college.placement.dto.ApplicationDto.InterviewResponse;
import com.college.placement.entity.Interview;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(Interview interview) {
        return new InterviewResponse(interview.getId(), interview.getApplication().getId(),
                interview.getScheduledAt(), interview.getRoundName(), interview.getResult(),
                interview.getFeedback());
    }
}
