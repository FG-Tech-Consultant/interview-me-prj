package com.interviewme.repository;

import com.interviewme.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByTenantId(Long tenantId);
    boolean existsByCnpj(String cnpj);
}
