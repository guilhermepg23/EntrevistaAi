package com.guilherme.entrevistaia.security;

import com.guilherme.entrevistaia.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

// Traduz o tipo da conta pra authority que o Spring Security entende:
// CANDIDATO -> ROLE_CANDIDATO, RECRUTADOR -> ROLE_RECRUTADOR. O prefixo ROLE_
// é o que o hasRole("CANDIDATO") do SecurityConfig procura.
//
// Num lugar só porque o JwtAuthenticationFilter e os testes de controller
// (que montam a autenticação na mão) precisam gerar exatamente a mesma coisa.
public final class UserAuthorities {

    private UserAuthorities() {}

    public static List<GrantedAuthority> of(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getTipo().name()));
    }
}
