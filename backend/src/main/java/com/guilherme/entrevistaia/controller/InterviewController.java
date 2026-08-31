package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.ai.AiAudioTranscriber;
import com.guilherme.entrevistaia.ai.AiQuestionResult;
import com.guilherme.entrevistaia.ai.AiQuestionStreamGenerator;
import com.guilherme.entrevistaia.dto.*;
import com.guilherme.entrevistaia.entity.*;
import com.guilherme.entrevistaia.service.InterviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Todas as rotas aqui exigem token válido (ver SecurityConfig: qualquer coisa
// que não seja /auth/** precisa de autenticação). Este controller é só uma
// "casca" fina: cada método valida o request (@Valid), delega pro
// InterviewService e converte a entidade retornada num DTO (*Response.from(...)).
@RestController
@RequestMapping("/interviews")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;
    private final AiQuestionStreamGenerator questionStreamGenerator;
    private final AiAudioTranscriber audioTranscriber;

    // Executor dedicado só pro streaming: a chamada de rede à OpenAI em modo
    // streaming bloqueia a thread por vários segundos, o que NUNCA pode
    // acontecer numa thread do pool HTTP normal do servlet container (senão
    // esgota o pool com poucas entrevistas simultâneas). O controller devolve
    // o SseEmitter imediatamente e o trabalho de verdade roda aqui.
    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    public InterviewController(InterviewService interviewService,
                                AiQuestionStreamGenerator questionStreamGenerator,
                                AiAudioTranscriber audioTranscriber) {
        this.interviewService = interviewService;
        this.questionStreamGenerator = questionStreamGenerator;
        this.audioTranscriber = audioTranscriber;
    }

    // POST /interviews — inicia uma nova entrevista para o usuário logado.
    // @AuthenticationPrincipal User user: o Spring Security injeta aqui o User
    // que o JwtAuthenticationFilter colocou no SecurityContextHolder — o
    // controller NUNCA lida com token diretamente, só recebe o usuário já resolvido.
    @PostMapping
    public ResponseEntity<InterviewResponse> start(@RequestBody @Valid StartInterviewRequest request,
                                                     @AuthenticationPrincipal User user) {
        Interview interview = interviewService.startInterview(
            user, request.stack(), request.nivel(), request.totalPerguntas(),
            request.descricaoVaga(), request.focoPratica());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterviewResponse.from(interview));
    }

    // GET /interviews/{id} — detalhe de uma entrevista específica (stack,
    // nível, totalPerguntas...). Usado pelo botão "Praticar os gaps" no
    // relatório, que precisa desses dados da entrevista original.
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> get(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Interview interview = interviewService.getInterview(id, user);
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    // GET /interviews/{id}/next-question — gera (via IA) e devolve a próxima
    // pergunta da entrevista. O front chama isso repetidamente até a
    // entrevista acabar.
    @GetMapping("/{id}/next-question")
    public ResponseEntity<QuestionResponse> nextQuestion(@PathVariable UUID id,
                                                           @AuthenticationPrincipal User user) {
        Question question = interviewService.getNextQuestion(id, user);
        return ResponseEntity.ok(QuestionResponse.from(question));
    }

    // GET /interviews/{id}/next-question/stream — mesma coisa que
    // /next-question, mas entrega o texto da pergunta em pedaços via
    // Server-Sent Events (efeito "IA digitando", ver useInterview.ts no
    // front). Dois eventos possíveis: "delta" (um pedaço de texto, repetidos
    // vários) e "done" (a QuestionResponse final, já persistida, uma vez só —
    // é o sinal de que a pergunta terminou e virou "real" no banco). Em caso
    // de erro, emite um evento "error" antes de fechar a conexão.
    //
    // O trabalho pesado roda em streamingExecutor (thread separada), NUNCA na
    // thread do servlet que atendeu essa requisição — por isso as chamadas ao
    // InterviewService (que abrem sua própria transação) e a chamada de IA em
    // si (sem transação nenhuma) estão todas dentro do Runnable abaixo.
    @GetMapping(value = "/{id}/next-question/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter nextQuestionStream(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        SseEmitter emitter = new SseEmitter(60_000L);

        streamingExecutor.execute(() -> {
            try {
                InterviewService.NextQuestionPromptContext ctx = interviewService.prepareNextQuestionPrompt(id, user);

                AiQuestionResult result = questionStreamGenerator.generateStreaming(ctx.userPrompt(), token -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(token));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                Question question = interviewService.persistStreamedQuestion(id, ctx.numeroAtual(), result);
                emitter.send(SseEmitter.event().name("done").data(QuestionResponse.from(question)));
                emitter.complete();
            } catch (Exception e) {
                log.error("[STREAM_QUESTION_ERROR] interviewId={} erro={}", id, e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // POST /interviews/questions/{questionId}/answer — envia a resposta do
    // candidato, dispara a avaliação da IA, e informa se essa era a última
    // pergunta (finalizada=true), pra o front saber se deve ir pro relatório.
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<AnswerResponse> submitAnswer(@PathVariable UUID questionId,
                                                         @RequestBody @Valid SubmitAnswerRequest request,
                                                         @AuthenticationPrincipal User user) {
        Answer answer = interviewService.submitAnswer(questionId, user, request.resposta());
        boolean finalizada = answer.getQuestion().getInterview().getStatus() == InterviewStatus.FINALIZADA;
        return ResponseEntity.ok(AnswerResponse.from(answer, finalizada));
    }

    // POST /interviews/transcribe — recebe o áudio da resposta falada
    // (multipart, campo "audio") e devolve só o texto transcrito pela IA. NÃO
    // avalia nada e não toca no banco: o front joga o texto no campo, o
    // candidato revisa/corrige e envia pelo endpoint de resposta acima. Exige
    // autenticação como qualquer rota /interviews/**, mas não precisa de id de
    // entrevista — transcrição é independente de contexto.
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(@RequestParam("audio") MultipartFile audio,
                                                             @AuthenticationPrincipal User user) throws IOException {
        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String nomeArquivo = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "resposta.webm";
        log.info("[AUDIO_TRANSCRIPTION_REQUEST] userId={} bytes={} arquivo={}",
            user.getId(), audio.getSize(), nomeArquivo);

        String texto = audioTranscriber.transcribe(audio.getBytes(), nomeArquivo);

        log.info("[AUDIO_TRANSCRIPTION_COMPLETE] userId={} chars={}", user.getId(), texto.length());
        return ResponseEntity.ok(new TranscriptionResponse(texto));
    }

    // POST /interviews/{id}/abandon — candidato desistiu no meio da entrevista
    // (front confirma antes de chamar isso, ver InterviewChat). Marca a
    // entrevista como ABANDONADA — ela para de aparecer como "em andamento"
    // no histórico e não pode mais ser retomada.
    @PostMapping("/{id}/abandon")
    public ResponseEntity<InterviewResponse> abandon(@PathVariable UUID id,
                                                       @AuthenticationPrincipal User user) {
        Interview interview = interviewService.abandonInterview(id, user);
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    // GET /interviews/{id}/report — devolve o relatório final (gera na
    // primeira chamada, reaproveita depois — ver InterviewService.getOrGenerateReport).
    @GetMapping("/{id}/report")
    public ResponseEntity<FeedbackReportResponse> report(@PathVariable UUID id,
                                                           @AuthenticationPrincipal User user) {
        FeedbackReport report = interviewService.getOrGenerateReport(id, user);
        return ResponseEntity.ok(FeedbackReportResponse.from(report));
    }

    // GET /interviews/{id}/transcript — todas as perguntas da entrevista, cada
    // uma com a resposta do candidato e a avaliação da IA (se já respondida).
    // Usado na tela de relatório pro candidato revisar a entrevista inteira.
    @GetMapping("/{id}/transcript")
    public ResponseEntity<List<QuestionWithAnswerResponse>> transcript(@PathVariable UUID id,
                                                                         @AuthenticationPrincipal User user) {
        List<QuestionWithAnswerResponse> transcript = interviewService.getTranscript(id, user).stream()
            .map(QuestionWithAnswerResponse::from)
            .toList();
        return ResponseEntity.ok(transcript);
    }

    // POST /interviews/{id}/resume — upload opcional do currículo (PDF) em
    // multipart/form-data, campo "arquivo". Extrai o texto, manda pra IA
    // analisar e devolve a leitura — que passa a influenciar as próximas
    // perguntas geradas (ver OpenAiQuestionGenerator) e aparece comparada ao
    // desempenho real na tela de relatório (ver OpenAiReportGenerator).
    @PostMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeAnalysisResponse> uploadResume(@PathVariable UUID id,
                                                                 @RequestParam("arquivo") MultipartFile arquivo,
                                                                 @AuthenticationPrincipal User user) throws IOException {
        ResumeAnalysis analysis = interviewService.analyzeResume(id, user, arquivo.getBytes());
        return ResponseEntity.ok(ResumeAnalysisResponse.from(analysis));
    }

    // GET /interviews/{id}/resume — devolve a leitura do currículo já feita
    // (se o candidato enviou um). 404 se não houver currículo pra essa
    // entrevista — não é erro, upload é opcional.
    @GetMapping("/{id}/resume")
    public ResponseEntity<ResumeAnalysisResponse> resume(@PathVariable UUID id,
                                                           @AuthenticationPrincipal User user) {
        ResumeAnalysis analysis = interviewService.getResumeAnalysis(id, user);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ResumeAnalysisResponse.from(analysis));
    }

    // POST /interviews/{id}/share — gera (ou reaproveita, se já existir) o
    // token que habilita a URL pública do relatório. Só funciona em entrevista
    // FINALIZADA (é o relatório que vira público, não faz sentido antes disso).
    @PostMapping("/{id}/share")
    public ResponseEntity<ShareTokenResponse> share(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        String shareToken = interviewService.getOrCreateShareToken(id, user);
        return ResponseEntity.ok(new ShareTokenResponse(shareToken));
    }

    // DELETE /interviews/{id}/share — revoga um link já gerado (ex.: candidato
    // mandou pro recrutador errado). O link para de funcionar imediatamente.
    @DeleteMapping("/{id}/share")
    public ResponseEntity<Void> revokeShare(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        interviewService.revokeShareToken(id, user);
        return ResponseEntity.noContent().build();
    }

    // GET /interviews/public/{shareToken}/report — ÚNICA rota deste controller
    // sem autenticação (ver SecurityConfig: /interviews/public/** é permitAll).
    // É o que faz o link de compartilhamento funcionar pra quem não tem login
    // nenhum na aplicação (ex.: um recrutador).
    @GetMapping("/public/{shareToken}/report")
    public ResponseEntity<PublicReportResponse> publicReport(@PathVariable String shareToken) {
        FeedbackReport report = interviewService.getReportByShareToken(shareToken);
        return ResponseEntity.ok(PublicReportResponse.from(report));
    }

    // GET /interviews — histórico de todas as entrevistas do usuário logado
    // (usado, por exemplo, numa tela de "minhas entrevistas anteriores").
    @GetMapping
    public ResponseEntity<List<InterviewResponse>> history(@AuthenticationPrincipal User user) {
        log.info("[INTERVIEW_HISTORY_REQUEST] userId={}", user.getId());
        List<InterviewResponse> historico = interviewService.listByUser(user).stream()
            .map(InterviewResponse::from)
            .toList();
        return ResponseEntity.ok(historico);
    }
}
