package com.guilherme.entrevistaia.security;

import com.guilherme.entrevistaia.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// JwtService não é gerenciado pelo Spring aqui (sem @SpringBootTest) — os
// campos @Value (secret, expirationMs) são setados na mão via
// ReflectionTestUtils, já que em produção quem faz isso é o Spring.
class JwtServiceTest {

    private static final String SECRET = "um-segredo-de-teste-com-pelo-menos-32-caracteres";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("teste@teste.com");
    }

    @Test
    void generateToken_deveGerarTokenValidoComIdDoUsuarioComoSubject() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void isTokenValid_deveRetornarFalseParaTokenAssinadoComOutraChave() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
            "outro-segredo-completamente-diferente-32-chars".getBytes(StandardCharsets.UTF_8));
        String tokenForjado = Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(outraChave)
            .compact();

        assertThat(jwtService.isTokenValid(tokenForjado)).isFalse();
    }

    @Test
    void isTokenValid_deveRetornarFalseParaTokenExpirado() {
        SecretKey chave = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenExpirado = Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(new Date(System.currentTimeMillis() - 10_000))
            .expiration(new Date(System.currentTimeMillis() - 5_000))
            .signWith(chave)
            .compact();

        assertThat(jwtService.isTokenValid(tokenExpirado)).isFalse();
    }

    @Test
    void isTokenValid_deveRetornarFalseParaTokenCorrompido() {
        assertThat(jwtService.isTokenValid("isso.nao.e-um-jwt-valido")).isFalse();
    }
}
