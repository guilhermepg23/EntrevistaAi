package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.AccountResponse;
import com.guilherme.entrevistaia.dto.ChangePasswordRequest;
import com.guilherme.entrevistaia.dto.UpdateAccountRequest;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Tela "Minha conta" do usuário logado. Todas as rotas exigem token (cai no
// anyRequest().authenticated() do SecurityConfig, sem regra própria lá).
// Casca fina: valida, delega pro AccountService, converte pra DTO.
@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // GET /account — detalhes da conta (nome, email, CPF mascarado, membro desde).
    @GetMapping
    public ResponseEntity<AccountResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AccountResponse.from(user));
    }

    // PATCH /account — edita o nome de exibição. Devolve os detalhes já
    // atualizados pro front atualizar o header/sessão.
    @PatchMapping
    public ResponseEntity<AccountResponse> update(@RequestBody @Valid UpdateAccountRequest request,
                                                   @AuthenticationPrincipal User user) {
        User atualizado = accountService.updateNome(user, request.nome());
        return ResponseEntity.ok(AccountResponse.from(atualizado));
    }

    // POST /account/change-password — troca de senha estando logado (senha
    // atual + nova). Senha atual errada -> 401 (AUTH_INVALID_CREDENTIALS).
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request,
                                                @AuthenticationPrincipal User user) {
        accountService.changePassword(user, request.senhaAtual(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }

    // DELETE /account — exclui a conta e tudo que depende dela (entrevistas,
    // análises de currículo, tokens de recuperação). Irreversível — o front
    // confirma antes de chamar.
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user) {
        accountService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
