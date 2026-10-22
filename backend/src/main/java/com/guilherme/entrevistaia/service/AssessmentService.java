package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.dto.AssessmentInviteResponse;
import com.guilherme.entrevistaia.dto.AssessmentRequest;
import com.guilherme.entrevistaia.dto.AssessmentResponse;
import com.guilherme.entrevistaia.dto.CandidateAssessmentInviteResponse;
import com.guilherme.entrevistaia.dto.CreatedAssessmentInviteResponse;
import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.AssessmentQuestion;
import com.guilherme.entrevistaia.entity.AssessmentStatus;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.InterviewStatus;
import com.guilherme.entrevistaia.entity.OrigemEntrevista;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.AssessmentRuleException;
import com.guilherme.entrevistaia.mail.AssessmentInviteMailer;
import com.guilherme.entrevistaia.repository.AssessmentInviteRepository;
import com.guilherme.entrevistaia.repository.AssessmentRepository;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.SecureTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Testes de entrevista da empresa e convites de candidatos (v1.1, etapa 3).
//
// Lado empresa (/assessments/**): qualquer membro da equipe cria, edita,
// arquiva e convida — o teste é da empresa, não de quem criou.
// Lado candidato: ler o convite pelo link (público), listar os próprios
// convites e aceitar, o que cria a Interview com origem CONVITE.
//
// Regras que este service garante:
// - o teste vale por um prazo fixo a partir da criação (app.assessments.validade-meses);
//   vencido ou arquivado, não aceita convites nem aceites;
// - o convite é pessoal: só a conta de candidato com o email convidado aceita;
// - o candidato faz cada teste uma vez só (não há refazer);
// - depois que alguém começou, o teste não muda mais (resultados comparáveis).
@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentInviteRepository inviteRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final AssessmentInviteMailer mailer;
    private final String frontendBaseUrl;
    private final int validadeMeses;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentInviteRepository inviteRepository,
                             InterviewRepository interviewRepository,
                             UserRepository userRepository,
                             CompanyService companyService,
                             AssessmentInviteMailer mailer,
                             @Value("${app.frontend-base-url}") String frontendBaseUrl,
                             @Value("${app.assessments.validade-meses:3}") int validadeMeses) {
        this.assessmentRepository = assessmentRepository;
        this.inviteRepository = inviteRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.mailer = mailer;
        this.frontendBaseUrl = frontendBaseUrl;
        this.validadeMeses = validadeMeses;
    }

    // ------------------------------------------------------- lado empresa

    // POST /assessments
    @Transactional
    public AssessmentResponse criar(User principal, AssessmentRequest request) {
        User membro = companyService.comEmpresa(principal);
        validarPerguntasFixas(request);

        OffsetDateTime agora = OffsetDateTime.now();
        Assessment assessment = new Assessment();
        assessment.setCompany(membro.getCompany());
        assessment.setCriadoPor(membro);
        assessment.setStatus(AssessmentStatus.ATIVO);
        assessment.setCriadoEm(agora);
        assessment.setExpiraEm(agora.plusMonths(validadeMeses));
        preencher(assessment, request);
        assessmentRepository.save(assessment);

        log.info("[ASSESSMENT_CREATED] assessmentId={} companyId={} por={} perguntasFixas={}",
            assessment.getId(), membro.getCompany().getId(), membro.getId(), assessment.getPerguntasFixas().size());
        return AssessmentResponse.from(assessment, List.of());
    }

    // GET /assessments — os testes da empresa, mais recentes primeiro.
    @Transactional(readOnly = true)
    public List<AssessmentResponse> listar(User principal) {
        User membro = companyService.comEmpresa(principal);
        return assessmentRepository.findByCompanyOrderByCriadoEmDesc(membro.getCompany()).stream()
            .map(a -> AssessmentResponse.from(a, inviteRepository.findByAssessmentOrderByCriadoEmDesc(a)))
            .toList();
    }

    // GET /assessments/{id}
    @Transactional(readOnly = true)
    public AssessmentResponse detalhe(User principal, UUID assessmentId) {
        Assessment assessment = daEmpresa(principal, assessmentId);
        return AssessmentResponse.from(assessment, inviteRepository.findByAssessmentOrderByCriadoEmDesc(assessment));
    }

    // PUT /assessments/{id} — só enquanto o teste está aberto e ninguém começou.
    // A validade não muda com a edição: continua contando da criação.
    @Transactional
    public AssessmentResponse atualizar(User principal, UUID assessmentId, AssessmentRequest request) {
        Assessment assessment = daEmpresa(principal, assessmentId);
        if (!assessment.aberto()) {
            throw AssessmentRuleException.encerrado();
        }
        List<AssessmentInvite> convites = inviteRepository.findByAssessmentOrderByCriadoEmDesc(assessment);
        if (convites.stream().anyMatch(c -> c.getAceitoEm() != null)) {
            throw AssessmentRuleException.bloqueadoParaEdicao();
        }
        validarPerguntasFixas(request);

        preencher(assessment, request);
        assessmentRepository.save(assessment);
        log.info("[ASSESSMENT_UPDATED] assessmentId={} por={}", assessment.getId(), principal.getId());
        return AssessmentResponse.from(assessment, convites);
    }

    // POST /assessments/{id}/archive — encerra o teste. Os convites pendentes
    // param de valer (AssessmentInvite.pendente olha o status do teste); quem já
    // começou pode terminar, e os resultados continuam visíveis. Idempotente.
    @Transactional
    public AssessmentResponse arquivar(User principal, UUID assessmentId) {
        Assessment assessment = daEmpresa(principal, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.ARQUIVADO) {
            assessment.setStatus(AssessmentStatus.ARQUIVADO);
            assessmentRepository.save(assessment);
            log.info("[ASSESSMENT_ARCHIVED] assessmentId={} por={}", assessment.getId(), principal.getId());
        }
        return AssessmentResponse.from(assessment, inviteRepository.findByAssessmentOrderByCriadoEmDesc(assessment));
    }

    // GET /assessments/{id}/invites — todos os convites do teste (pendentes,
    // aceitos, cancelados e vencidos), mais recentes primeiro.
    @Transactional(readOnly = true)
    public List<AssessmentInviteResponse> convites(User principal, UUID assessmentId) {
        Assessment assessment = daEmpresa(principal, assessmentId);
        return inviteRepository.findByAssessmentOrderByCriadoEmDesc(assessment).stream()
            .map(AssessmentInviteResponse::from)
            .toList();
    }

    // POST /assessments/{id}/invites — gera o link, tenta mandar por email e
    // devolve o link de qualquer jeito. Convidar de novo o mesmo email cancela
    // o convite pendente anterior (vale o mais recente), mas quem já aceitou um
    // convite deste teste não pode ser convidado de novo: não há refazer.
    @Transactional
    public CreatedAssessmentInviteResponse convidar(User principal, UUID assessmentId, String email) {
        User membro = companyService.comEmpresa(principal);
        Assessment assessment = daEmpresaDoMembro(membro, assessmentId);
        if (!assessment.aberto()) {
            throw AssessmentRuleException.encerrado();
        }
        String emailNormalizado = email.trim();

        // Uma conta = um tipo: recrutador não faz teste. Email sem conta pode
        // ser convidado — a pessoa cria a conta de candidato pelo link.
        userRepository.findByEmail(emailNormalizado)
            .filter(u -> u.getTipo() == TipoConta.RECRUTADOR)
            .ifPresent(u -> { throw AssessmentRuleException.emailDeRecrutador(); });

        List<AssessmentInvite> anteriores = inviteRepository.findByAssessmentAndEmailIgnoreCase(assessment, emailNormalizado);
        if (anteriores.stream().anyMatch(c -> c.getAceitoEm() != null)) {
            throw AssessmentRuleException.jaRealizado();
        }

        OffsetDateTime agora = OffsetDateTime.now();
        anteriores.stream()
            .filter(AssessmentInvite::pendente)
            .forEach(anterior -> {
                anterior.setCanceladoEm(agora);
                inviteRepository.save(anterior);
            });

        String rawToken = SecureTokens.gerar();
        AssessmentInvite invite = new AssessmentInvite();
        invite.setAssessment(assessment);
        invite.setEmail(emailNormalizado);
        invite.setTokenHash(SecureTokens.hash(rawToken));
        invite.setCriadoPor(membro);
        invite.setCriadoEm(agora);
        inviteRepository.save(invite);

        String link = frontendBaseUrl.replaceAll("/+$", "") + "/teste/" + rawToken;
        boolean enviado = mailer.sendAssessmentInvite(emailNormalizado, assessment.getCompany().getNome(),
            assessment.getTitulo(), link, assessment.getExpiraEm());

        log.info("[ASSESSMENT_INVITE_CREATED] assessmentId={} inviteId={} emailEnviado={}",
            assessment.getId(), invite.getId(), enviado);
        return new CreatedAssessmentInviteResponse(AssessmentInviteResponse.from(invite), link, enviado);
    }

    // DELETE /assessments/{id}/invites/{inviteId} — só convite pendente.
    @Transactional
    public void cancelarConvite(User principal, UUID assessmentId, UUID inviteId) {
        Assessment assessment = daEmpresa(principal, assessmentId);
        AssessmentInvite invite = inviteRepository.findById(inviteId)
            .filter(i -> i.getAssessment().getId().equals(assessment.getId()))
            .filter(AssessmentInvite::pendente)
            .orElseThrow(AssessmentRuleException::conviteInvalido);
        invite.setCanceladoEm(OffsetDateTime.now());
        inviteRepository.save(invite);
        log.info("[ASSESSMENT_INVITE_CANCELLED] inviteId={} por={}", invite.getId(), principal.getId());
    }

    // ----------------------------------------------------- lado candidato

    // GET /auth/assessment-invites/{token} — público: quem abre o link pode
    // ainda não ter conta. Mostra a empresa, o teste e o email do convite (pra
    // tela de cadastro já vir com ele).
    @Transactional(readOnly = true)
    public CandidateAssessmentInviteResponse conviteParaAceitar(String rawToken) {
        AssessmentInvite invite = inviteRepository.findByTokenHash(SecureTokens.hash(rawToken))
            .filter(AssessmentInvite::pendente)
            .orElseThrow(AssessmentRuleException::conviteInvalido);
        return CandidateAssessmentInviteResponse.from(invite);
    }

    // GET /interviews/invites — convites endereçados ao email do candidato:
    // os pendentes e os já aceitos (pra retomar ou ver que terminou). Cancelados
    // e vencidos sem aceite somem da lista.
    @Transactional(readOnly = true)
    public List<CandidateAssessmentInviteResponse> convitesDoCandidato(User principal) {
        return inviteRepository.findByEmailIgnoreCaseOrderByCriadoEmDesc(principal.getEmail()).stream()
            .filter(i -> i.getAceitoEm() != null || i.pendente())
            .map(CandidateAssessmentInviteResponse::from)
            .toList();
    }

    // POST /interviews/invites/{inviteId}/accept — cria a entrevista do teste.
    // O id do convite não é segredo (aparece na página pública do link); o que
    // protege o aceite é o email da conta bater com o do convite.
    //
    // Idempotente para o próprio dono: aceitar de novo um convite já aceito
    // (duplo clique, link aberto outra vez) devolve a mesma entrevista.
    @Transactional
    public Interview aceitar(User principal, UUID inviteId) {
        User candidato = userRepository.findById(principal.getId())
            .orElseThrow(AssessmentRuleException::conviteInvalido);
        AssessmentInvite invite = inviteRepository.findById(inviteId)
            .orElseThrow(AssessmentRuleException::conviteInvalido);

        if (!invite.getEmail().equalsIgnoreCase(candidato.getEmail())) {
            log.warn("[ASSESSMENT_INVITE_WRONG_ACCOUNT] inviteId={} userId={}", invite.getId(), candidato.getId());
            throw AssessmentRuleException.conviteDeOutraConta();
        }

        if (invite.getAceitoEm() != null) {
            if (invite.getInterview() != null && invite.getInterview().getUser().getId().equals(candidato.getId())) {
                return invite.getInterview();
            }
            throw AssessmentRuleException.conviteInvalido();
        }
        if (!invite.pendente()) {
            throw AssessmentRuleException.conviteInvalido();
        }

        Assessment assessment = invite.getAssessment();
        // Defesa extra contra refazer: o convite é único por email e teste, mas
        // a conta pode ter feito o teste por um convite anterior.
        if (interviewRepository.existsByUserAndAssessment(candidato, assessment)) {
            throw AssessmentRuleException.jaRealizado();
        }

        OffsetDateTime agora = OffsetDateTime.now();
        Interview interview = new Interview();
        interview.setUser(candidato);
        interview.setOrigem(OrigemEntrevista.CONVITE);
        interview.setAssessment(assessment);
        interview.setStack(assessment.getStack());
        interview.setNivel(assessment.getNivel());
        interview.setTotalPerguntas(assessment.getTotalPerguntas());
        interview.setDescricaoVaga(assessment.getDescricaoVaga());
        interview.setStatus(InterviewStatus.EM_ANDAMENTO);
        interview.setCriadoEm(agora);
        interviewRepository.save(interview);

        invite.setAceitoEm(agora);
        invite.setInterview(interview);
        inviteRepository.save(invite);

        log.info("[ASSESSMENT_INVITE_ACCEPTED] assessmentId={} inviteId={} interviewId={} userId={}",
            assessment.getId(), invite.getId(), interview.getId(), candidato.getId());
        return interview;
    }

    // ----------------------------------------------------------- auxiliares

    private Assessment daEmpresa(User principal, UUID assessmentId) {
        return daEmpresaDoMembro(companyService.comEmpresa(principal), assessmentId);
    }

    // membro: já carregado por companyService.comEmpresa (tem empresa).
    private Assessment daEmpresaDoMembro(User membro, UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
            .filter(a -> a.getCompany().getId().equals(membro.getCompany().getId()))
            .orElseThrow(AssessmentRuleException::naoEncontrado);
    }

    private static void validarPerguntasFixas(AssessmentRequest request) {
        if (request.perguntasFixas() != null && request.perguntasFixas().size() > request.totalPerguntas()) {
            throw AssessmentRuleException.perguntasFixasDemais(request.totalPerguntas());
        }
    }

    // Copia os campos editáveis. As perguntas fixas são substituídas por inteiro
    // (orphanRemoval apaga as antigas).
    private static void preencher(Assessment assessment, AssessmentRequest request) {
        assessment.setTitulo(request.titulo().trim());
        assessment.setStack(request.stack().trim());
        assessment.setNivel(request.nivel().trim());
        assessment.setTotalPerguntas(request.totalPerguntas());
        assessment.setDescricaoVaga(request.descricaoVaga() == null || request.descricaoVaga().isBlank()
            ? null : request.descricaoVaga().trim());

        assessment.getPerguntasFixas().clear();
        List<String> perguntas = request.perguntasFixas() == null ? List.of() : request.perguntasFixas();
        for (int i = 0; i < perguntas.size(); i++) {
            AssessmentQuestion q = new AssessmentQuestion();
            q.setAssessment(assessment);
            q.setOrdem(i + 1);
            q.setPergunta(perguntas.get(i).trim());
            assessment.getPerguntasFixas().add(q);
        }
    }
}
