package com.guilherme.entrevistaia.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

// @RestControllerAdvice: intercepta exceções lançadas por QUALQUER controller
// da aplicação, num lugar só — sem precisar de try/catch espalhado pelos
// controllers. Cada @ExceptionHandler abaixo diz "se essa exceção específica
// escapar de um controller, transforma ela nesta resposta HTTP".
//
// O padrão se repete: loga (com o errorCode como prefixo, pra facilitar busca
// nos logs) e devolve um ErrorResponse com status apropriado.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AiResponseParseException.class)
    public ResponseEntity<ErrorResponse> handleAiParse(AiResponseParseException ex) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getErrorCode(), "Não foi possível processar a resposta da IA no momento."));
    }

    @ExceptionHandler(AiSchemaValidationException.class)
    public ResponseEntity<ErrorResponse> handleSchemaInvalid(AiSchemaValidationException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getErrorCode(), "Resposta da IA fora do formato esperado."));
    }

    @ExceptionHandler(AiRetriesExhaustedException.class)
    public ResponseEntity<ErrorResponse> handleRetriesExhausted(AiRetriesExhaustedException ex) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getErrorCode(), "Serviço de IA indisponível no momento. Tente novamente em instantes."));
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InterviewNotFoundException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotFound(QuestionNotFoundException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidInterviewStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidInterviewStateException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InterviewAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(InterviewAccessDeniedException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AiStreamingException.class)
    public ResponseEntity<ErrorResponse> handleAiStreaming(AiStreamingException ex) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getErrorCode(), "Não foi possível gerar a pergunta em tempo real. Tente novamente."));
    }

    @ExceptionHandler(AudioTranscriptionException.class)
    public ResponseEntity<ErrorResponse> handleAudioTranscription(AudioTranscriptionException ex) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(ex.getErrorCode(), "Não foi possível transcrever o áudio. Tente de novo ou digite a resposta."));
    }

    @ExceptionHandler(ShareLinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShareLinkNotFound(ShareLinkNotFoundException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ResumeParseException.class)
    public ResponseEntity<ErrorResponse> handleResumeParse(ResumeParseException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleEmailTaken(EmailAlreadyInUseException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CpfAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleCpfTaken(CpfAlreadyInUseException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResetToken(InvalidResetTokenException ex) {
        log.info("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    // Erro de validação do @Valid (ex.: @Email, @NotBlank em RegisterRequest/
    // LoginRequest/etc.) — sem esse handler, cai no catch-all genérico abaixo
    // e vira 500, escondendo que o problema é só um campo mal preenchido.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.info("[VALIDATION_ERROR] {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    // Rede de segurança: qualquer exceção NÃO mapeada acima (bug, NullPointerException,
    // etc.) cai aqui em vez de vazar stacktrace pro cliente. Gera um traceId
    // aleatório, loga o erro completo com esse id, e devolve ao cliente só o
    // id — assim dá pra cruzar "usuário reportou erro X" com o log detalhado
    // no servidor, sem expor detalhes internos numa resposta HTTP pública.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("[UNEXPECTED_ERROR] traceId={} - {}", traceId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("UNEXPECTED_ERROR", "Erro interno. Referência: " + traceId));
    }
}
