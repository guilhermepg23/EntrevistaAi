package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Repositório Spring Data JPA: é só uma interface, sem nenhuma implementação
// escrita por nós. O Spring gera a implementação em tempo de execução (proxy).
// JpaRepository<User, UUID> já dá de graça: save(), findById(), findAll(), delete()...
public interface UserRepository extends JpaRepository<User, UUID> {

    // "Query methods": o Spring lê o NOME do método e monta o SQL sozinho.
    // findByEmail(x) vira "SELECT * FROM users WHERE email = x".
    Optional<User> findByEmail(String email);

    // Vira um SELECT COUNT/EXISTS otimizado (não traz a entidade inteira).
    // Usado no AuthController pra checar duplicidade de email antes de cadastrar.
    boolean existsByEmail(String email);
}
