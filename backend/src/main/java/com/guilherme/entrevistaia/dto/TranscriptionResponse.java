package com.guilherme.entrevistaia.dto;

// Resposta de POST /interviews/transcribe: só o texto que a IA transcreveu do
// áudio. O front joga isso no campo de resposta, o candidato revisa e envia
// pelo endpoint de resposta em texto de sempre.
public record TranscriptionResponse(String texto) {}
