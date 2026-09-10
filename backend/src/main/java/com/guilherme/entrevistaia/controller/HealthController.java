package com.guilherme.entrevistaia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Endpoint de health check, público (ver SecurityConfig). Só responde 200 com
// um corpo mínimo depois que o Spring terminou de subir — é o que o Render usa
// pra saber que o deploy ficou pronto (healthCheckPath no render.yaml). O
// projeto não usa Spring Boot Actuator de propósito (dependência a mais só pra
// isso), então este controller de uma linha faz o papel.
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
