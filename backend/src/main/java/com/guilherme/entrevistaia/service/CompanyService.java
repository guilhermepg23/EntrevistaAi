package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.dto.CreatedTeamInviteResponse;
import com.guilherme.entrevistaia.dto.PublicTeamInviteResponse;
import com.guilherme.entrevistaia.dto.TeamInviteResponse;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.CompanyInvite;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.CompanyRuleException;
import com.guilherme.entrevistaia.exception.EmailAlreadyInUseException;
import com.guilherme.entrevistaia.mail.TeamInviteMailer;
import com.guilherme.entrevistaia.repository.AssessmentInviteRepository;
import com.guilherme.entrevistaia.repository.AssessmentRepository;
import com.guilherme.entrevistaia.repository.CompanyInviteRepository;
import com.guilherme.entrevistaia.repository.CompanyRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.SecureTokens;
import com.guilherme.entrevistaia.validation.CpfValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Regras de empresa e equipe (v1.1, etapa 2): criar empresa, editar, listar e
// gerenciar membros, convidar recrutadores e aceitar convite.
//
// Invariantes que este service garante:
// - todo recrutador com empresa tem um papel nela (ADMIN ou RECRUTADOR);
// - uma empresa com membros nunca fica sem ADMIN (rebaixar o último ADMIN e
//   excluir a conta dele são barrados com COMPANY_LAST_ADMIN);
// - só ADMIN convida, cancela convite, promove/rebaixa, remove e edita a empresa.
//
// Mesmo cuidado do AccountService: o User que chega dos controllers vem do
// @AuthenticationPrincipal, carregado fora de transação — os métodos re-buscam
// pelo id antes de ler a empresa ou escrever.
@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final CompanyInviteRepository inviteRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentInviteRepository assessmentInviteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeamInviteMailer mailer;
    private final String frontendBaseUrl;

    public CompanyService(CompanyRepository companyRepository,
                          CompanyInviteRepository inviteRepository,
                          AssessmentRepository assessmentRepository,
                          AssessmentInviteRepository assessmentInviteRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          TeamInviteMailer mailer,
                          @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.companyRepository = companyRepository;
        this.inviteRepository = inviteRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentInviteRepository = assessmentInviteRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    // ------------------------------------------------------------ empresa

    // Cadastro de recrutador (AuthController.register): cria a empresa e salva
    // o novo usuário como ADMIN dela — numa transação só, pra nunca sobrar
    // empresa sem dono se o save do usuário falhar.
    @Transactional
    public void fundarEmpresa(User novoRecrutador, String nomeEmpresa, String cnpj) {
        if (nomeEmpresa == null || nomeEmpresa.isBlank()) {
            throw CompanyRuleException.nomeEmpresaObrigatorio();
        }
        Company company = novaEmpresa(nomeEmpresa, cnpj);
        novoRecrutador.setCompany(company);
        novoRecrutador.setPapelEmpresa(PapelEmpresa.ADMIN);
        userRepository.save(novoRecrutador);
        log.info("[COMPANY_FOUNDED] companyId={}", company.getId());
    }

    // POST /company — recrutador que foi removido da equipe (e ficou sem
    // empresa) cria uma nova, da qual vira ADMIN.
    @Transactional
    public Company criarEmpresa(User principal, String nome, String cnpj) {
        User user = carregar(principal);
        if (user.getCompany() != null) {
            throw CompanyRuleException.jaTemEmpresa();
        }
        Company company = novaEmpresa(nome, cnpj);
        user.setCompany(company);
        user.setPapelEmpresa(PapelEmpresa.ADMIN);
        userRepository.save(user);
        log.info("[COMPANY_CREATED] companyId={} adminId={}", company.getId(), user.getId());
        return company;
    }

    // GET /company — a empresa do usuário logado (ou COMPANY_NOT_FOUND).
    @Transactional(readOnly = true)
    public User comEmpresa(User principal) {
        User user = carregar(principal);
        if (user.getCompany() == null) {
            throw CompanyRuleException.semEmpresa();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public long totalMembros(Company company) {
        return userRepository.countByCompany(company);
    }

    // PATCH /company — só ADMIN. cnpj ou descricao vazios/null apagam o salvo.
    @Transactional
    public Company atualizarEmpresa(User principal, String nome, String cnpj, String descricao) {
        User admin = exigirAdmin(principal);
        Company company = admin.getCompany();
        company.setNome(nome.trim());
        company.setCnpj(normalizarCnpj(cnpj));
        company.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
        companyRepository.save(company);
        log.info("[COMPANY_UPDATED] companyId={} por={}", company.getId(), admin.getId());
        return company;
    }

    // ------------------------------------------------------------- equipe

    @Transactional(readOnly = true)
    public List<User> membros(User principal) {
        return userRepository.findByCompanyOrderByNomeAsc(comEmpresa(principal).getCompany());
    }

    // PATCH /company/members/{id}/role — só ADMIN. Rebaixar é permitido (inclusive
    // a si mesmo), desde que sobre pelo menos um ADMIN.
    @Transactional
    public User alterarPapel(User principal, UUID membroId, PapelEmpresa novoPapel) {
        User admin = exigirAdmin(principal);
        User membro = membroDaMesmaEmpresa(admin, membroId);

        if (membro.getPapelEmpresa() == PapelEmpresa.ADMIN && novoPapel != PapelEmpresa.ADMIN
            && userRepository.countByCompanyAndPapelEmpresa(admin.getCompany(), PapelEmpresa.ADMIN) <= 1) {
            throw CompanyRuleException.ultimoAdmin();
        }

        membro.setPapelEmpresa(novoPapel);
        userRepository.save(membro);
        log.info("[COMPANY_ROLE_CHANGED] companyId={} membroId={} papel={} por={}",
            admin.getCompany().getId(), membro.getId(), novoPapel, admin.getId());
        return membro;
    }

    // DELETE /company/members/{id} — só ADMIN, e nunca a si mesmo. Como quem
    // remove é ADMIN e não é o removido, a empresa nunca fica sem ADMIN aqui.
    // O removido continua com a conta, só sem empresa (ver criarEmpresa).
    @Transactional
    public void removerMembro(User principal, UUID membroId) {
        User admin = exigirAdmin(principal);
        if (admin.getId().equals(membroId)) {
            throw CompanyRuleException.naoPodeRemoverASiMesmo();
        }
        User membro = membroDaMesmaEmpresa(admin, membroId);
        membro.setCompany(null);
        membro.setPapelEmpresa(null);
        userRepository.save(membro);
        log.info("[COMPANY_MEMBER_REMOVED] companyId={} membroId={} por={}",
            admin.getCompany().getId(), membro.getId(), admin.getId());
    }

    // Chamado pelo AccountService antes de excluir uma conta de recrutador.
    // - último ADMIN com outros membros: barra (alguém precisa ser promovido antes);
    // - único membro: a empresa (com convites e testes) vai junto;
    // - em qualquer caso, convites e testes criados por ele perdem a referência ao criador.
    @Transactional
    public void antesDeExcluirConta(User user) {
        inviteRepository.desvincularCriador(user);
        assessmentRepository.desvincularCriador(user);
        assessmentInviteRepository.desvincularCriador(user);

        Company company = user.getCompany();
        if (company == null) {
            return;
        }
        long membros = userRepository.countByCompany(company);
        if (membros <= 1) {
            user.setCompany(null);
            userRepository.save(user);
            inviteRepository.deleteByCompany(company);
            assessmentRepository.deleteByCompany(company);
            companyRepository.delete(company);
            log.info("[COMPANY_DELETED_WITH_LAST_MEMBER] companyId={}", company.getId());
            return;
        }
        if (user.getPapelEmpresa() == PapelEmpresa.ADMIN
            && userRepository.countByCompanyAndPapelEmpresa(company, PapelEmpresa.ADMIN) <= 1) {
            throw CompanyRuleException.ultimoAdmin();
        }
    }

    // ------------------------------------------------------------ convites

    // POST /company/invites — só ADMIN. Gera o link, tenta mandar por email e
    // devolve o link de qualquer jeito (os dois jeitos de convidar). Convidar
    // de novo o mesmo email cancela o convite pendente anterior — vale sempre
    // o mais recente.
    @Transactional
    public CreatedTeamInviteResponse convidar(User principal, String email, int validadeDias) {
        User admin = exigirAdmin(principal);
        Company company = admin.getCompany();
        String emailNormalizado = email.trim();

        // Uma conta = um tipo e no máximo uma empresa: quem já tem conta (de
        // candidato ou de outra empresa) não pode entrar por convite.
        if (userRepository.existsByEmail(emailNormalizado)) {
            throw new EmailAlreadyInUseException(emailNormalizado);
        }

        OffsetDateTime agora = OffsetDateTime.now();
        inviteRepository.findByCompanyAndEmailIgnoreCase(company, emailNormalizado).stream()
            .filter(CompanyInvite::pendente)
            .forEach(anterior -> {
                anterior.setCanceladoEm(agora);
                inviteRepository.save(anterior);
            });

        String rawToken = SecureTokens.gerar();
        CompanyInvite invite = new CompanyInvite();
        invite.setCompany(company);
        invite.setEmail(emailNormalizado);
        invite.setTokenHash(SecureTokens.hash(rawToken));
        invite.setCriadoPor(admin);
        invite.setCriadoEm(agora);
        invite.setExpiraEm(agora.plusDays(validadeDias));
        inviteRepository.save(invite);

        String link = frontendBaseUrl.replaceAll("/+$", "") + "/convite-equipe/" + rawToken;
        boolean enviado = mailer.sendTeamInvite(emailNormalizado, company.getNome(), admin.getNome(),
            link, invite.getExpiraEm());

        log.info("[COMPANY_INVITE_CREATED] companyId={} inviteId={} validadeDias={} emailEnviado={}",
            company.getId(), invite.getId(), validadeDias, enviado);
        return new CreatedTeamInviteResponse(TeamInviteResponse.from(invite), link, enviado);
    }

    // GET /company/invites — convites ainda utilizáveis. Qualquer membro vê;
    // só ADMIN age (convidar/cancelar).
    @Transactional(readOnly = true)
    public List<CompanyInvite> convitesPendentes(User principal) {
        Company company = comEmpresa(principal).getCompany();
        return inviteRepository.findByCompanyOrderByCriadoEmDesc(company).stream()
            .filter(CompanyInvite::pendente)
            .toList();
    }

    // DELETE /company/invites/{id} — só ADMIN, só convite da própria empresa.
    @Transactional
    public void cancelarConvite(User principal, UUID inviteId) {
        User admin = exigirAdmin(principal);
        CompanyInvite invite = inviteRepository.findById(inviteId)
            .filter(i -> i.getCompany().getId().equals(admin.getCompany().getId()))
            .filter(CompanyInvite::pendente)
            .orElseThrow(CompanyRuleException::conviteInvalido);
        invite.setCanceladoEm(OffsetDateTime.now());
        inviteRepository.save(invite);
        log.info("[COMPANY_INVITE_CANCELLED] inviteId={} por={}", invite.getId(), admin.getId());
    }

    // GET /auth/team-invites/{token} — público (quem foi convidado ainda não tem conta).
    @Transactional(readOnly = true)
    public PublicTeamInviteResponse conviteParaAceitar(String rawToken) {
        CompanyInvite invite = conviteValido(rawToken);
        return new PublicTeamInviteResponse(invite.getEmail(), invite.getCompany().getNome(),
            invite.getCriadoPor() != null ? invite.getCriadoPor().getNome() : null, invite.getExpiraEm());
    }

    // POST /auth/team-invites/{token}/accept — público. Cria a conta do
    // convidado (recrutador, sem CPF) já dentro da empresa, como RECRUTADOR, e
    // marca o convite como usado. Devolve o usuário salvo pro controller emitir o token.
    @Transactional
    public User aceitarConvite(String rawToken, String nome, String senha) {
        CompanyInvite invite = conviteValido(rawToken);

        // Alguém pode ter criado conta com esse email depois do convite.
        if (userRepository.existsByEmail(invite.getEmail())) {
            throw new EmailAlreadyInUseException(invite.getEmail());
        }

        User user = new User();
        user.setEmail(invite.getEmail());
        user.setNome(nome.trim());
        user.setTipo(TipoConta.RECRUTADOR);
        user.setCompany(invite.getCompany());
        user.setPapelEmpresa(PapelEmpresa.RECRUTADOR);
        user.setCriadoEm(OffsetDateTime.now());
        user.setSenhaHash(passwordEncoder.encode(senha));
        userRepository.save(user);

        invite.setAceitoEm(OffsetDateTime.now());
        inviteRepository.save(invite);

        log.info("[COMPANY_INVITE_ACCEPTED] companyId={} inviteId={} userId={}",
            invite.getCompany().getId(), invite.getId(), user.getId());
        return user;
    }

    // ----------------------------------------------------------- auxiliares

    private Company novaEmpresa(String nome, String cnpj) {
        Company company = new Company();
        company.setNome(nome.trim());
        company.setCnpj(normalizarCnpj(cnpj));
        company.setCriadoEm(OffsetDateTime.now());
        return companyRepository.save(company);
    }

    // @Cnpj já validou os dígitos; aqui só tira a máscara. Vazio vira null.
    private static String normalizarCnpj(String cnpj) {
        String digitos = CpfValidator.stripToDigits(cnpj);
        return digitos.isEmpty() ? null : digitos;
    }

    private CompanyInvite conviteValido(String rawToken) {
        return inviteRepository.findByTokenHash(SecureTokens.hash(rawToken))
            .filter(CompanyInvite::pendente)
            .orElseThrow(CompanyRuleException::conviteInvalido);
    }

    private User carregar(User principal) {
        return userRepository.findById(principal.getId())
            .orElseThrow(CompanyRuleException::semEmpresa);
    }

    private User exigirAdmin(User principal) {
        User user = comEmpresa(principal);
        if (user.getPapelEmpresa() != PapelEmpresa.ADMIN) {
            throw CompanyRuleException.adminObrigatorio();
        }
        return user;
    }

    private User membroDaMesmaEmpresa(User admin, UUID membroId) {
        return userRepository.findById(membroId)
            .filter(m -> m.getCompany() != null && m.getCompany().getId().equals(admin.getCompany().getId()))
            .orElseThrow(CompanyRuleException::membroNaoEncontrado);
    }
}
