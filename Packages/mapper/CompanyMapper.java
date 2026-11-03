package com.college.placement.mapper;

import org.springframework.stereotype.Component;

import com.college.placement.dto.DriveDto.CompanyPatchRequest;
import com.college.placement.dto.DriveDto.CompanyRequest;
import com.college.placement.dto.DriveDto.CompanyResponse;
import com.college.placement.entity.Company;

@Component
public class CompanyMapper {

    public Company toEntity(CompanyRequest request) {
        return new Company(request.name().trim(), request.website(), request.description());
    }

    public void updateEntity(Company company, CompanyRequest request) {
        company.setName(request.name().trim());
        company.setWebsite(request.website());
        company.setDescription(request.description());
    }

    public void patchEntity(Company company, CompanyPatchRequest request) {
        if (request.name() != null) {
            company.setName(request.name().trim());
        }
        if (request.website() != null) {
            company.setWebsite(request.website());
        }
        if (request.description() != null) {
            company.setDescription(request.description());
        }
    }

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getWebsite(),
                company.getDescription(), company.isApproved());
    }
}
