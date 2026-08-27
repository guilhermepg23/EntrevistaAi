package com.guilherme.entrevistaia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// PONTO DE ENTRADA da aplicação — é a classe que você roda (mvn spring-boot:run
// ou o main() direto). @SpringBootApplication é uma anotação "combo" que liga:
// - detecção automática de todos os @Component/@Service/@Repository/@RestController
//   dentro deste pacote e subpacotes (com.guilherme.entrevistaia.*);
// - auto-configuração do Spring Boot (Tomcat embutido, JPA, Security, etc.,
//   configurados a partir do que está no pom.xml e no application.yml).
//
// SUGESTÃO DE LEITURA: comece por aqui, depois siga a ordem descrita na
// resposta do chat (security -> controller -> service -> ai).
@SpringBootApplication
public class EntrevistaIaApplication {
    public static void main(String[] args) {
        SpringApplication.run(EntrevistaIaApplication.class, args);
    }
}

