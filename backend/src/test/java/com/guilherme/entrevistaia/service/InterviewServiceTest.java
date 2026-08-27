package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.ai.*;
import com.guilherme.entrevistaia.entity.*;
import com.guilherme.entrevistaia.exception.*;
import com.guilherme.entrevistaia.repository.AnswerRepository;
import com.guilherme.entrevistaia.repository.FeedbackReportRepository;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.QuestionRepository;
import com.guilherme.entrevistaia.repository.ResumeAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Testa a regra de negócio central da aplicação: transições de estado da
// entrevista, ownership e os limites de perguntas. Repositories e os clients
// de IA são mockados (Mockito) porque aqui só nos interessa a lógica do
// service em si, não infraestrutura (banco, HTTP pra OpenAI).
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private FeedbackReportRepository reportRepository;
    @Mock private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock private AiQuestionGenerator questionGenerator;
    @Mock private AiAnswerEvaluator answerEvaluator;
    @Mock private AiReportGenerator reportGenerator;
    @Mock private ResumeTextExtractor resumeTextExtractor;
    @Mock private AiResumeAnalyzer resumeAnalyzer;
    @Mock private QuestionPromptBuilder questionPromptBuilder;

    private InterviewService service;

    private User owner;
    private User outroUsuario;

    @BeforeEach
    void setUp() {
        service = new InterviewService(interviewRepository, questionRepository, answerRepository,
            reportRepository, resumeAnalysisRepository, questionGenerator, answerEvaluator, reportGenerator,
            resumeTextExtractor, resumeAnalyzer, questionPromptBuilder);

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("dono@teste.com");

        outroUsuario = new User();
        outroUsuario.setId(UUID.randomUUID());
        outroUsuario.setEmail("outro@teste.com");
    }

    private Interview novaInterview(InterviewStatus status, int totalPerguntas) {
        Interview interview = new Interview();
        interview.setId(UUID.randomUUID());
        interview.setUser(owner);
        interview.setStack("Java");
        interview.setNivel("junior");
        interview.setStatus(status);
        interview.setTotalPerguntas(totalPerguntas);
        return interview;
    }

    // ---- startInterview ----

    @Test
    void startInterview_deveCriarEntrevistaEmAndamentoComDadosInformados() {
        Interview criada = service.startInterview(owner, "Java", "pleno", 10, "Vaga de Java pleno", "Kubernetes, testes");

        assertThat(criada.getUser()).isEqualTo(owner);
        assertThat(criada.getStack()).isEqualTo("Java");
        assertThat(criada.getNivel()).isEqualTo("pleno");
        assertThat(criada.getDescricaoVaga()).isEqualTo("Vaga de Java pleno");
        assertThat(criada.getFocoPratica()).isEqualTo("Kubernetes, testes");
        assertThat(criada.getTotalPerguntas()).isEqualTo(10);
        assertThat(criada.getStatus()).isEqualTo(InterviewStatus.EM_ANDAMENTO);
        assertThat(criada.getCriadoEm()).isNotNull();
        verify(interviewRepository).save(criada);
    }

    // ---- getNextQuestion ----

    @Test
    void getNextQuestion_deveGerarPrimeiraPerguntaEQuandoNaoHaHistorico() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        when(questionGenerator.generate(interview, 1))
            .thenReturn(new AiQuestionResult("Pergunta 1", "JVM", Dificuldade.BASICO, "motivo"));

        Question question = service.getNextQuestion(interview.getId(), owner);

        assertThat(question.getOrdem()).isEqualTo(1);
        assertThat(question.getPergunta()).isEqualTo("Pergunta 1");
        assertThat(question.getDificuldade()).isEqualTo(Dificuldade.BASICO);
        verify(questionRepository).save(question);
    }

    @Test
    void getNextQuestion_deveLancar404QuandoEntrevistaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(interviewRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNextQuestion(id, owner))
            .isInstanceOf(InterviewNotFoundException.class);
    }

    @Test
    void getNextQuestion_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getNextQuestion(interview.getId(), outroUsuario))
            .isInstanceOf(InterviewAccessDeniedException.class);
        verifyNoInteractions(questionGenerator);
    }

    @Test
    void getNextQuestion_deveLancarEstadoInvalidoQuandoEntrevistaJaFinalizada() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getNextQuestion(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
        verifyNoInteractions(questionGenerator);
    }

    @Test
    void getNextQuestion_deveLancarEstadoInvalidoQuandoTotalDePerguntasJaFoiAtingido() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 1);
        Question jaExistente = new Question();
        jaExistente.setOrdem(1);
        interview.getQuestions().add(jaExistente);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getNextQuestion(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
        verifyNoInteractions(questionGenerator);
    }

    // ---- prepareNextQuestionPrompt / persistStreamedQuestion (caminho de streaming) ----

    @Test
    void prepareNextQuestionPrompt_deveMontarContextoComOrdemEPromptCorretos() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        when(questionPromptBuilder.build(interview, 1)).thenReturn("prompt montado");

        InterviewService.NextQuestionPromptContext ctx =
            service.prepareNextQuestionPrompt(interview.getId(), owner);

        assertThat(ctx.numeroAtual()).isEqualTo(1);
        assertThat(ctx.userPrompt()).isEqualTo("prompt montado");
    }

    @Test
    void prepareNextQuestionPrompt_deveLancarEstadoInvalidoQuandoEntrevistaJaFinalizada() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.prepareNextQuestionPrompt(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
        verifyNoInteractions(questionPromptBuilder);
    }

    @Test
    void prepareNextQuestionPrompt_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.prepareNextQuestionPrompt(interview.getId(), outroUsuario))
            .isInstanceOf(InterviewAccessDeniedException.class);
        verifyNoInteractions(questionPromptBuilder);
    }

    @Test
    void persistStreamedQuestion_deveSalvarPerguntaComResultadoJaPronto() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        AiQuestionResult result = new AiQuestionResult("Pergunta via stream", "JVM", Dificuldade.BASICO, null);

        Question question = service.persistStreamedQuestion(interview.getId(), 1, result);

        assertThat(question.getPergunta()).isEqualTo("Pergunta via stream");
        assertThat(question.getOrdem()).isEqualTo(1);
        assertThat(question.getDificuldade()).isEqualTo(Dificuldade.BASICO);
        verify(questionRepository).save(question);
    }

    @Test
    void persistStreamedQuestion_deveLancar404QuandoEntrevistaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(interviewRepository.findById(id)).thenReturn(Optional.empty());
        AiQuestionResult result = new AiQuestionResult("pergunta", "topico", Dificuldade.BASICO, null);

        assertThatThrownBy(() -> service.persistStreamedQuestion(id, 1, result))
            .isInstanceOf(InterviewNotFoundException.class);
    }

    // ---- submitAnswer ----

    @Test
    void submitAnswer_naoDeveFinalizarEntrevistaQuandoAindaHaPerguntasRestantes() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(answerEvaluator.evaluate(question, "minha resposta"))
            .thenReturn(new AiEvaluationResult(8, "bom", List.of("ponto forte"), List.of("gap"), NivelDominio.INTERMEDIARIO, "resposta modelo"));

        Answer answer = service.submitAnswer(question.getId(), owner, "minha resposta");

        assertThat(answer.getNota()).isEqualTo(8);
        assertThat(answer.getRespostaModelo()).isEqualTo("resposta modelo");
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.EM_ANDAMENTO);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void submitAnswer_deveFinalizarEntrevistaQuandoEraUltimaPergunta() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 1);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(answerEvaluator.evaluate(question, "minha resposta"))
            .thenReturn(new AiEvaluationResult(9, "otimo", List.of(), List.of(), NivelDominio.AVANCADO, "resposta modelo"));

        service.submitAnswer(question.getId(), owner, "minha resposta");

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.FINALIZADA);
        assertThat(interview.getFinalizadoEm()).isNotNull();
        verify(interviewRepository).save(interview);
    }

    @Test
    void submitAnswer_deveLancar404QuandoPerguntaNaoExiste() {
        UUID questionId = UUID.randomUUID();
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitAnswer(questionId, owner, "resposta"))
            .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void submitAnswer_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.submitAnswer(question.getId(), outroUsuario, "resposta"))
            .isInstanceOf(InterviewAccessDeniedException.class);
        verifyNoInteractions(answerEvaluator);
    }

    @Test
    void submitAnswer_deveLancarEstadoInvalidoQuandoPerguntaJaFoiRespondida() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        question.setAnswer(new Answer());
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.submitAnswer(question.getId(), owner, "resposta"))
            .isInstanceOf(InvalidInterviewStateException.class);
        verifyNoInteractions(answerEvaluator);
    }

    // ---- abandonInterview ----

    @Test
    void abandonInterview_deveMarcarComoAbandonadaQuandoEmAndamento() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        Interview resultado = service.abandonInterview(interview.getId(), owner);

        assertThat(resultado.getStatus()).isEqualTo(InterviewStatus.ABANDONADA);
        assertThat(resultado.getFinalizadoEm()).isNotNull();
        verify(interviewRepository).save(interview);
    }

    @Test
    void abandonInterview_deveLancarEstadoInvalidoQuandoJaFinalizada() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.abandonInterview(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void abandonInterview_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.abandonInterview(interview.getId(), outroUsuario))
            .isInstanceOf(InterviewAccessDeniedException.class);
        verify(interviewRepository, never()).save(any());
    }

    // ---- getOrGenerateReport ----

    @Test
    void getOrGenerateReport_deveReaproveitarRelatorioJaExistenteSemChamarIaDeNovo() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        FeedbackReport reportExistente = new FeedbackReport();
        interview.setReport(reportExistente);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        FeedbackReport resultado = service.getOrGenerateReport(interview.getId(), owner);

        assertThat(resultado).isEqualTo(reportExistente);
        verifyNoInteractions(reportGenerator);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void getOrGenerateReport_deveGerarNovoRelatorioQuandoEntrevistaFinalizadaSemRelatorio() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        when(reportGenerator.generate(interview)).thenReturn(new AiReportResult(
            8, "resumo executivo", List.of("forte"), List.of("fraco"), List.of("estudar X"),
            NivelPercebido.PLENO, Recomendacao.APROVADO));

        FeedbackReport report = service.getOrGenerateReport(interview.getId(), owner);

        assertThat(report.getNotaGeral()).isEqualTo(8);
        assertThat(report.getRecomendacao()).isEqualTo(Recomendacao.APROVADO);
        verify(reportRepository).save(report);
    }

    @Test
    void getOrGenerateReport_deveLancarEstadoInvalidoQuandoEntrevistaAindaEmAndamento() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getOrGenerateReport(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
        verifyNoInteractions(reportGenerator);
    }

    // ---- getInterview ----

    @Test
    void getInterview_deveDevolverEntrevistaQuandoDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThat(service.getInterview(interview.getId(), owner)).isEqualTo(interview);
    }

    @Test
    void getInterview_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getInterview(interview.getId(), outroUsuario))
            .isInstanceOf(InterviewAccessDeniedException.class);
    }

    // ---- getOrCreateShareToken / revokeShareToken / getReportByShareToken ----

    @Test
    void getOrCreateShareToken_deveGerarTokenQuandoEntrevistaFinalizada() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        String token = service.getOrCreateShareToken(interview.getId(), owner);

        assertThat(token).isNotBlank();
        assertThat(interview.getShareToken()).isEqualTo(token);
        verify(interviewRepository).save(interview);
    }

    @Test
    void getOrCreateShareToken_deveReaproveitarTokenJaExistente() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        interview.setShareToken("token-existente");
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        String token = service.getOrCreateShareToken(interview.getId(), owner);

        assertThat(token).isEqualTo("token-existente");
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void getOrCreateShareToken_deveLancarEstadoInvalidoQuandoEntrevistaNaoFinalizada() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getOrCreateShareToken(interview.getId(), owner))
            .isInstanceOf(InvalidInterviewStateException.class);
    }

    @Test
    void revokeShareToken_deveLimparToken() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        interview.setShareToken("token-existente");
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        service.revokeShareToken(interview.getId(), owner);

        assertThat(interview.getShareToken()).isNull();
        verify(interviewRepository).save(interview);
    }

    @Test
    void getReportByShareToken_deveDevolverRelatorioQuandoTokenValido() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 3);
        FeedbackReport report = new FeedbackReport();
        report.setInterview(interview);
        interview.setReport(report);
        interview.setShareToken("token-valido");
        when(interviewRepository.findByShareToken("token-valido")).thenReturn(Optional.of(interview));

        assertThat(service.getReportByShareToken("token-valido")).isEqualTo(report);
    }

    @Test
    void getReportByShareToken_deveLancarNotFoundQuandoTokenNaoExiste() {
        when(interviewRepository.findByShareToken("invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReportByShareToken("invalido"))
            .isInstanceOf(ShareLinkNotFoundException.class);
    }

    // ---- getTranscript ----

    @Test
    void getTranscript_deveDevolverPerguntasDaEntrevistaEmOrdem() {
        Interview interview = novaInterview(InterviewStatus.FINALIZADA, 2);
        Question pergunta1 = new Question();
        pergunta1.setOrdem(1);
        Question pergunta2 = new Question();
        pergunta2.setOrdem(2);
        interview.getQuestions().add(pergunta1);
        interview.getQuestions().add(pergunta2);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        List<Question> transcript = service.getTranscript(interview.getId(), owner);

        assertThat(transcript).containsExactly(pergunta1, pergunta2);
    }

    @Test
    void getTranscript_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 2);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.getTranscript(interview.getId(), outroUsuario))
            .isInstanceOf(InterviewAccessDeniedException.class);
    }

    @Test
    void getTranscript_deveLancar404QuandoEntrevistaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(interviewRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTranscript(id, owner))
            .isInstanceOf(InterviewNotFoundException.class);
    }

    // ---- analyzeResume ----

    @Test
    void analyzeResume_deveExtrairTextoAnalisarComIaESalvar() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        byte[] pdf = "conteudo-fake".getBytes();
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        when(resumeTextExtractor.extract(pdf)).thenReturn("texto extraido do curriculo");
        when(resumeAnalyzer.analyze("texto extraido do curriculo", "Java", "junior", null))
            .thenReturn(new AiResumeResult(NivelPercebido.PLENO, "leitura do curriculo",
                List.of("5 anos com Java"), List.of("sem projetos concretos citados"), null, List.of(), List.of()));

        ResumeAnalysis analysis = service.analyzeResume(interview.getId(), owner, pdf);

        assertThat(analysis.getNivelPercebidoCurriculo()).isEqualTo(NivelPercebido.PLENO);
        assertThat(analysis.getResumo()).isEqualTo("leitura do curriculo");
        assertThat(analysis.getAderenciaVagaPercentual()).isNull();
        assertThat(interview.getResumeAnalysis()).isEqualTo(analysis);
        verify(resumeAnalysisRepository).save(analysis);
    }

    @Test
    void analyzeResume_devePassarDescricaoDaVagaESalvarAderencia() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        interview.setDescricaoVaga("Vaga de Java pleno com Spring Boot");
        byte[] pdf = "conteudo-fake".getBytes();
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
        when(resumeTextExtractor.extract(pdf)).thenReturn("texto extraido do curriculo");
        when(resumeAnalyzer.analyze("texto extraido do curriculo", "Java", "junior", "Vaga de Java pleno com Spring Boot"))
            .thenReturn(new AiResumeResult(NivelPercebido.PLENO, "leitura do curriculo",
                List.of("5 anos com Java"), List.of(), 72, List.of("Spring Boot"), List.of("Kubernetes")));

        ResumeAnalysis analysis = service.analyzeResume(interview.getId(), owner, pdf);

        assertThat(analysis.getAderenciaVagaPercentual()).isEqualTo(72);
        assertThat(analysis.getPontosAderenciaVaga()).containsExactly("Spring Boot");
        assertThat(analysis.getGapsVaga()).containsExactly("Kubernetes");
    }

    @Test
    void analyzeResume_deveReaproveitarAnaliseJaExistenteSemChamarIaDeNovo() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        ResumeAnalysis existente = new ResumeAnalysis();
        interview.setResumeAnalysis(existente);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        ResumeAnalysis resultado = service.analyzeResume(interview.getId(), owner, "pdf".getBytes());

        assertThat(resultado).isEqualTo(existente);
        verifyNoInteractions(resumeTextExtractor, resumeAnalyzer);
        verify(resumeAnalysisRepository, never()).save(any());
    }

    @Test
    void analyzeResume_deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.analyzeResume(interview.getId(), outroUsuario, "pdf".getBytes()))
            .isInstanceOf(InterviewAccessDeniedException.class);
        verifyNoInteractions(resumeTextExtractor, resumeAnalyzer);
    }

    // ---- getResumeAnalysis ----

    @Test
    void getResumeAnalysis_deveDevolverNullQuandoNaoHaCurriculoEnviado() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThat(service.getResumeAnalysis(interview.getId(), owner)).isNull();
    }

    // ---- listByUser ----

    @Test
    void listByUser_deveDelegarParaRepository() {
        Interview interview = novaInterview(InterviewStatus.EM_ANDAMENTO, 3);
        when(interviewRepository.findByUserOrderByCriadoEmDesc(owner)).thenReturn(List.of(interview));

        List<Interview> resultado = service.listByUser(owner);

        assertThat(resultado).containsExactly(interview);
    }
}
