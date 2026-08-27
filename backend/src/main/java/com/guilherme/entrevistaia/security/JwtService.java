package com.guilherme.entrevistaia.security;

import com.guilherme.entrevistaia.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

// Responsável por CRIAR e VALIDAR tokens JWT. Não sabe nada sobre HTTP —
// isso é papel do JwtAuthenticationFilter, que usa esta classe.
//
// Conceito-chave: JWT é "stateless". Diferente de sessão tradicional (onde o
// servidor guarda quem está logado em memória/banco), aqui o próprio token
// carrega a informação (o id do usuário) e é assinado digitalmente. Qualquer
// instância da aplicação consegue validar o token sozinha, sem consultar nada
// além da chave secreta — por isso dá pra ter vários servidores sem "sticky session".
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    // Lido de application.yml (jwt.secret, que por sua vez vem da env var JWT_SECRET).
    // É o "segredo compartilhado": só quem tem essa chave consegue gerar uma
    // assinatura válida ou confirmar que uma assinatura é válida.
    @Value("${jwt.secret}")
    private String secret;

    // Tempo de vida do token em milissegundos (24h por padrão). Depois disso,
    // isTokenValid() passa a rejeitar o token mesmo que a assinatura esteja ok.
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // Converte a string secreta numa SecretKey no formato que a lib jjwt entende,
    // pronta para assinar (HMAC-SHA).
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Chamado em AuthController após registro/login bem-sucedido. Monta um JWT
    // com 3 partes (header.payload.signature): aqui definimos o payload (subject
    // = id do usuário, claim extra "email") e assinamos com a chave secreta.
    // O resultado é a string que o front-end vai guardar e reenviar em cada
    // requisição, no header "Authorization: Bearer <token>".
    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    // Extrai o id do usuário de dentro do token (não precisa de banco pra isso —
    // é só decodificar e checar a assinatura). Usado pelo filtro pra depois
    // buscar o User de verdade no banco.
    public UUID extractUserId(String token) {
        String subject = parseClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    // true = assinatura confere E o token ainda não expirou.
    // Qualquer coisa errada (assinatura inválida, token corrompido, expirado)
    // vira "false" em vez de exceção — quem chama não precisa lidar com try/catch.
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("[JWT_EXPIRED] Token expirado: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[JWT_INVALID] Token inválido: {}", ex.getMessage());
            return false;
        }
    }

    // Faz o trabalho pesado: decodifica o token e RECALCULA a assinatura com a
    // chave secreta pra comparar com a que veio no token. Se alguém alterar o
    // payload (ex.: trocar o id do usuário) sem ter a chave, a assinatura não
    // vai bater e isso lança JwtException — é o que impede forjar tokens.
    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
