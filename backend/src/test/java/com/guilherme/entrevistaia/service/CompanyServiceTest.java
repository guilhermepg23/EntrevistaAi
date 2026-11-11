package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.dto.CreatedTeamInviteResponse;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.CompanyInvite;
import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.TamanhoEmpresa;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.CompanyRuleException;
import com.guilherme.entrevistaia.exception.EmailAlreadyInUseException;
import com.guilherme.entrevistaia.mail.TeamInviteMailer;
import com.guilherme.entrevistaia.repository.AssessmentInviteRepository;
import com.guilherme.entrevistaia.repository.AssessmentRepository;
import com.guilherme.entrevistaia.repository.CompanyInviteRepository;
import com.guilherme.entrevistaia.repository.CompanyRepository;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.SecureTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Regras de empresa e equipe, sem Spring: os repositórios e o mailer são mocks.
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyInviteRepository inviteRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentInviteRepository assessmentInviteRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TeamInviteMailer mailer;
    @Mock private ImageService imageService;

    private CompanyService service;
    private Company acme;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companyRepository, inviteRepository, assessmentRepository,
            assessmentInviteRepository, userRepository,
            passwordEncoder, mailer, imageService, "https://app.exemplo.com/");
        acme = new Company();
        acme.setId(UUID.randomUUID());
        acme.setNome("Acme");
        admin = membro("Ana", PapelEmpresa.ADMIN, acme);
    }

    private User membro(String nome, PapelEmpresa papel, Company company) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@acme.com");
        u.setTipo(TipoConta.RECRUTADOR);
        u.setCompany(company);
        u.setPapelEmpresa(papel);
        lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    // ---------------------------------------------------------- dados

    @Test
    void atualizarEmpresa_salvaADescricaoComTrimEVaziaApaga() {
        service.atualizarEmpresa(admin, "Acme", null, "  Fintech de pagamentos.  ");
        assertThat(acme.getDescricao()).isEqualTo("Fintech de pagamentos.");

        service.atualizarEmpresa(admin, "Acme", null, "   ");
        assertThat(acme.getDescricao()).isNull();
    }

    @Test
    void atualizarEmpresa_recrutadorComumNaoEdita() {
        User comum = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        assertThatThrownBy(() -> service.atualizarEmpresa(comum, "Outra", null, "x"))
            .isInstanceOf(CompanyRuleException.class);
        assertThat(acme.getNome()).isEqualTo("Acme");
    }

    // ----------------------------------------------------------- perfil

    @Test
    void atualizarPerfil_salvaComTrimEVazioApaga() {
        service.atualizarPerfil(admin, " https://acme.com.br ", " Pagamentos ", TamanhoEmpresa.DE_11_A_50, " Recife ");
        assertThat(acme.getSite()).isEqualTo("https://acme.com.br");
        assertThat(acme.getSetor()).isEqualTo("Pagamentos");
        assertThat(acme.getTamanho()).isEqualTo(TamanhoEmpresa.DE_11_A_50);
        assertThat(acme.getCidade()).isEqualTo("Recife");

        service.atualizarPerfil(admin, "  ", "", null, null);
        assertThat(acme.getSite()).isNull();
        assertThat(acme.getSetor()).isNull();
        assertThat(acme.getTamanho()).isNull();
        assertThat(acme.getCidade()).isNull();
    }

    @Test
    void atualizarPerfil_recrutadorComumNaoEdita() {
        User comum = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        assertThatThrownBy(() -> service.atualizarPerfil(comum, null, "x", null, null))
            .isInstanceOf(CompanyRuleException.class);
        assertThat(acme.getSetor()).isNull();
    }

    @Test
    void atualizarCorMarca_normalizaEVazioRemove() {
        service.atualizarCorMarca(admin, " #0F766E ");
        assertThat(acme.getCorMarca()).isEqualTo("#0f766e");

        service.atualizarCorMarca(admin, "");
        assertThat(acme.getCorMarca()).isNull();
    }

    @Test
    void atualizarCorMarca_recrutadorComumNaoEdita() {
        User comum = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        assertThatThrownBy(() -> service.atualizarCorMarca(comum, "#123456"))
            .isInstanceOf(CompanyRuleException.class);
        assertThat(acme.getCorMarca()).isNull();
    }

    @Test
    void normalizarCor_aceitaSemCerquilhaERecusaOResto() {
        assertThat(CompanyService.normalizarCor("abcdef")).isEqualTo("#abcdef");
        assertThat(CompanyService.normalizarCor(null)).isNull();
        for (String ruim : List.of("#abc", "#12345g", "red", "#1234567", "rgb(0,0,0)", "#12 456")) {
            assertThatThrownBy(() -> CompanyService.normalizarCor(ruim))
                .as(ruim)
                .extracting("errorCode").isEqualTo("COMPANY_COLOR_INVALID");
        }
    }

    @Test
    void normalizarSite_semEsquemaGanhaHttps() {
        assertThat(CompanyService.normalizarSite("acme.com.br")).isEqualTo("https://acme.com.br");
        assertThat(CompanyService.normalizarSite("http://acme.com/vagas?x=1")).isEqualTo("http://acme.com/vagas?x=1");
        assertThat(CompanyService.normalizarSite("HTTPS://Acme.com")).isEqualTo("HTTPS://Acme.com");
    }

    @Test
    void normalizarSite_recusaEsquemaPerigosoOuHostInvalido() {
        for (String ruim : List.of("javascript:alert(1)", "javascript://acme.com/%0aalert(1)", "data:text/html,oi",
                                   "ftp://acme.com", "https://localhost", "acme", "https://usuario:senha@acme.com",
                                   "https://acme .com", "https://.com")) {
            assertThatThrownBy(() -> CompanyService.normalizarSite(ruim))
                .as(ruim)
                .isInstanceOf(CompanyRuleException.class)
                .extracting("errorCode").isEqualTo("COMPANY_SITE_INVALID");
        }
    }

    // ------------------------------------------------------------- logo

    @Test
    void trocarLogo_salvaANovaEApagaAAntiga() {
        UUID antiga = UUID.randomUUID();
        acme.setLogoId(antiga);
        Imagem nova = new Imagem();
        nova.setId(UUID.randomUUID());
        byte[] bytes = {1, 2, 3};
        when(imageService.salvar(bytes)).thenReturn(nova);

        service.trocarLogo(admin, bytes);

        assertThat(acme.getLogoId()).isEqualTo(nova.getId());
        verify(imageService).apagar(antiga);
    }

    @Test
    void trocarLogo_imagemRecusadaMantemALogoAtual() {
        UUID atual = UUID.randomUUID();
        acme.setLogoId(atual);
        when(imageService.salvar(any())).thenThrow(ImageRuleException.formatoInvalido());

        assertThatThrownBy(() -> service.trocarLogo(admin, new byte[]{9}))
            .isInstanceOf(ImageRuleException.class);
        assertThat(acme.getLogoId()).isEqualTo(atual);
        verify(imageService, never()).apagar(any());
    }

    @Test
    void trocarLogo_recrutadorComumNaoTroca() {
        User comum = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        assertThatThrownBy(() -> service.trocarLogo(comum, new byte[]{1}))
            .isInstanceOf(CompanyRuleException.class);
        verify(imageService, never()).salvar(any());
    }

    @Test
    void removerLogo_desvinculaEApaga() {
        UUID atual = UUID.randomUUID();
        acme.setLogoId(atual);

        service.removerLogo(admin);

        assertThat(acme.getLogoId()).isNull();
        verify(imageService).apagar(atual);
    }

    // ---------------------------------------------------------- fundação

    @Test
    void fundarEmpresa_criaAEmpresaESalvaOUsuarioComoAdmin() {
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        User novo = new User();

        service.fundarEmpresa(novo, "  Acme  ", "11.222.333/0001-81");

        assertThat(novo.getCompany().getNome()).isEqualTo("Acme");
        assertThat(novo.getCompany().getCnpj()).isEqualTo("11222333000181");
        assertThat(novo.getPapelEmpresa()).isEqualTo(PapelEmpresa.ADMIN);
        verify(userRepository).save(novo);
    }

    @Test
    void criarEmpresa_quemJaTemEmpresaNaoCria() {
        assertThatThrownBy(() -> service.criarEmpresa(admin, "Outra", null))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_ALREADY_EXISTS");
    }

    // ------------------------------------------------------------ convites

    @Test
    void convidar_soAdmin() {
        User recrutador = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        assertThatThrownBy(() -> service.convidar(recrutador, "novo@x.com", 3))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_ADMIN_REQUIRED");
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void convidar_emailQueJaTemContaRetornaConflito() {
        when(userRepository.existsByEmail("ja@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.convidar(admin, "ja@x.com", 3))
            .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void convidar_geraLinkComValidadeEscolhidaECancelaOPendenteAnterior() {
        CompanyInvite anterior = new CompanyInvite();
        anterior.setExpiraEm(OffsetDateTime.now().plusDays(1));
        when(inviteRepository.findByCompanyAndEmailIgnoreCase(acme, "novo@x.com")).thenReturn(List.of(anterior));
        when(mailer.sendTeamInvite(eq("novo@x.com"), eq("Acme"), eq("Ana"), anyString(), any())).thenReturn(false);

        CreatedTeamInviteResponse resposta = service.convidar(admin, " novo@x.com ", 5);

        // O anterior deixa de valer: vale sempre o convite mais recente.
        assertThat(anterior.getCanceladoEm()).isNotNull();

        ArgumentCaptor<CompanyInvite> salvo = ArgumentCaptor.forClass(CompanyInvite.class);
        verify(inviteRepository, org.mockito.Mockito.times(2)).save(salvo.capture());
        CompanyInvite novo = salvo.getAllValues().get(1);
        assertThat(novo.getEmail()).isEqualTo("novo@x.com");
        assertThat(novo.getExpiraEm()).isCloseTo(OffsetDateTime.now().plusDays(5), within(1, MINUTES));

        // O link carrega o token; o banco só guarda o hash dele.
        assertThat(resposta.link()).startsWith("https://app.exemplo.com/convite-equipe/");
        String token = resposta.link().substring(resposta.link().lastIndexOf('/') + 1);
        assertThat(novo.getTokenHash()).isEqualTo(SecureTokens.hash(token));
        assertThat(resposta.emailEnviado()).isFalse();
    }

    @Test
    void aceitarConvite_criaRecrutadorNaEmpresaEMarcaOConviteComoUsado() {
        CompanyInvite invite = new CompanyInvite();
        invite.setCompany(acme);
        invite.setEmail("bia@x.com");
        invite.setExpiraEm(OffsetDateTime.now().plusDays(2));
        when(inviteRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(invite));
        when(passwordEncoder.encode("segredo1")).thenReturn("hash");

        User criado = service.aceitarConvite("tok", "Bia", "segredo1");

        assertThat(criado.getEmail()).isEqualTo("bia@x.com");
        assertThat(criado.getTipo()).isEqualTo(TipoConta.RECRUTADOR);
        assertThat(criado.getCompany()).isSameAs(acme);
        assertThat(criado.getPapelEmpresa()).isEqualTo(PapelEmpresa.RECRUTADOR);
        assertThat(invite.getAceitoEm()).isNotNull();
    }

    @Test
    void aceitarConvite_expiradoEhInvalido() {
        CompanyInvite invite = new CompanyInvite();
        invite.setCompany(acme);
        invite.setExpiraEm(OffsetDateTime.now().minusMinutes(1));
        when(inviteRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.aceitarConvite("tok", "Bia", "segredo1"))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_INVITE_INVALID");
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------- equipe

    @Test
    void alterarPapel_naoDeixaAEmpresaSemAdmin() {
        when(userRepository.countByCompanyAndPapelEmpresa(acme, PapelEmpresa.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarPapel(admin, admin.getId(), PapelEmpresa.RECRUTADOR))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_LAST_ADMIN");
    }

    @Test
    void alterarPapel_promoveEDepoisPermiteOAdminOriginalSeRebaixar() {
        User bia = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        service.alterarPapel(admin, bia.getId(), PapelEmpresa.ADMIN);
        assertThat(bia.getPapelEmpresa()).isEqualTo(PapelEmpresa.ADMIN);

        when(userRepository.countByCompanyAndPapelEmpresa(acme, PapelEmpresa.ADMIN)).thenReturn(2L);
        service.alterarPapel(admin, admin.getId(), PapelEmpresa.RECRUTADOR);
        assertThat(admin.getPapelEmpresa()).isEqualTo(PapelEmpresa.RECRUTADOR);
    }

    @Test
    void removerMembro_naoRemoveASiMesmo() {
        assertThatThrownBy(() -> service.removerMembro(admin, admin.getId()))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_CANNOT_REMOVE_SELF");
    }

    @Test
    void removerMembro_deOutraEmpresaNaoEncontra() {
        Company outra = new Company();
        outra.setId(UUID.randomUUID());
        User estranho = membro("Zé", PapelEmpresa.RECRUTADOR, outra);

        assertThatThrownBy(() -> service.removerMembro(admin, estranho.getId()))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_MEMBER_NOT_FOUND");
        assertThat(estranho.getCompany()).isSameAs(outra);
    }

    @Test
    void removerMembro_tiraDaEmpresaMasMantemAConta() {
        User bia = membro("Bia", PapelEmpresa.RECRUTADOR, acme);

        service.removerMembro(admin, bia.getId());

        assertThat(bia.getCompany()).isNull();
        assertThat(bia.getPapelEmpresa()).isNull();
        verify(userRepository).save(bia);
        verify(userRepository, never()).delete(any());
    }

    // ---------------------------------------------- exclusão de conta

    @Test
    void antesDeExcluirConta_unicoMembroLevaAEmpresaJunto() {
        when(userRepository.countByCompany(acme)).thenReturn(1L);

        service.antesDeExcluirConta(admin);

        verify(inviteRepository).deleteByCompany(acme);
        verify(companyRepository).delete(acme);
    }

    @Test
    void antesDeExcluirConta_ultimoAdminComOutrosMembrosEhBarrado() {
        when(userRepository.countByCompany(acme)).thenReturn(3L);
        when(userRepository.countByCompanyAndPapelEmpresa(acme, PapelEmpresa.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.antesDeExcluirConta(admin))
            .isInstanceOf(CompanyRuleException.class)
            .extracting("errorCode").isEqualTo("COMPANY_LAST_ADMIN");
        verify(companyRepository, never()).delete(any());
    }

    @Test
    void antesDeExcluirConta_recrutadorComumSaiSemMexerNaEmpresa() {
        User bia = membro("Bia", PapelEmpresa.RECRUTADOR, acme);
        when(userRepository.countByCompany(acme)).thenReturn(2L);

        service.antesDeExcluirConta(bia);

        verify(inviteRepository).desvincularCriador(bia);
        verify(companyRepository, never()).delete(any());
    }
}
