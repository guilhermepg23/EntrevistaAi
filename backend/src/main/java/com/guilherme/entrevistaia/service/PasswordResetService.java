package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.PasswordResetToken;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.InvalidResetTokenException;
import com.guilherme.entrevistaia.mail.PasswordResetMailer;
import com.guilherme.entrevistaia.repository.PasswordResetTokenRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// Fluxo de recuperação de senha (POST /auth/forgot-password -> /auth/reset-password).
//
// Segurança do desenho:
// - forgot-password NUNCA revela se o email existe (o controller sempre responde
//   200; aqui, email inexistente simplesmente não faz nada).
// - o token que vai no email é aleatório (256 bits) e só o hash SHA-256 dele é
//   salvo — vazar o banco não entrega tokens usáveis.
// - uso único (usado=true) e validade curta (app.password-reset.ttl-minutes).
// - pedir um link novo invalida os anteriores (deleteByUser antes de criar).
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final String frontendBaseUrl;
    private final long ttlMinutes;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordResetMailer mailer,
                                 @Value("${app.frontend-base-url}") String frontendBaseUrl,
                                 @Value("${app.password-reset.ttl-minutes}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.frontendBaseUrl = frontendBaseUrl;
        this.ttlMinutes = ttlMinutes;
    }

    // Chamado por POST /auth/forgot-password. Se o email tiver conta, gera um
    // token novo (invalidando os antigos) e dispara o email com o link. Se não
    // tiver, não faz nada — o controller responde 200 do mesmo jeito.
    @Transactional
    public void requestReset(String email) {
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.info("[PASSWORD_RESET_REQUEST_UNKNOWN_EMAIL] email={}", email);
            return;
        }
        User user = maybeUser.get();

        tokenRepository.deleteByUser(user);

        String rawToken = gerarTokenAleatorio();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiraEm(OffsetDateTime.now().plusMinutes(ttlMinutes));
        token.setUsado(false);
        token.setCriadoEm(OffsetDateTime.now());
        tokenRepository.save(token);

        String link = frontendBaseUrl.replaceAll("/+$", "") + "/redefinir-senha?token=" + rawToken;
        log.info("[PASSWORD_RESET_REQUESTED] userId={} expiraEm={}", user.getId(), token.getExpiraEm());
        mailer.sendPasswordReset(user.getEmail(), user.getNome(), link);
    }

    // Chamado por POST /auth/reset-password. Valida o token (existe, não usado,
    // não expirado) e troca a senha. Qualquer problema -> InvalidResetTokenException (400).
    @Transactional
    public void resetPassword(String rawToken, String novaSenha) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidResetTokenException::new);

        if (!token.utilizavel()) {
            log.info("[PASSWORD_RESET_TOKEN_UNUSABLE] tokenId={} usado={} expiraEm={}",
                token.getId(), token.isUsado(), token.getExpiraEm());
            throw new InvalidResetTokenException();
        }

        User user = token.getUser();
        user.setSenhaHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);

        token.setUsado(true);
        tokenRepository.save(token);

        log.info("[PASSWORD_RESET_COMPLETE] userId={}", user.getId());
    }

    private static String gerarTokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 é garantido pela plataforma; se faltar, é erro de ambiente.
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
