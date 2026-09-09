package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.PasswordResetTokenRepository;
import com.guilherme.entrevistaia.repository.ResumeReviewRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ResumeReviewRepository resumeReviewRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    private AccountService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountService(userRepository, passwordEncoder,
            resumeReviewRepository, passwordResetTokenRepository);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ana@teste.com");
        user.setNome("Ana");
        user.setSenhaHash("hash-atual");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void updateNome_alteraESalva() {
        User out = service.updateNome(user, "Ana Paula");

        assertThat(out.getNome()).isEqualTo("Ana Paula");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_senhaAtualCorreta_troca() {
        when(passwordEncoder.matches("hash-atual-plano", "hash-atual")).thenReturn(true);
        when(passwordEncoder.encode("nova-senha-123")).thenReturn("hash-novo");

        service.changePassword(user, "hash-atual-plano", "nova-senha-123");

        assertThat(user.getSenhaHash()).isEqualTo("hash-novo");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_senhaAtualErrada_lancaInvalidCredentialsENaoSalva() {
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(user, "errada", "nova-senha-123"))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(user);
    }

    @Test
    void deleteAccount_apagaDependenciasSemCascadeAntesDoUsuario() {
        service.deleteAccount(user);

        InOrder ordem = inOrder(resumeReviewRepository, passwordResetTokenRepository, userRepository);
        ordem.verify(resumeReviewRepository).deleteByUser(user);
        ordem.verify(passwordResetTokenRepository).deleteByUser(user);
        ordem.verify(userRepository).delete(user);
    }
}
