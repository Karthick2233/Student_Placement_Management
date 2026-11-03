package com.college.placement.mapper;

import org.springframework.stereotype.Component;

import com.college.placement.dto.ApplicationDto.OfferResponse;
import com.college.placement.entity.Offer;

@Component
public class OfferMapper {

    public OfferResponse toResponse(Offer offer) {
        return new OfferResponse(offer.getId(), offer.getApplication().getId(),
                offer.getApplication().getStudent().getUser().getName(),
                offer.getApplication().getJobDrive().getTitle(), offer.getSalary(), offer.getExpiryDate(),
                offer.getStatus());
    }
}
