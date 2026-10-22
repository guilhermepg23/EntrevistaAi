package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.dto.AssessmentRequest;
import com.guilherme.entrevistaia.dto.AssessmentResponse;
import com.guilherme.entrevistaia.dto.CandidateAssessmentInviteResponse;
import com.guilherme.entrevistaia.dto.CreatedAssessmentInviteResponse;
import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.AssessmentStatus;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.InterviewStatus;
import com.guilherme.entrevistaia.entity.OrigemEntrevista;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.AssessmentRuleException;
import com.guilherme.entrevistaia.mail.AssessmentInviteMailer;
import com.guilherme.entrevistaia.repository.AssessmentInviteRepository;
import com.guilherme.entrevistaia.repository.AssessmentRepository;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.SecureTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Regras de testes de empresa e convites de candidatos, sem Spring: os
// repositórios, o CompanyService e o mailer são mocks.
@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentInviteRepository inviteRepository;
    @Mock private InterviewRepository interviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyService companyService;
    @Mock private AssessmentInviteMailer mailer;

    private AssessmentService service;
    private Company acme;
    private User recrutador;
    private User candidato;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(assessmentRepository, inviteRepository, interviewRepository,
            userRepository, companyService, mailer, "https://app.exemplo.com/", 3);

        acme = new Company();
        acme.setId(UUID.randomUUID());
        acme.setNome("Acme");

        recrutador = new User();
        recrutador.setId(UUID.randomUUID());
        recrutador.setNome("Rita");
        recrutador.setTipo(TipoConta.RECRUTADOR);
        recrutador.setCompany(acme);
        recrutador.setPapelEmpresa(PapelEmpresa.RECRUTADOR);
        lenient().when(companyService.comEmpresa(recrutador)).thenReturn(recrutador);

        candidato = new User();
        candidato.setId(UUID.randomUUID());
        candidato.setEmail("cand@exemplo.com");
        candidato.setTipo(TipoConta.CANDIDATO);
        lenient().when(userRepository.findById(candidato.getId())).thenReturn(Optional.of(candidato));
    }

    private Assessment testeDaAcme() {
        Assessment a = new Assessment();
        a.setId(UUID.randomUUID());
        a.setCompany(acme);
        a.setTitulo("Backend Java");
        a.setStack("Java");
        a.setNivel("pleno");
        a.setTotalPerguntas(5);
        a.setStatus(AssessmentStatus.ATIVO);
        a.setCriadoEm(OffsetDateTime.now());
        a.setExpiraEm(OffsetDateTime.now().plusMonths(3));
        lenient().when(assessmentRepository.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    private AssessmentInvite convite(Assessment a, String email) {
        AssessmentInvite i = new AssessmentInvite();
        i.setId(UUID.randomUUID());
        i.setAssessment(a);
        i.setEmail(email);
        i.setTokenHash(SecureTokens.hash("tok-" + i.getId()));
        i.setCriadoEm(OffsetDateTime.now());
        lenient().when(inviteRepository.findById(i.getId())).thenReturn(Optional.of(i));
        return i;
    }

    private static AssessmentRequest request(int total, List<String> fixas) {
        return new AssessmentRequest(" Backend Java ", "Java", "pleno", total, "  ", fixas);
    }

    // ---- criar / editar / arquivar ----

    @Test
    void criar_deveSalvarTesteDaEmpresaComValidadeDe3MesesEPerguntasFixasEmOrdem() {
        AssessmentResponse resp = service.criar(recrutador, request(5, List.of("P1", " P2 ")));

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(captor.capture());
        Assessment salvo = captor.getValue();
        assertThat(salvo.getCompany()).isEqualTo(acme);
        assertThat(salvo.getCriadoPor()).isEqualTo(recrutador);
        assertThat(salvo.getTitulo()).isEqualTo("Backend Java");
        assertThat(salvo.getDescricaoVaga()).isNull();
        assertThat(salvo.getStatus()).isEqualTo(AssessmentStatus.ATIVO);
        assertThat(salvo.getExpiraEm()).isCloseTo(salvo.getCriadoEm().plusMonths(3), within(1, MINUTES));
        assertThat(salvo.getPerguntasFixas()).extracting("ordem").containsExactly(1, 2);
        assertThat(salvo.getPerguntasFixas()).extracting("pergunta").containsExactly("P1", "P2");
        assertThat(resp.perguntasFixas()).containsExactly("P1", "P2");
        assertThat(resp.editavel()).isTrue();
    }

    @Test
    void criar_deveRecusarMaisPerguntasFixasQueOTotal() {
        assertThatThrownBy(() -> service.criar(recrutador, request(5, List.of("1", "2", "3", "4", "5", "6"))))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("VALIDATION_ERROR");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void detalhe_deveDar404ParaTesteDeOutraEmpresa() {
        Assessment deOutra = testeDaAcme();
        Company outra = new Company();
        outra.setId(UUID.randomUUID());
        deOutra.setCompany(outra);

        assertThatThrownBy(() -> service.detalhe(recrutador, deOutra.getId()))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_NOT_FOUND");
    }

    @Test
    void atualizar_deveBloquearDepoisQueAlguemComecou() {
        Assessment a = testeDaAcme();
        AssessmentInvite aceito = convite(a, "x@exemplo.com");
        aceito.setAceitoEm(OffsetDateTime.now());
        when(inviteRepository.findByAssessmentOrderByCriadoEmDesc(a)).thenReturn(List.of(aceito));

        assertThatThrownBy(() -> service.atualizar(recrutador, a.getId(), request(5, List.of())))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_LOCKED");
    }

    @Test
    void atualizar_deveSubstituirPerguntasFixas() {
        Assessment a = testeDaAcme();
        service.atualizar(recrutador, a.getId(), request(5, List.of("Antiga")));

        service.atualizar(recrutador, a.getId(), request(6, List.of("Nova 1", "Nova 2")));

        assertThat(a.getTotalPerguntas()).isEqualTo(6);
        assertThat(a.getPerguntasFixas()).extracting("pergunta").containsExactly("Nova 1", "Nova 2");
    }

    @Test
    void arquivar_deveEncerrarOTesteEInvalidarConvitesPendentes() {
        Assessment a = testeDaAcme();
        AssessmentInvite pendente = convite(a, "x@exemplo.com");
        assertThat(pendente.pendente()).isTrue();

        AssessmentResponse resp = service.arquivar(recrutador, a.getId());

        assertThat(a.getStatus()).isEqualTo(AssessmentStatus.ARQUIVADO);
        assertThat(resp.aberto()).isFalse();
        assertThat(pendente.pendente()).isFalse();
    }

    // ---- convidar ----

    @Test
    void convidar_deveCriarConviteDevolverLinkETentarEmail() {
        Assessment a = testeDaAcme();
        when(mailer.sendAssessmentInvite(eq("cand@exemplo.com"), eq("Acme"), eq("Backend Java"), anyString(), any()))
            .thenReturn(false);

        CreatedAssessmentInviteResponse resp = service.convidar(recrutador, a.getId(), " cand@exemplo.com ");

        assertThat(resp.link()).startsWith("https://app.exemplo.com/teste/");
        assertThat(resp.emailEnviado()).isFalse();
        assertThat(resp.convite().status()).isEqualTo("PENDENTE");
        ArgumentCaptor<AssessmentInvite> captor = ArgumentCaptor.forClass(AssessmentInvite.class);
        verify(inviteRepository).save(captor.capture());
        String token = resp.link().substring(resp.link().lastIndexOf('/') + 1);
        assertThat(captor.getValue().getTokenHash()).isEqualTo(SecureTokens.hash(token));
        assertThat(captor.getValue().getCriadoPor()).isEqualTo(recrutador);
    }

    @Test
    void convidar_deveCancelarConvitePendenteAnteriorDoMesmoEmail() {
        Assessment a = testeDaAcme();
        AssessmentInvite anterior = convite(a, "cand@exemplo.com");
        when(inviteRepository.findByAssessmentAndEmailIgnoreCase(a, "cand@exemplo.com")).thenReturn(List.of(anterior));

        service.convidar(recrutador, a.getId(), "cand@exemplo.com");

        assertThat(anterior.getCanceladoEm()).isNotNull();
    }

    @Test
    void convidar_deveRecusarQuemJaFezOTeste() {
        Assessment a = testeDaAcme();
        AssessmentInvite aceito = convite(a, "cand@exemplo.com");
        aceito.setAceitoEm(OffsetDateTime.now());
        when(inviteRepository.findByAssessmentAndEmailIgnoreCase(a, "cand@exemplo.com")).thenReturn(List.of(aceito));

        assertThatThrownBy(() -> service.convidar(recrutador, a.getId(), "cand@exemplo.com"))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_ALREADY_TAKEN");
    }

    @Test
    void convidar_deveRecusarEmailDeRecrutador() {
        Assessment a = testeDaAcme();
        User outroRecrutador = new User();
        outroRecrutador.setTipo(TipoConta.RECRUTADOR);
        when(userRepository.findByEmail("rh@exemplo.com")).thenReturn(Optional.of(outroRecrutador));

        assertThatThrownBy(() -> service.convidar(recrutador, a.getId(), "rh@exemplo.com"))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_INVITE_RECRUITER_EMAIL");
    }

    @Test
    void convidar_deveRecusarTesteVencido() {
        Assessment a = testeDaAcme();
        a.setExpiraEm(OffsetDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.convidar(recrutador, a.getId(), "cand@exemplo.com"))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_CLOSED");
    }

    // ---- lado candidato ----

    @Test
    void conviteParaAceitar_deveDevolverDadosDoTestePeloToken() {
        Assessment a = testeDaAcme();
        AssessmentInvite i = convite(a, "cand@exemplo.com");
        when(inviteRepository.findByTokenHash(SecureTokens.hash("tok-" + i.getId()))).thenReturn(Optional.of(i));

        CandidateAssessmentInviteResponse resp = service.conviteParaAceitar("tok-" + i.getId());

        assertThat(resp.inviteId()).isEqualTo(i.getId());
        assertThat(resp.nomeEmpresa()).isEqualTo("Acme");
        assertThat(resp.totalPerguntas()).isEqualTo(5);
        assertThat(resp.status()).isEqualTo("PENDENTE");
    }

    @Test
    void aceitar_deveCriarEntrevistaDeConviteComOsDadosDoTeste() {
        Assessment a = testeDaAcme();
        a.setDescricaoVaga("Vaga X");
        AssessmentInvite i = convite(a, "CAND@exemplo.com");

        Interview interview = service.aceitar(candidato, i.getId());

        assertThat(interview.getOrigem()).isEqualTo(OrigemEntrevista.CONVITE);
        assertThat(interview.getAssessment()).isEqualTo(a);
        assertThat(interview.getUser()).isEqualTo(candidato);
        assertThat(interview.getStack()).isEqualTo("Java");
        assertThat(interview.getTotalPerguntas()).isEqualTo(5);
        assertThat(interview.getDescricaoVaga()).isEqualTo("Vaga X");
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.EM_ANDAMENTO);
        verify(interviewRepository).save(interview);
        assertThat(i.getAceitoEm()).isNotNull();
        assertThat(i.getInterview()).isEqualTo(interview);
    }

    @Test
    void aceitar_deveRecusarContaComOutroEmail() {
        Assessment a = testeDaAcme();
        AssessmentInvite i = convite(a, "outra.pessoa@exemplo.com");

        assertThatThrownBy(() -> service.aceitar(candidato, i.getId()))
            .isInstanceOf(AssessmentRuleException.class)
            .extracting("errorCode").isEqualTo("ASSESSMENT_INVITE_WRONG_ACCOUNT");
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void aceitar_deNovoDevolveAMesmaEntrevista() {
        Assessment a = testeDaAcme();
        AssessmentInvite i = convite(a, "cand@exemplo.com");
        Interview primeira = service.aceitar(candidato, i.getId());

        Interview segunda = service.aceitar(candidato, i.getId());

        assertThat(segunda).isSameAs(primeira);
        verify(interviewRepository).save(any());
    }

    @Test
    void aceitar_deveRecusarConviteCanceladoOuDeTesteArquivado() {
        Assessment a = testeDaAcme();
        AssessmentInvite cancelado = convite(a, "cand@exemplo.com");
        cancelado.setCanceladoEm(OffsetDateTime.now());

        assertThatThrownBy(() -> service.aceitar(candidato, cancelado.getId()))
            .extracting("errorCode").isEqualTo("ASSESSMENT_INVITE_INVALID");

        AssessmentInvite outro = convite(a, "cand@exemplo.com");
        a.setStatus(AssessmentStatus.ARQUIVADO);
        assertThatThrownBy(() -> service.aceitar(candidato, outro.getId()))
            .extracting("errorCode").isEqualTo("ASSESSMENT_INVITE_INVALID");
    }

    @Test
    void aceitar_deveRecusarQuemJaFezOTestePorOutroConvite() {
        Assessment a = testeDaAcme();
        AssessmentInvite i = convite(a, "cand@exemplo.com");
        when(interviewRepository.existsByUserAndAssessment(candidato, a)).thenReturn(true);

        assertThatThrownBy(() -> service.aceitar(candidato, i.getId()))
            .extracting("errorCode").isEqualTo("ASSESSMENT_ALREADY_TAKEN");
    }

    @Test
    void convitesDoCandidato_deveMostrarPendentesEAceitosMasNaoCancelados() {
        Assessment a = testeDaAcme();
        AssessmentInvite pendente = convite(a, "cand@exemplo.com");
        AssessmentInvite cancelado = convite(a, "cand@exemplo.com");
        cancelado.setCanceladoEm(OffsetDateTime.now());
        when(inviteRepository.findByEmailIgnoreCaseOrderByCriadoEmDesc("cand@exemplo.com"))
            .thenReturn(List.of(pendente, cancelado));

        List<CandidateAssessmentInviteResponse> lista = service.convitesDoCandidato(candidato);

        assertThat(lista).extracting(CandidateAssessmentInviteResponse::inviteId).containsExactly(pendente.getId());
    }
}
