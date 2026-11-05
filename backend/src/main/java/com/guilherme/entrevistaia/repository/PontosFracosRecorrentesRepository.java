package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.PontosFracosRecorrentes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// A chave é o id do candidato (uma linha por candidato).
public interface PontosFracosRecorrentesRepository extends JpaRepository<PontosFracosRecorrentes, UUID> {
}
