package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.PasswordResetTokenRepository;
import com.guilherme.entrevistaia.repository.ResumeReviewRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Regras da tela "Minha conta" (GET/PATCH /account, POST /account/change-password,
// DELETE /account). O User que chega dos controllers vem do
// @AuthenticationPrincipal (carregado pelo JwtAuthenticationFilter, fora de
// transação) — por isso os métodos que escrevem re-buscam a entidade pelo id
// dentro da própria transação antes de mexer.
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResumeReviewRepository resumeReviewRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CompanyService companyService;
    private final ImageService imageService;

    public AccountService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ResumeReviewRepository resumeReviewRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           CompanyService companyService,
                           ImageService imageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resumeReviewRepository = resumeReviewRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.companyService = companyService;
        this.imageService = imageService;
    }

    // PATCH /account — por ora só o nome. Devolve o User já atualizado pro
    // controller montar o AccountResponse.
    @Transactional
    public User updateNome(User principal, String novoNome) {
        User user = carregar(principal.getId());
        user.setNome(novoNome);
        userRepository.save(user);
        log.info("[ACCOUNT_NAME_UPDATED] userId={}", user.getId());
        return user;
    }

    // POST /account/change-password — troca com o usuário logado, exigindo a
    // senha atual. Senha atual errada -> 401 (mesmo errorCode do login).
    @Transactional
    public void changePassword(User principal, String senhaAtual, String novaSenha) {
        User user = carregar(principal.getId());
        if (!passwordEncoder.matches(senhaAtual, user.getSenhaHash())) {
            log.info("[ACCOUNT_PASSWORD_CHANGE_DENIED] userId={} motivo=senha_atual_incorreta", user.getId());
            throw new InvalidCredentialsException();
        }
        user.setSenhaHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
        log.info("[ACCOUNT_PASSWORD_CHANGED] userId={}", user.getId());
    }

    // PUT /account/foto — só recrutador (a foto aparece para a equipe; candidato
    // não tem foto, para ela não chegar à empresa junto com o teste). Salva a
    // imagem nova antes de apagar a antiga: se a nova for recusada, a atual fica.
    @Transactional
    public User trocarFoto(User principal, byte[] conteudo) {
        User user = carregarRecrutador(principal);
        UUID antiga = user.getFotoId();
        user.setFotoId(imageService.salvar(conteudo).getId());
        userRepository.save(user);
        imageService.apagar(antiga);
        log.info("[ACCOUNT_PHOTO_UPDATED] userId={}", user.getId());
        return user;
    }

    // DELETE /account/foto — sem foto, não faz nada.
    @Transactional
    public User removerFoto(User principal) {
        User user = carregarRecrutador(principal);
        UUID antiga = user.getFotoId();
        if (antiga != null) {
            user.setFotoId(null);
            userRepository.save(user);
            imageService.apagar(antiga);
            log.info("[ACCOUNT_PHOTO_REMOVED] userId={}", user.getId());
        }
        return user;
    }

    private User carregarRecrutador(User principal) {
        User user = carregar(principal.getId());
        if (user.getTipo() != TipoConta.RECRUTADOR) {
            throw ImageRuleException.fotoSoRecrutador();
        }
        return user;
    }

    // DELETE /account — apaga a conta e tudo que pendura nela. Interviews (com
    // perguntas, respostas, relatório e análise de currículo da entrevista) são
    // removidas por cascade do User; ResumeReview e PasswordResetToken têm FK
    // pra User sem cascade, então são apagados aqui antes.
    @Transactional
    public void deleteAccount(User principal) {
        User user = carregar(principal.getId());
        // Recrutador: barra se for o último ADMIN de uma equipe com outros
        // membros (COMPANY_LAST_ADMIN, 409); se for o único membro, a empresa
        // vai junto. Roda antes de apagar qualquer coisa, pra o bloqueio não
        // deixar a exclusão pela metade.
        companyService.antesDeExcluirConta(user);
        resumeReviewRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        imageService.apagar(user.getFotoId());
        log.info("[ACCOUNT_DELETED] userId={}", user.getId());
    }

    private User carregar(UUID id) {
        // O usuário está autenticado (token válido carregado pelo filtro), então
        // ele existe — mas re-buscar garante uma entidade gerenciada nesta transação.
        return userRepository.findById(id)
            .orElseThrow(InvalidCredentialsException::new);
    }
}
