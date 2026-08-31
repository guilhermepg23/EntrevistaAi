package com.guilherme.entrevistaia.ai;

/**
 * Transcreve um áudio (a resposta falada do candidato, gravada no navegador via
 * MediaRecorder) para texto puro. Só isso — não avalia nada: o texto volta pro
 * front, o candidato revisa/edita e envia pelo fluxo normal de resposta em
 * texto (POST /interviews/questions/{id}/answer). Manter transcrição e
 * avaliação separadas dá ao candidato a chance de corrigir erros da
 * transcrição antes de a IA pontuar.
 *
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiAudioTranscriber}.
 */
public interface AiAudioTranscriber {

    /**
     * @param audio           bytes do arquivo de áudio (ex.: webm/opus do Chrome, mp4 do Safari)
     * @param nomeArquivo     nome original enviado pelo cliente — a API da OpenAI usa a
     *                        extensão pra inferir o formato, então precisa ser algo como
     *                        "resposta.webm"
     * @return o texto transcrito, já com trim; nunca null (string vazia se o áudio não tinha fala)
     */
    String transcribe(byte[] audio, String nomeArquivo);
}
