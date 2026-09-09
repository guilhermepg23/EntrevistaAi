package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.AuthResponse;
import com.guilherme.entrevistaia.dto.ForgotPasswordRequest;
import com.guilherme.entrevistaia.dto.LoginRequest;
import com.guilherme.entrevistaia.dto.RegisterRequest;
import com.guilherme.entrevistaia.dto.ResetPasswordRequest;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.CpfAlreadyInUseException;
import com.guilherme.entrevistaia.exception.EmailAlreadyInUseException;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.service.PasswordResetService;
import com.guilherme.entrevistaia.validation.CpfValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

// Controller = a "porta de entrada" HTTP. Fica propositalmente magro: só
// recebe o request, valida (via @Valid), delega a regra de negócio pra
// baixo (aqui, direto no repository + JwtService, já que login/registro
// não tem regra complexa o bastante pra merecer um Service próprio) e
// devolve um DTO. As duas rotas aqui (/auth/register, /auth/login) são as
// ÚNICAS liberadas sem token, configurado em SecurityConfig.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.info("[AUTH_EMAIL_TAKEN] Tentativa de registro com email já existente: {}", request.email());
            throw new EmailAlreadyInUseException(request.email());
        }

        // O @Cpf já garantiu que é um CPF válido; aqui normalizamos pra só
        // dígitos antes de checar duplicidade e salvar (o cliente pode ter
        // mandado com máscara).
        String cpf = CpfValidator.stripToDigits(request.cpf());
        if (userRepository.existsByCpf(cpf)) {
            log.info("[AUTH_CPF_TAKEN] Tentativa de registro com CPF já existente");
            throw new CpfAlreadyInUseException();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setNome(request.nome());
        user.setCpf(cpf);
        user.setCriadoEm(OffsetDateTime.now());
        // Nunca salvamos request.senha() direto — sempre passa pelo BCrypt antes.
        user.setSenhaHash(passwordEncoder.encode(request.senha()));
        userRepository.save(user);

        log.info("[AUTH_REGISTER_SUCCESS] Novo usuário registrado: userId={}", user.getId());

        // Já loga o usuário automaticamente após o registro, devolvendo um
        // token pronto pra usar — evita ter que registrar e depois logar
        // como duas chamadas separadas.
        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user.getNome()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.info("[AUTH_LOGIN_FAILED] Email não encontrado: {}", request.email());
                return new InvalidCredentialsException();
            });

        // .matches() faz o hash da senha digitada e compara com o hash salvo —
        // nunca "descriptografamos" o hash salvo (BCrypt é one-way, não dá pra
        // reverter), é assim que toda comparação de senha deve funcionar.
        if (!passwordEncoder.matches(request.senha(), user.getSenhaHash())) {
            log.info("[AUTH_LOGIN_FAILED] Senha incorreta para userId={}", user.getId());
            throw new InvalidCredentialsException();
        }

        log.info("[AUTH_LOGIN_SUCCESS] Login bem-sucedido: userId={}", user.getId());

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getNome()));
    }

    // POST /auth/forgot-password — dispara o email com o link de recuperação.
    // Responde SEMPRE 200 (mesmo se o email não tiver conta), de propósito: não
    // dá pra um atacante descobrir quais emails estão cadastrados por aqui.
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        log.info("[AUTH_FORGOT_PASSWORD] pedido de recuperação para email={}", request.email());
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }

    // POST /auth/reset-password — troca a senha usando o token do link do email.
    // Token inválido/expirado/já usado -> 400 (AUTH_RESET_TOKEN_INVALID).
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.novaSenha());
        return ResponseEntity.ok().build();
    }
}
