package com.interviewme.mapper;

import com.interviewme.dto.company.CompanyProfileDto;
import com.interviewme.dto.company.CompanyUpdateRequest;
import com.interviewme.model.Company;

public class CompanyMapper {

    public static CompanyProfileDto toProfileDto(Company company) {
        return new CompanyProfileDto(
            company.getId(),
            company.getName(),
            company.getCnpj(),
            company.getWebsite(),
            company.getSector(),
            company.getSize(),
            company.getDescription(),
            company.getLogoUrl(),
            company.getCountry(),
            company.getCity(),
            company.getActive(),
            company.getCreatedAt(),
            company.getUpdatedAt()
        );
    }

    public static void updateEntity(Company company, CompanyUpdateRequest request) {
        company.setName(request.name());
        company.setCnpj(request.cnpj());
        company.setWebsite(request.website());
        company.setSector(request.sector());
        company.setSize(request.size());
        company.setDescription(request.description());
        company.setLogoUrl(request.logoUrl());
        company.setCountry(request.country());
        company.setCity(request.city());
    }
}
