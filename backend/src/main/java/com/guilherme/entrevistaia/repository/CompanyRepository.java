package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}
