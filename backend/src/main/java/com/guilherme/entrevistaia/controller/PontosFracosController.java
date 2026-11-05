package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.PontosFracosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Fica fora do InterviewController para não engordar o construtor dele. O
// caminho literal /interviews/pontos-fracos ganha do /interviews/{id} no
// mapeamento do Spring, e o acesso já é só de CANDIDATO (ver SecurityConfig).
@RestController
@RequestMapping("/interviews")
public class PontosFracosController {

    private final PontosFracosService pontosFracosService;

    public PontosFracosController(PontosFracosService pontosFracosService) {
        this.pontosFracosService = pontosFracosService;
    }

    // GET /interviews/pontos-fracos: temas que se repetem nos pontos fracos
    // dos últimos treinos, mais uma sugestão de próximo treino. Leva alguns
    // segundos quando há relatório novo (chamada de IA); senão, vem do cache.
    @GetMapping("/pontos-fracos")
    public ResponseEntity<PontosFracosRecorrentesResponse> recorrentes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pontosFracosService.recorrentes(user));
    }
}
