package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.exception.CompanyRuleException;
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
import static org.mockito.Mockito.doThrow;
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
    @Mock private CompanyService companyService;
    @Mock private ImageService imageService;

    private AccountService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountService(userRepository, passwordEncoder,
            resumeReviewRepository, passwordResetTokenRepository, companyService, imageService);
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

        InOrder ordem = inOrder(companyService, resumeReviewRepository, passwordResetTokenRepository, userRepository);
        ordem.verify(companyService).antesDeExcluirConta(user);
        ordem.verify(resumeReviewRepository).deleteByUser(user);
        ordem.verify(passwordResetTokenRepository).deleteByUser(user);
        ordem.verify(userRepository).delete(user);
    }

    // Último ADMIN de uma equipe com outros membros: a regra de empresa barra
    // ANTES de qualquer exclusão — nada fica apagado pela metade.
    @Test
    void deleteAccount_ultimoAdminNaoApagaNada() {
        doThrow(CompanyRuleException.ultimoAdmin()).when(companyService).antesDeExcluirConta(user);

        assertThatThrownBy(() -> service.deleteAccount(user))
            .isInstanceOf(CompanyRuleException.class);

        verify(resumeReviewRepository, never()).deleteByUser(user);
        verify(userRepository, never()).delete(user);
    }

    // ---- foto (v1.2, etapa 3) ----

    private Imagem imagem() {
        Imagem i = new Imagem();
        i.setId(UUID.randomUUID());
        return i;
    }

    @Test
    void trocarFoto_salvaANovaAntesDeApagarAAntiga() {
        user.setTipo(TipoConta.RECRUTADOR);
        UUID antiga = UUID.randomUUID();
        user.setFotoId(antiga);
        Imagem nova = imagem();
        when(imageService.salvar(new byte[] {1, 2})).thenReturn(nova);

        User out = service.trocarFoto(user, new byte[] {1, 2});

        assertThat(out.getFotoId()).isEqualTo(nova.getId());
        InOrder ordem = inOrder(imageService, userRepository);
        ordem.verify(imageService).salvar(new byte[] {1, 2});
        ordem.verify(userRepository).save(user);
        ordem.verify(imageService).apagar(antiga);
    }

    @Test
    void trocarFoto_imagemRecusadaMantemAAtual() {
        user.setTipo(TipoConta.RECRUTADOR);
        UUID atual = UUID.randomUUID();
        user.setFotoId(atual);
        when(imageService.salvar(new byte[] {9})).thenThrow(ImageRuleException.formatoInvalido());

        assertThatThrownBy(() -> service.trocarFoto(user, new byte[] {9}))
            .isInstanceOf(ImageRuleException.class);

        assertThat(user.getFotoId()).isEqualTo(atual);
        verify(imageService, never()).apagar(atual);
    }

    @Test
    void trocarFoto_candidatoNaoTemFoto() {
        user.setTipo(TipoConta.CANDIDATO);

        assertThatThrownBy(() -> service.trocarFoto(user, new byte[] {1}))
            .extracting("errorCode").isEqualTo("ACCOUNT_PHOTO_RECRUITER_ONLY");

        verify(imageService, never()).salvar(new byte[] {1});
    }

    @Test
    void removerFoto_desvinculaEApaga() {
        user.setTipo(TipoConta.RECRUTADOR);
        UUID antiga = UUID.randomUUID();
        user.setFotoId(antiga);

        User out = service.removerFoto(user);

        assertThat(out.getFotoId()).isNull();
        verify(userRepository).save(user);
        verify(imageService).apagar(antiga);
    }

    @Test
    void deleteAccount_apagaAFotoDepoisDoUsuario() {
        UUID foto = UUID.randomUUID();
        user.setFotoId(foto);

        service.deleteAccount(user);

        InOrder ordem = inOrder(userRepository, imageService);
        ordem.verify(userRepository).delete(user);
        ordem.verify(imageService).apagar(foto);
    }
}
