package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.ai.*;
import com.guilherme.entrevistaia.entity.*;
import com.guilherme.entrevistaia.exception.*;
import com.guilherme.entrevistaia.repository.AnswerRepository;
import com.guilherme.entrevistaia.repository.FeedbackReportRepository;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.QuestionRepository;
import com.guilherme.entrevistaia.repository.ResumeAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Classe central da aplicação: é aqui que a lógica de negócio de verdade
// acontece. Controllers ficam "burros" de propósito (só traduzem HTTP <-> DTO),
// e este Service é quem: valida regras (dono da entrevista, estado válido),
// coordena chamadas de IA, e persiste no banco através dos repositories.
//
// @Transactional nos métodos garante que, se algo falhar no meio (ex.: banco
// caiu depois de gerar a pergunta com IA mas antes de salvar), TUDO é
// desfeito — não fica um estado inconsistente meio-salvo no banco.
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackReportRepository reportRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AiQuestionGenerator questionGenerator;
    private final AiAnswerEvaluator answerEvaluator;
    private final AiReportGenerator reportGenerator;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiResumeAnalyzer resumeAnalyzer;
    private final QuestionPromptBuilder questionPromptBuilder;

    public InterviewService(InterviewRepository interviewRepository,
                             QuestionRepository questionRepository,
                             AnswerRepository answerRepository,
                             FeedbackReportRepository reportRepository,
                             ResumeAnalysisRepository resumeAnalysisRepository,
                             AiQuestionGenerator questionGenerator,
                             AiAnswerEvaluator answerEvaluator,
                             AiReportGenerator reportGenerator,
                             ResumeTextExtractor resumeTextExtractor,
                             AiResumeAnalyzer resumeAnalyzer,
                             QuestionPromptBuilder questionPromptBuilder) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.reportRepository = reportRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.questionGenerator = questionGenerator;
        this.answerEvaluator = answerEvaluator;
        this.reportGenerator = reportGenerator;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeAnalyzer = resumeAnalyzer;
        this.questionPromptBuilder = questionPromptBuilder;
    }

    // Chamado por POST /interviews. Só cria o "envelope" da entrevista — as
    // perguntas em si são geradas sob demanda, uma por vez, em getNextQuestion().
    @Transactional
    public Interview startInterview(User user, String stack, String nivel, int totalPerguntas,
                                     String descricaoVaga, String focoPratica) {
        Interview interview = new Interview();
        interview.setUser(user);
        interview.setStack(stack);
        interview.setNivel(nivel);
        interview.setTotalPerguntas(totalPerguntas);
        interview.setDescricaoVaga(descricaoVaga);
        interview.setFocoPratica(focoPratica);
        interview.setStatus(InterviewStatus.EM_ANDAMENTO);
        interview.setCriadoEm(OffsetDateTime.now());
        interviewRepository.save(interview);

        log.info("[INTERVIEW_STARTED] interviewId={} userId={} stack={} nivel={}",
            interview.getId(), user.getId(), stack, nivel);

        return interview;
    }

    // Chamado por GET /interviews/{id}/next-question. Gera E salva uma nova
    // pergunta a cada chamada — não existe "banco de perguntas" pré-criado,
    // cada uma é gerada na hora, adaptada ao histórico até então.
    @Transactional
    public Question getNextQuestion(UUID interviewId, User user) {
        // loadOwnedInterview já garante duas coisas: a entrevista existe E
        // pertence ao usuário logado (ver validateOwnership mais abaixo).
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getStatus() != InterviewStatus.EM_ANDAMENTO) {
            log.warn("[INTERVIEW_INVALID_STATE] interviewId={} status={} acao=getNextQuestion",
                interviewId, interview.getStatus());
            throw new InvalidInterviewStateException(interviewId, interview.getStatus().name(), "getNextQuestion");
        }

        // "Próxima ordem" = quantas perguntas já existem + 1. Não guardamos um
        // contador separado no banco — a lista de questions já É a fonte da verdade.
        int proximaOrdem = interview.getQuestions().size() + 1;

        // Guarda contra pedir mais perguntas do que o combinado no início
        // (ex.: cliente chamando /next-question repetidamente por engano).
        if (proximaOrdem > interview.getTotalPerguntas()) {
            log.info("[INTERVIEW_ALREADY_COMPLETE] interviewId={} totalPerguntas={}",
                interviewId, interview.getTotalPerguntas());
            throw new InvalidInterviewStateException(interviewId, "PERGUNTAS_ESGOTADAS", "getNextQuestion");
        }

        log.info("[AI_QUESTION_REQUEST] interviewId={} ordem={}/{}",
            interviewId, proximaOrdem, interview.getTotalPerguntas());

        // Chamada de rede pra OpenAI acontece aqui (ver ai/impl/OpenAiQuestionGenerator).
        // interview inteira é passada pra IA poder olhar o histórico de perguntas
        // anteriores e ser adaptativa.
        AiQuestionResult result = questionGenerator.generate(interview, proximaOrdem);

        Question question = new Question();
        question.setInterview(interview);
        question.setOrdem(proximaOrdem);
        question.setPergunta(result.pergunta());
        question.setTopico(result.topico());
        question.setDificuldade(result.dificuldade());
        questionRepository.save(question);

        log.info("[AI_QUESTION_GENERATED] interviewId={} questionId={} topico={} dificuldade={}",
            interviewId, question.getId(), result.topico(), result.dificuldade());

        return question;
    }

    // Contexto pronto pra chamar a IA em streaming — ver prepareNextQuestionPrompt.
    // Carrega só dados primitivos (nenhuma referência a entidade JPA), de
    // propósito: é passado pra fora da transação, potencialmente pra uma
    // thread diferente (ver InterviewController.nextQuestionStream), e
    // entidades JPA não sobrevivem a isso (sessão do Hibernate fecha).
    public record NextQuestionPromptContext(int numeroAtual, String userPrompt) {}

    // Chamado por GET /interviews/{id}/next-question/stream — ETAPA 1 de 2.
    // Faz toda a leitura no banco (valida estado, monta o prompt com o
    // histórico) e devolve só texto puro. A chamada de streaming à IA em si
    // acontece DEPOIS, fora de qualquer transação (ver persistStreamedQuestion
    // pra etapa 2) — não faz sentido segurar uma transação/conexão de banco
    // aberta durante os vários segundos que um streaming pode levar.
    @Transactional
    public NextQuestionPromptContext prepareNextQuestionPrompt(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getStatus() != InterviewStatus.EM_ANDAMENTO) {
            log.warn("[INTERVIEW_INVALID_STATE] interviewId={} status={} acao=prepareNextQuestionPrompt",
                interviewId, interview.getStatus());
            throw new InvalidInterviewStateException(interviewId, interview.getStatus().name(), "getNextQuestion");
        }

        int proximaOrdem = interview.getQuestions().size() + 1;
        if (proximaOrdem > interview.getTotalPerguntas()) {
            log.info("[INTERVIEW_ALREADY_COMPLETE] interviewId={} totalPerguntas={}",
                interviewId, interview.getTotalPerguntas());
            throw new InvalidInterviewStateException(interviewId, "PERGUNTAS_ESGOTADAS", "getNextQuestion");
        }

        String userPrompt = questionPromptBuilder.build(interview, proximaOrdem);
        return new NextQuestionPromptContext(proximaOrdem, userPrompt);
    }

    // Chamado por GET /interviews/{id}/next-question/stream — ETAPA 2 de 2,
    // depois que o streaming terminou e o texto já foi todo pro candidato.
    // Re-busca a entrevista (nova transação, pode ser outra thread) e persiste
    // a pergunta gerada — mesma coisa que getNextQuestion faz no fim, só que
    // com o resultado já pronto em vez de chamar a IA aqui dentro.
    @Transactional
    public Question persistStreamedQuestion(UUID interviewId, int ordem, AiQuestionResult result) {
        Interview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new InterviewNotFoundException(interviewId));

        Question question = new Question();
        question.setInterview(interview);
        question.setOrdem(ordem);
        question.setPergunta(result.pergunta());
        question.setTopico(result.topico());
        question.setDificuldade(result.dificuldade());
        questionRepository.save(question);

        log.info("[AI_QUESTION_GENERATED] interviewId={} questionId={} topico={} dificuldade={} streaming=true",
            interviewId, question.getId(), result.topico(), result.dificuldade());

        return question;
    }

    // Chamado por POST /interviews/questions/{questionId}/answer. Note que
    // recebe questionId (não interviewId) — a entrevista é descoberta a partir
    // da pergunta (question.getInterview()).
    @Transactional
    public Answer submitAnswer(UUID questionId, User user, String respostaTexto) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Interview interview = question.getInterview();
        validateOwnership(interview, user);

        // Impede responder a mesma pergunta duas vezes (o que geraria duas
        // avaliações da IA pra mesma pergunta e confundiria o histórico).
        if (question.getAnswer() != null) {
            log.warn("[QUESTION_ALREADY_ANSWERED] questionId={} interviewId={}", questionId, interview.getId());
            throw new InvalidInterviewStateException(interview.getId(), "PERGUNTA_JA_RESPONDIDA", "submitAnswer");
        }

        log.info("[AI_EVALUATION_REQUEST] questionId={} interviewId={}", questionId, interview.getId());

        // Chamada de rede pra OpenAI (ver ai/impl/OpenAiAnswerEvaluator).
        AiEvaluationResult result = answerEvaluator.evaluate(question, respostaTexto);

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setRespostaTexto(respostaTexto);
        answer.setNota(result.nota());
        answer.setResumoAvaliacao(result.resumo());
        answer.setPontosFortes(result.pontosFortes());
        answer.setGaps(result.gaps());
        answer.setNivelDominio(result.nivelDominio());
        answer.setRespostaModelo(result.respostaModelo());
        answer.setCriadoEm(OffsetDateTime.now());
        answerRepository.save(answer);

        log.info("[AI_EVALUATION_COMPLETE] questionId={} interviewId={} nota={} nivelDominio={}",
            questionId, interview.getId(), result.nota(), result.nivelDominio());

        // Transição de estado da entrevista: se essa era a última pergunta
        // (ordem == totalPerguntas), a entrevista acaba de virar FINALIZADA.
        // É essa mudança de status que libera o endpoint /report (ver abaixo).
        boolean eraUltimaPergunta = question.getOrdem() >= interview.getTotalPerguntas();
        if (eraUltimaPergunta) {
            interview.setStatus(InterviewStatus.FINALIZADA);
            interview.setFinalizadoEm(OffsetDateTime.now());
            interviewRepository.save(interview);
            log.info("[INTERVIEW_FINISHED] interviewId={} totalPerguntas={}",
                interview.getId(), interview.getTotalPerguntas());
        }

        return answer;
    }

    // Chamado por POST /interviews/{id}/abandon — o candidato desistiu no meio
    // (botão "Sair da entrevista" no front, com confirmação antes de chamar
    // isso). Só faz sentido em EM_ANDAMENTO: uma entrevista já FINALIZADA ou já
    // ABANDONADA não pode ser abandonada de novo (guarda de estado igual às
    // outras transições).
    @Transactional
    public Interview abandonInterview(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getStatus() != InterviewStatus.EM_ANDAMENTO) {
            log.warn("[INTERVIEW_INVALID_STATE] interviewId={} status={} acao=abandonInterview",
                interviewId, interview.getStatus());
            throw new InvalidInterviewStateException(interviewId, interview.getStatus().name(), "abandonInterview");
        }

        interview.setStatus(InterviewStatus.ABANDONADA);
        interview.setFinalizadoEm(OffsetDateTime.now());
        interviewRepository.save(interview);

        log.info("[INTERVIEW_ABANDONED] interviewId={} perguntasRespondidas={}/{}",
            interviewId, interview.getQuestions().size(), interview.getTotalPerguntas());

        return interview;
    }

    // Chamado por GET /interviews/{id}/report. O nome "getOrGenerate" já entrega
    // o comportamento: idempotente — a primeira chamada gera e salva o
    // relatório (chamando a IA, que custa dinheiro e tempo); chamadas seguintes
    // só devolvem o que já foi salvo, sem gastar outra chamada de IA à toa.
    @Transactional
    public FeedbackReport getOrGenerateReport(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getReport() != null) {
            return interview.getReport();
        }

        if (interview.getStatus() != InterviewStatus.FINALIZADA) {
            log.warn("[REPORT_REQUESTED_TOO_EARLY] interviewId={} status={}",
                interviewId, interview.getStatus());
            throw new InvalidInterviewStateException(interviewId, interview.getStatus().name(), "getOrGenerateReport");
        }

        log.info("[AI_REPORT_REQUEST] interviewId={} totalPerguntas={}",
            interviewId, interview.getQuestions().size());

        // Chamada de rede pra OpenAI (ver ai/impl/OpenAiReportGenerator),
        // passando a entrevista inteira pra IA ponderar a evolução do candidato.
        AiReportResult result = reportGenerator.generate(interview);

        FeedbackReport report = new FeedbackReport();
        report.setInterview(interview);
        report.setNotaGeral(result.notaGeral());
        report.setResumoExecutivo(result.resumoExecutivo());
        report.setPontosFortes(result.pontosFortes());
        report.setPontosFracos(result.pontosFracos());
        report.setSugestoesEstudo(result.sugestoesEstudo());
        report.setNivelPercebido(result.nivelPercebido());
        report.setRecomendacao(result.recomendacao());
        report.setCriadoEm(OffsetDateTime.now());
        reportRepository.save(report);

        log.info("[AI_REPORT_GENERATED] interviewId={} notaGeral={} recomendacao={}",
            interviewId, result.notaGeral(), result.recomendacao());

        return report;
    }

    // Chamado por POST /interviews/{id}/resume. Upload é opcional e só faz
    // sentido antes (ou logo no início) da entrevista, pra poder influenciar
    // as perguntas geradas — mas não travamos por status aqui: o pior caso de
    // enviar tarde é só a leitura não ter chegado a tempo de mudar perguntas
    // já feitas, o que não é motivo pra bloquear o candidato.
    //
    // Idempotente feito nem por acaso, igual getOrGenerateReport: reenviar o
    // mesmo currículo (ex.: duplo clique) não gera duas chamadas de IA nem
    // duas ResumeAnalysis pra mesma entrevista.
    @Transactional
    public ResumeAnalysis analyzeResume(UUID interviewId, User user, byte[] arquivoPdf) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getResumeAnalysis() != null) {
            return interview.getResumeAnalysis();
        }

        String textoCurriculo = resumeTextExtractor.extract(arquivoPdf);

        log.info("[AI_RESUME_ANALYSIS_REQUEST] interviewId={}", interviewId);
        AiResumeResult result = resumeAnalyzer.analyze(
            textoCurriculo, interview.getStack(), interview.getNivel(), interview.getDescricaoVaga());

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setInterview(interview);
        analysis.setNivelPercebidoCurriculo(result.nivelPercebido());
        analysis.setResumo(result.resumo());
        analysis.setPontosFortes(result.pontosFortes());
        analysis.setGaps(result.gaps());
        analysis.setAderenciaVagaPercentual(result.aderenciaVagaPercentual());
        analysis.setPontosAderenciaVaga(result.pontosAderenciaVaga());
        analysis.setGapsVaga(result.gapsVaga());
        analysis.setCriadoEm(OffsetDateTime.now());
        resumeAnalysisRepository.save(analysis);
        interview.setResumeAnalysis(analysis);

        log.info("[AI_RESUME_ANALYSIS_COMPLETE] interviewId={} nivelPercebidoCurriculo={}",
            interviewId, result.nivelPercebido());

        return analysis;
    }

    // Chamado por GET /interviews/{id}/resume. Devolve null se o candidato não
    // enviou currículo pra essa entrevista (upload é opcional) — o controller
    // traduz isso pra 404.
    public ResumeAnalysis getResumeAnalysis(UUID interviewId, User user) {
        return loadOwnedInterview(interviewId, user).getResumeAnalysis();
    }

    // Chamado por GET /interviews (histórico do usuário logado).
    public List<Interview> listByUser(User user) {
        return interviewRepository.findByUserOrderByCriadoEmDesc(user);
    }

    // Chamado por GET /interviews/{id} — detalhe de uma entrevista específica.
    // Existia só implicitamente via listByUser antes; ficou necessário como
    // endpoint próprio pro botão "Praticar os gaps" (ver InterviewReportPage),
    // que precisa saber stack/nível/totalPerguntas da entrevista original pra
    // montar a nova sem pedir esses dados de novo ao candidato.
    public Interview getInterview(UUID interviewId, User user) {
        return loadOwnedInterview(interviewId, user);
    }

    // Chamado por POST /interviews/{id}/share. Só entrevista FINALIZADA tem
    // relatório pra compartilhar. Idempotente: se já existe um token, devolve
    // o mesmo (reforçar o botão "Compartilhar" não invalida um link já enviado).
    @Transactional
    public String getOrCreateShareToken(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getStatus() != InterviewStatus.FINALIZADA) {
            throw new InvalidInterviewStateException(interviewId, interview.getStatus().name(), "getOrCreateShareToken");
        }

        if (interview.getShareToken() == null) {
            interview.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            interviewRepository.save(interview);
            log.info("[SHARE_LINK_CREATED] interviewId={}", interviewId);
        }

        return interview.getShareToken();
    }

    // Chamado por DELETE /interviews/{id}/share — revoga um link já enviado
    // (ex.: candidato mandou pro recrutador errado). Links antigos param de
    // funcionar imediatamente, já que a busca pública é por shareToken.
    @Transactional
    public void revokeShareToken(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);
        interview.setShareToken(null);
        interviewRepository.save(interview);
        log.info("[SHARE_LINK_REVOKED] interviewId={}", interviewId);
    }

    // Chamado por GET /interviews/public/{shareToken}/report — SEM ownership
    // check de propósito (esse é o único método de todo o service que não
    // exige User; é isso que torna o link acessível sem login).
    public FeedbackReport getReportByShareToken(String shareToken) {
        Interview interview = interviewRepository.findByShareToken(shareToken)
            .orElseThrow(ShareLinkNotFoundException::new);
        if (interview.getReport() == null) {
            throw new ShareLinkNotFoundException();
        }
        return interview.getReport();
    }

    // Chamado por GET /interviews/{id}/transcript. Devolve todas as perguntas
    // da entrevista (já em ordem, ver @OrderBy em Interview.questions), cada
    // uma com sua resposta se já existir — é o que permite ao candidato revisar
    // a entrevista inteira, não só o resumo do relatório final. Funciona tanto
    // pra entrevista FINALIZADA quanto EM_ANDAMENTO (não há motivo pra travar
    // isso só numa finalizada).
    public List<Question> getTranscript(UUID interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);
        return interview.getQuestions();
    }

    // Helper usado por getNextQuestion/getOrGenerateReport: busca a entrevista
    // por id (404 se não existir) E já confere que pertence ao usuário logado
    // (403 se não for dono) — as duas checagens mais comuns, num lugar só.
    private Interview loadOwnedInterview(UUID interviewId, User user) {
        Interview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        validateOwnership(interview, user);
        return interview;
    }

    // Regra de segurança central do domínio: um usuário só pode ver/interagir
    // com as PRÓPRIAS entrevistas. Isso é checado aqui (na camada de negócio),
    // não no controller nem no banco — mesmo que alguém adivinhe o UUID de uma
    // entrevista de outra pessoa, essa checagem barra o acesso.
    private void validateOwnership(Interview interview, User user) {
        if (!interview.getUser().getId().equals(user.getId())) {
            log.warn("[INTERVIEW_ACCESS_DENIED] interviewId={} ownerId={} requesterId={}",
                interview.getId(), interview.getUser().getId(), user.getId());
            throw new InterviewAccessDeniedException(interview.getId());
        }
    }
}
