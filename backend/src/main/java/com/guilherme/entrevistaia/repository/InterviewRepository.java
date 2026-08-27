package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Igual ao UserRepository: interface pura, Spring gera a implementação.
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    // Nome do método vira a query: "WHERE user = :user ORDER BY criadoEm DESC".
    // Usado em InterviewService.listByUser() pra montar o histórico do candidato.
    List<Interview> findByUserOrderByCriadoEmDesc(User user);

    // Usado pelo endpoint PÚBLICO de relatório (GET /interviews/public/{shareToken}/report)
    // — sem ownership check de propósito, é isso que torna o link compartilhável
    // sem exigir login de quem recebe o link.
    Optional<Interview> findByShareToken(String shareToken);
}
