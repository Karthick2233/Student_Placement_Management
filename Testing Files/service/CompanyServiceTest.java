package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.college.placement.dto.DriveDto.CompanyRequest;
import com.college.placement.dto.DriveDto.CompanyResponse;
import com.college.placement.entity.Company;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.CompanyMapper;
import com.college.placement.repository.CompanyRepository;
import com.college.placement.repository.JobDriveRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companies;
    @Mock
    private JobDriveRepository drives;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companies, drives, new CompanyMapper());
    }

    @Test
    void createSavesNewCompanyAsNotApproved() {
        when(companies.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(companies.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        CompanyResponse response = service.create(new CompanyRequest("Acme", "https://acme.com", "IT"));

        assertFalse(response.approved());
    }

    @Test
    void createRejectsDuplicateName() {
        when(companies.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new CompanyRequest("Acme", "https://acme.com", "IT")));
        verify(companies, never()).save(any(Company.class));
    }

    @Test
    void approveMarksCompanyAsApproved() {
        when(companies.findById(1L)).thenReturn(Optional.of(new Company("Acme", null, null)));

        assertTrue(service.approve(1L).approved());
    }

    @Test
    void getFailsForUnknownCompany() {
        when(companies.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(9L));
    }

    @Test
    void deleteFailsWhenCompanyHasDrives() {
        when(companies.findById(1L)).thenReturn(Optional.of(new Company("Acme", null, null)));
        when(drives.existsByCompanyId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(companies, never()).delete(any(Company.class));
    }
}
