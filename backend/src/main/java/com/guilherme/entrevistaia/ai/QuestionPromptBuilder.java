package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.Question;
import com.guilherme.entrevistaia.entity.ResumeAnalysis;
import org.springframework.stereotype.Component;

// Monta o "contexto" que a IA recebe pra decidir a próxima pergunta: stack,
// nível, vaga, leitura de currículo, foco de prática, e o HISTÓRICO completo
// (cada pergunta anterior + resposta + nota + resumo da avaliação). É isso que
// torna a entrevista adaptativa — sem esse histórico, cada pergunta seria
// gerada isoladamente, sem continuidade.
//
// Extraído de OpenAiQuestionGenerator pra ser reaproveitado também pelo
// caminho de streaming (ver InterviewService.prepareNextQuestionPrompt) — o
// FORMATO de saída da IA difere entre os dois (JSON vs. texto delimitado),
// mas o CONTEXTO que ela recebe é exatamente o mesmo, então não faz sentido
// duplicar essa montagem.
@Component
public class QuestionPromptBuilder {

    public String build(Interview interview, int numeroAtual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack: ").append(interview.getStack()).append("\n");
        sb.append("Nível do candidato: ").append(interview.getNivel()).append("\n");
        sb.append("Pergunta ").append(numeroAtual).append(" de ").append(interview.getTotalPerguntas()).append("\n");

        if (interview.getDescricaoVaga() != null && !interview.getDescricaoVaga().isBlank()) {
            sb.append("\nDescrição da vaga:\n").append(interview.getDescricaoVaga()).append("\n");
        }

        if (interview.getFocoPratica() != null && !interview.getFocoPratica().isBlank()) {
            sb.append("\nFoco de prática (gaps de uma entrevista anterior a reforçar):\n")
                .append(interview.getFocoPratica()).append("\n");
        }

        ResumeAnalysis resume = interview.getResumeAnalysis();
        if (resume != null) {
            sb.append("\nLeitura do currículo do candidato:\n");
            sb.append("Nível percebido pelo currículo: ").append(resume.getNivelPercebidoCurriculo()).append("\n");
            sb.append("Resumo: ").append(resume.getResumo()).append("\n");
            if (!resume.getPontosFortes().isEmpty()) {
                sb.append("Pontos fortes declarados: ").append(String.join(", ", resume.getPontosFortes())).append("\n");
            }
            if (resume.getAderenciaVagaPercentual() != null) {
                sb.append("Aderência à vaga: ").append(resume.getAderenciaVagaPercentual()).append("%\n");
                if (!resume.getGapsVaga().isEmpty()) {
                    sb.append("Gaps em relação à vaga: ").append(String.join(", ", resume.getGapsVaga())).append("\n");
                }
            }
        }
        sb.append("\n");

        if (interview.getQuestions().isEmpty()) {
            sb.append("Esta é a primeira pergunta da entrevista. Escolha um tópico introdutório ")
                .append("compatível com a stack e o nível informados.");
            return sb.toString();
        }

        sb.append("Histórico da entrevista até agora:\n");
        for (Question q : interview.getQuestions()) {
            sb.append("- Pergunta ").append(q.getOrdem()).append(" [").append(q.getTopico())
                .append(" / ").append(q.getDificuldade()).append("]: ").append(q.getPergunta()).append("\n");
            if (q.getAnswer() != null) {
                sb.append("  Resposta do candidato: ").append(q.getAnswer().getRespostaTexto()).append("\n");
                sb.append("  Avaliação: nota ").append(q.getAnswer().getNota())
                    .append(", nível de domínio ").append(q.getAnswer().getNivelDominio())
                    .append(", resumo: ").append(q.getAnswer().getResumoAvaliacao()).append("\n");
            }
        }

        return sb.toString();
    }
}
