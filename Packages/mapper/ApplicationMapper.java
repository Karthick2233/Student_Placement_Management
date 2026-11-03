package com.college.placement.mapper;

import org.springframework.stereotype.Component;

import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.entity.Application;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(application.getId(), application.getStudent().getId(),
                application.getStudent().getUser().getName(), application.getJobDrive().getId(),
                application.getJobDrive().getTitle(), application.getStatus());
    }
}
