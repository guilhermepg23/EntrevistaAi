package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.User;
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

    public AccountService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ResumeReviewRepository resumeReviewRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resumeReviewRepository = resumeReviewRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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

    // DELETE /account — apaga a conta e tudo que pendura nela. Interviews (com
    // perguntas, respostas, relatório e análise de currículo da entrevista) são
    // removidas por cascade do User; ResumeReview e PasswordResetToken têm FK
    // pra User sem cascade, então são apagados aqui antes.
    @Transactional
    public void deleteAccount(User principal) {
        User user = carregar(principal.getId());
        resumeReviewRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("[ACCOUNT_DELETED] userId={}", user.getId());
    }

    private User carregar(UUID id) {
        // O usuário está autenticado (token válido carregado pelo filtro), então
        // ele existe — mas re-buscar garante uma entidade gerenciada nesta transação.
        return userRepository.findById(id)
            .orElseThrow(InvalidCredentialsException::new);
    }
}
