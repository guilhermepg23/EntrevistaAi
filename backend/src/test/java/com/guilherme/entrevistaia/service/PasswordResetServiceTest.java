package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.PasswordResetToken;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.InvalidResetTokenException;
import com.guilherme.entrevistaia.mail.PasswordResetMailer;
import com.guilherme.entrevistaia.repository.PasswordResetTokenRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetMailer mailer;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailer,
            "https://app.exemplo.com/", 30);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ana@teste.com");
        user.setNome("Ana");
        user.setSenhaHash("hash-antigo");
    }

    @Test
    void requestReset_emailDesconhecido_naoGeraTokenNemEmail() {
        when(userRepository.findByEmail("ninguem@teste.com")).thenReturn(Optional.empty());

        service.requestReset("ninguem@teste.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(mailer);
    }

    @Test
    void requestReset_emailConhecido_invalidaAntigosSalvaNovoEEnviaLink() {
        when(userRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));

        service.requestReset("ana@teste.com");

        // Tokens antigos do usuário são apagados antes de criar o novo.
        verify(tokenRepository).deleteByUser(user);

        ArgumentCaptor<PasswordResetToken> tokenCap = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCap.capture());
        PasswordResetToken salvo = tokenCap.getValue();
        assertThat(salvo.getUser()).isEqualTo(user);
        assertThat(salvo.getTokenHash()).isNotBlank();
        assertThat(salvo.isUsado()).isFalse();
        assertThat(salvo.getExpiraEm()).isAfter(OffsetDateTime.now());

        ArgumentCaptor<String> linkCap = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendPasswordReset(org.mockito.ArgumentMatchers.eq("ana@teste.com"),
            org.mockito.ArgumentMatchers.eq("Ana"), linkCap.capture());
        // Barra dupla não acontece mesmo com base terminando em "/".
        assertThat(linkCap.getValue()).startsWith("https://app.exemplo.com/redefinir-senha?token=");
    }

    @Test
    void resetPassword_tokenValido_trocaSenhaEMarcaComoUsado() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hash-qualquer");
        token.setExpiraEm(OffsetDateTime.now().plusMinutes(10));
        token.setUsado(false);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("senha-nova-123")).thenReturn("hash-novo");

        service.resetPassword("token-cru", "senha-nova-123");

        assertThat(user.getSenhaHash()).isEqualTo("hash-novo");
        assertThat(token.isUsado()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_tokenDesconhecido_lancaInvalidResetToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("nao-existe", "senha-nova-123"))
            .isInstanceOf(InvalidResetTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_tokenJaUsado_lancaInvalidResetToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("h");
        token.setExpiraEm(OffsetDateTime.now().plusMinutes(10));
        token.setUsado(true);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("t", "senha-nova-123"))
            .isInstanceOf(InvalidResetTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_tokenExpirado_lancaInvalidResetToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("h");
        token.setExpiraEm(OffsetDateTime.now().minusMinutes(1));
        token.setUsado(false);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("t", "senha-nova-123"))
            .isInstanceOf(InvalidResetTokenException.class);
        verify(userRepository, never()).save(any());
    }
}
