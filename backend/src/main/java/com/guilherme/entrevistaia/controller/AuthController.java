package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.AcceptTeamInviteRequest;
import com.guilherme.entrevistaia.dto.AuthResponse;
import com.guilherme.entrevistaia.dto.ForgotPasswordRequest;
import com.guilherme.entrevistaia.dto.LoginRequest;
import com.guilherme.entrevistaia.dto.PublicTeamInviteResponse;
import com.guilherme.entrevistaia.dto.RegisterRequest;
import com.guilherme.entrevistaia.dto.ResetPasswordRequest;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.CompanyRuleException;
import com.guilherme.entrevistaia.exception.CpfAlreadyInUseException;
import com.guilherme.entrevistaia.exception.CpfRequiredException;
import com.guilherme.entrevistaia.exception.EmailAlreadyInUseException;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.service.CompanyService;
import com.guilherme.entrevistaia.service.PasswordResetService;
import com.guilherme.entrevistaia.validation.CpfValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final CompanyService companyService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, PasswordResetService passwordResetService,
                          CompanyService companyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.companyService = companyService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        TipoConta tipo = request.tipo() != null ? request.tipo() : TipoConta.CANDIDATO;

        // Documento depende do tipo: candidato informa CPF (obrigatório);
        // recrutador não informa CPF, só o CNPJ da empresa, e ainda assim
        // opcional. O que não se aplica ao tipo é ignorado em vez de salvo.
        // @Cpf/@Cnpj já garantiram os dígitos verificadores; aqui normalizamos
        // pra só dígitos (o cliente pode ter mandado com máscara).
        // A falta do CPF é validação de entrada, como as do @Valid: barrada
        // antes de qualquer consulta ao banco.
        // O recrutador funda a empresa no cadastro, então o nome dela é
        // obrigatório pra ele (mesma lógica do CPF pro candidato).
        String cpf = tipo == TipoConta.CANDIDATO ? CpfValidator.stripToDigits(request.cpf()) : null;
        if (tipo == TipoConta.CANDIDATO && cpf.isEmpty()) {
            throw new CpfRequiredException();
        }
        if (tipo == TipoConta.RECRUTADOR && (request.nomeEmpresa() == null || request.nomeEmpresa().isBlank())) {
            throw CompanyRuleException.nomeEmpresaObrigatorio();
        }

        if (userRepository.existsByEmail(request.email())) {
            log.info("[AUTH_EMAIL_TAKEN] Tentativa de registro com email já existente: {}", request.email());
            throw new EmailAlreadyInUseException(request.email());
        }
        if (tipo == TipoConta.CANDIDATO && userRepository.existsByCpf(cpf)) {
            log.info("[AUTH_CPF_TAKEN] Tentativa de registro com CPF já existente");
            throw new CpfAlreadyInUseException();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setNome(request.nome());
        user.setCpf(cpf);
        user.setCriadoEm(OffsetDateTime.now());
        user.setTipo(tipo);
        // Nunca salvamos request.senha() direto — sempre passa pelo BCrypt antes.
        user.setSenhaHash(passwordEncoder.encode(request.senha()));
        if (tipo == TipoConta.RECRUTADOR) {
            // Cria a empresa (com o CNPJ opcional) e salva o usuário como ADMIN
            // dela, na mesma transação.
            companyService.fundarEmpresa(user, request.nomeEmpresa(), request.cnpj());
        } else {
            userRepository.save(user);
        }

        log.info("[AUTH_REGISTER_SUCCESS] Novo usuário registrado: userId={} tipo={}", user.getId(), user.getTipo());

        // Já loga o usuário automaticamente após o registro, devolvendo um
        // token pronto pra usar — evita ter que registrar e depois logar
        // como duas chamadas separadas.
        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user.getNome(), user.getTipo()));
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
        return ResponseEntity.ok(new AuthResponse(token, user.getNome(), user.getTipo()));
    }

    // GET /auth/team-invites/{token} — dados do convite de equipe pra tela de
    // aceitar (empresa, quem convidou, email travado). Público: o convidado
    // ainda não tem conta. Convite inválido/expirado/usado -> 404.
    @GetMapping("/team-invites/{token}")
    public ResponseEntity<PublicTeamInviteResponse> teamInvite(@PathVariable String token) {
        return ResponseEntity.ok(companyService.conviteParaAceitar(token));
    }

    // POST /auth/team-invites/{token}/accept — cria a conta do recrutador
    // convidado já dentro da empresa e já loga (devolve o token), como o register.
    @PostMapping("/team-invites/{token}/accept")
    public ResponseEntity<AuthResponse> acceptTeamInvite(@PathVariable String token,
                                                         @RequestBody @Valid AcceptTeamInviteRequest request) {
        User user = companyService.aceitarConvite(token, request.nome(), request.senha());
        String jwt = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt, user.getNome(), user.getTipo()));
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
