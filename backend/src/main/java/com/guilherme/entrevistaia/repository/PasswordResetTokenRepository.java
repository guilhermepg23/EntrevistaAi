package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.PasswordResetToken;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Invalida qualquer token anterior do usuário quando ele pede um novo, e
    // limpa os tokens quando a conta é excluída. Chamado dentro de métodos
    // @Transactional (PasswordResetService / AccountService).
    void deleteByUser(User user);
}
