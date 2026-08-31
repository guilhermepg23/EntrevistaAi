package com.guilherme.entrevistaia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// PONTO DE ENTRADA da aplicação — é a classe que você roda (mvn spring-boot:run
// ou o main() direto). @SpringBootApplication é uma anotação "combo" que liga:
// - detecção automática de todos os @Component/@Service/@Repository/@RestController
//   dentro deste pacote e subpacotes (com.guilherme.entrevistaia.*);
// - auto-configuração do Spring Boot (Tomcat embutido, JPA, Security, etc.,
//   configurados a partir do que está no pom.xml e no application.yml).
//
// SUGESTÃO DE LEITURA: comece por aqui, depois siga a ordem descrita na
// resposta do chat (security -> controller -> service -> ai).
//
// UserDetailsServiceAutoConfiguration é excluída de propósito: a autenticação
// é 100% via JWT (ver SecurityConfig + JwtAuthenticationFilter), não há
// formLogin nem httpBasic. Sem essa exclusão, o Spring Boot cria um usuário
// "user" em memória e loga "Using generated security password: ..." a cada
// boot — senha que nunca é usada, só polui o log.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EntrevistaIaApplication {
    public static void main(String[] args) {
        SpringApplication.run(EntrevistaIaApplication.class, args);
    }
}

