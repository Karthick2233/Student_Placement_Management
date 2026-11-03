package com.college.placement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.config.AppConfig;
import com.college.placement.dto.DriveDto.CompanyPatchRequest;
import com.college.placement.dto.DriveDto.CompanyRequest;
import com.college.placement.dto.DriveDto.CompanyResponse;
import com.college.placement.entity.Company;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.CompanyMapper;
import com.college.placement.repository.CompanyRepository;
import com.college.placement.repository.JobDriveRepository;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companies;
    private final JobDriveRepository drives;
    private final CompanyMapper mapper;

    public CompanyService(CompanyRepository companies, JobDriveRepository drives, CompanyMapper mapper) {
        this.companies = companies;
        this.drives = drives;
        this.mapper = mapper;
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companies.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Company already exists");
        }
        Company company = companies.save(mapper.toEntity(request));
        log.info("Company created: {}", company.getName());
        return mapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(Pageable pageable) {
        return companies.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> search(String keyword, Pageable pageable) {
        String text = keyword == null ? "" : keyword.trim();
        return companies.findByNameContainingIgnoreCase(text, pageable).map(mapper::toResponse);
    }

    @Cacheable(cacheNames = AppConfig.COMPANY_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @CacheEvict(cacheNames = AppConfig.COMPANY_CACHE, key = "#id")
    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = find(id);
        checkNameFree(request.name().trim(), id);
        mapper.updateEntity(company, request);
        log.info("Company {} updated", id);
        return mapper.toResponse(company);
    }

    @CacheEvict(cacheNames = AppConfig.COMPANY_CACHE, key = "#id")
    @Transactional
    public CompanyResponse patch(Long id, CompanyPatchRequest request) {
        Company company = find(id);
        if (request.name() != null) {
            checkNameFree(request.name().trim(), id);
        }
        mapper.patchEntity(company, request);
        log.info("Company {} patched", id);
        return mapper.toResponse(company);
    }

    @CacheEvict(cacheNames = AppConfig.COMPANY_CACHE, key = "#id")
    @Transactional
    public CompanyResponse approve(Long id) {
        Company company = find(id);
        company.setApproved(true);
        log.info("Company {} approved", id);
        return mapper.toResponse(company);
    }

    @CacheEvict(cacheNames = AppConfig.COMPANY_CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        Company company = find(id);
        if (drives.existsByCompanyId(id)) {
            throw new ConflictException("Company has job drives and cannot be deleted");
        }
        companies.delete(company);
        log.info("Company {} deleted", id);
    }

    private Company find(Long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private void checkNameFree(String name, Long id) {
        if (companies.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Company name already used");
        }
    }
}
