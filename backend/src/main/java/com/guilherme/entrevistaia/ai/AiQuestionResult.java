package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.Dificuldade;

// Resultado já "traduzido" do JSON da IA para tipos Java fortemente tipados
// (Dificuldade é enum, não String solta). motivoEscolha não é salvo no banco
// hoje — existe pra fins de log/depuração de por que a IA escolheu essa pergunta.
public record AiQuestionResult(
    String pergunta,
    String topico,
    Dificuldade dificuldade,
    String motivoEscolha
) {}
