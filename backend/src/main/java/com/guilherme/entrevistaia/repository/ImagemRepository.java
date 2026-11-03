package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Imagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImagemRepository extends JpaRepository<Imagem, UUID> {
}
