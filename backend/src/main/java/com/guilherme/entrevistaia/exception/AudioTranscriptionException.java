package com.guilherme.entrevistaia.exception;

// Falha ao transcrever o áudio da resposta na OpenAI (rede, HTTP não-2xx,
// formato de áudio recusado, etc.). Vira HTTP 503 igual às outras falhas de
// dependência de IA — do ponto de vista do candidato é "tente de novo, ou
// digite a resposta".
public class AudioTranscriptionException extends AppException {
    public AudioTranscriptionException(String motivo, Throwable causa) {
        super("AUDIO_TRANSCRIPTION_ERROR", motivo, causa);
    }
}
