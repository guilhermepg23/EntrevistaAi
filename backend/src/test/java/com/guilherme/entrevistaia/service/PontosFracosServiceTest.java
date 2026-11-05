package com.guilherme.entrevistaia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.AiWeaknessGrouper;
import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse;
import com.guilherme.entrevistaia.entity.FeedbackReport;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.InterviewStatus;
import com.guilherme.entrevistaia.entity.OrigemEntrevista;
import com.guilherme.entrevistaia.entity.PontosFracosRecorrentes;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.PontosFracosRecorrentesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontosFracosServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private PontosFracosRecorrentesRepository cacheRepository;
    @Mock private AiWeaknessGrouper grouper;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PontosFracosService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PontosFracosService(interviewRepository, cacheRepository, grouper, objectMapper);
        user = new User();
        user.setId(UUID.randomUUID());
    }

    private Interview treino(String stack, String nivel, int dia, String... pontosFracos) {
        Interview i = new Interview();
        i.setId(UUID.randomUUID());
        i.setUser(user);
        i.setStack(stack);
        i.setNivel(nivel);
        i.setTotalPerguntas(8);
        i.setStatus(InterviewStatus.FINALIZADA);
        i.setCriadoEm(OffsetDateTime.parse("2026-10-01T10:00:00Z"));
        i.setFinalizadoEm(OffsetDateTime.parse("2026-10-01T10:00:00Z").plusDays(dia));
        FeedbackReport r = new FeedbackReport();
        r.setId(UUID.randomUUID());
        r.setPontosFracos(new ArrayList<>(List.of(pontosFracos)));
        i.setReport(r);
        return i;
    }

    @Test
    void comMenosDeDoisTreinosComRelatorio_naoChamaAIa() {
        Interview semRelatorio = treino("Java", "pleno", 2, "x");
        semRelatorio.setReport(null);
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user))
            .thenReturn(List.of(treino("Java", "pleno", 1, "transações"), semRelatorio));

        PontosFracosRecorrentesResponse r = service.recorrentes(user);

        assertThat(r.treinosAnalisados()).isEqualTo(1);
        assertThat(r.temas()).isEmpty();
        assertThat(r.proximoTreino()).isNull();
        verify(grouper, never()).agrupar(any());
    }

    @Test
    void deixaDeForaConviteEmAndamentoERelatorioSemPontosFracos() {
        Interview convite = treino("Java", "pleno", 1, "a");
        convite.setOrigem(OrigemEntrevista.CONVITE);
        Interview andamento = treino("Java", "pleno", 2, "b");
        andamento.setStatus(InterviewStatus.EM_ANDAMENTO);
        Interview vazio = treino("Java", "pleno", 3);
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user))
            .thenReturn(List.of(convite, andamento, vazio, treino("Go", "junior", 4, "c")));

        assertThat(service.recorrentes(user).treinosAnalisados()).isEqualTo(1);
        verify(grouper, never()).agrupar(any());
    }

    @Test
    void mandaOsTreinosDoMaisRecenteAoMaisAntigoEMapeiaOsTemas() {
        Interview antigo = treino("Java", "pleno", 1, "não explicou transações");
        Interview meio = treino("React", "junior", 2, "hooks confusos");
        Interview recente = treino("java ", "senior", 3, "pouca clareza sobre @Transactional");
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user)).thenReturn(List.of(meio, antigo, recente));
        when(cacheRepository.findById(user.getId())).thenReturn(Optional.empty());
        when(grouper.agrupar(any())).thenReturn(List.of(
            new AiWeaknessGrouper.Tema("Transações no Spring", "Revise propagação e rollback.", List.of(3, 1))));

        PontosFracosRecorrentesResponse r = service.recorrentes(user);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiWeaknessGrouper.Treino>> entrada = ArgumentCaptor.forClass(List.class);
        verify(grouper).agrupar(entrada.capture());
        assertThat(entrada.getValue()).extracting(AiWeaknessGrouper.Treino::numero).containsExactly(1, 2, 3);
        assertThat(entrada.getValue()).extracting(AiWeaknessGrouper.Treino::stack).containsExactly("java ", "React", "Java");

        assertThat(r.treinosAnalisados()).isEqualTo(3);
        assertThat(r.temas()).hasSize(1);
        assertThat(r.temas().get(0).ocorrencias()).isEqualTo(2);
        assertThat(r.temas().get(0).interviewIds()).containsExactly(recente.getId(), antigo.getId());
        // A stack é a do tema (Java, sem diferenciar maiúsculas); nível do treino mais recente dela.
        assertThat(r.proximoTreino().stack()).isEqualTo("java");
        assertThat(r.proximoTreino().nivel()).isEqualTo("senior");
        assertThat(r.proximoTreino().totalPerguntas()).isEqualTo(8);
        assertThat(r.proximoTreino().focoPratica()).isEqualTo("Transações no Spring: Revise propagação e rollback.");
    }

    @Test
    void descartaTemaDeUmTreinoSoNumeroInvalidoENomeVazioEOrdenaPorFrequencia() {
        List<Interview> treinos = List.of(
            treino("Go", "pleno", 3, "a"), treino("Go", "pleno", 2, "b"), treino("Go", "pleno", 1, "c"));
        List<AiWeaknessGrouper.Tema> brutos = List.of(
            new AiWeaknessGrouper.Tema("Só num", "x", List.of(1)),
            new AiWeaknessGrouper.Tema("Inventado", "x", List.of(7, 9)),
            new AiWeaknessGrouper.Tema("  ", "x", List.of(1, 2)),
            new AiWeaknessGrouper.Tema("Repetido", "x", List.of(2, 2)),
            new AiWeaknessGrouper.Tema("Dois", "d", List.of(1, 3)),
            new AiWeaknessGrouper.Tema("Três", "t", List.of(1, 2, 3)));

        PontosFracosRecorrentesResponse r = PontosFracosService.montar(treinos, brutos);

        assertThat(r.temas()).extracting(PontosFracosRecorrentesResponse.Tema::tema).containsExactly("Três", "Dois");
        assertThat(r.proximoTreino().focoPratica()).isEqualTo("Três: t; Dois: d");
    }

    @Test
    void juntaTemasComOMesmoNome() {
        List<Interview> treinos = List.of(
            treino("Go", "pleno", 3, "a"), treino("Go", "pleno", 2, "b"), treino("Go", "pleno", 1, "c"));
        List<AiWeaknessGrouper.Tema> brutos = List.of(
            new AiWeaknessGrouper.Tema("Testes", "", List.of(1)),
            new AiWeaknessGrouper.Tema("Concorrência", "c", List.of(1, 2)),
            new AiWeaknessGrouper.Tema(" testes ", "Escreva testes.", List.of(1, 3)));

        PontosFracosRecorrentesResponse r = PontosFracosService.montar(treinos, brutos);

        assertThat(r.temas()).extracting(PontosFracosRecorrentesResponse.Tema::tema).containsExactly("Testes", "Concorrência");
        PontosFracosRecorrentesResponse.Tema testes = r.temas().get(0);
        assertThat(testes.descricao()).isEqualTo("Escreva testes.");
        assertThat(testes.interviewIds()).containsExactly(treinos.get(0).getId(), treinos.get(2).getId());
    }

    @Test
    void semTemaRecorrente_naoSugereTreino() {
        List<Interview> treinos = List.of(treino("Go", "pleno", 2, "a"), treino("Go", "pleno", 1, "b"));

        PontosFracosRecorrentesResponse r = PontosFracosService.montar(treinos, List.of());

        assertThat(r.temas()).isEmpty();
        assertThat(r.proximoTreino()).isNull();
    }

    @Test
    void usaOCacheQuandoOsRelatoriosNaoMudaram() throws Exception {
        List<Interview> treinos = List.of(treino("Go", "pleno", 2, "a"), treino("Go", "pleno", 1, "b"));
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user)).thenReturn(treinos);
        PontosFracosRecorrentesResponse salvo = new PontosFracosRecorrentesResponse(2,
            List.of(new PontosFracosRecorrentesResponse.Tema("Erros", "d", 2, List.of(treinos.get(0).getId(), treinos.get(1).getId()))),
            new PontosFracosRecorrentesResponse.ProximoTreino("Go", "pleno", 8, "Erros: d"));
        PontosFracosRecorrentes cache = new PontosFracosRecorrentes();
        cache.setChave(PontosFracosService.chave(treinos));
        cache.setResultado(objectMapper.writeValueAsString(salvo));
        when(cacheRepository.findById(user.getId())).thenReturn(Optional.of(cache));

        assertThat(service.recorrentes(user)).isEqualTo(salvo);
        verify(grouper, never()).agrupar(any());
    }

    @Test
    void refazQuandoEntraUmRelatorioNovoESalvaOCache() {
        List<Interview> treinos = List.of(treino("Go", "pleno", 2, "a"), treino("Go", "pleno", 1, "b"));
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user)).thenReturn(treinos);
        PontosFracosRecorrentes velho = new PontosFracosRecorrentes();
        velho.setChave("chave-de-outro-conjunto");
        velho.setResultado("{}");
        when(cacheRepository.findById(user.getId())).thenReturn(Optional.of(velho));
        when(grouper.agrupar(any())).thenReturn(List.of());

        service.recorrentes(user);

        ArgumentCaptor<PontosFracosRecorrentes> salvo = ArgumentCaptor.forClass(PontosFracosRecorrentes.class);
        verify(cacheRepository).saveAndFlush(salvo.capture());
        assertThat(salvo.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(salvo.getValue().getChave()).isEqualTo(PontosFracosService.chave(treinos));
    }

    @Test
    void falhaAoSalvarOCache_naoDerrubaAResposta() {
        List<Interview> treinos = List.of(treino("Go", "pleno", 2, "a"), treino("Go", "pleno", 1, "b"));
        when(interviewRepository.findByUserOrderByCriadoEmDesc(user)).thenReturn(treinos);
        when(cacheRepository.findById(user.getId())).thenReturn(Optional.empty());
        when(grouper.agrupar(any())).thenReturn(List.of());
        when(cacheRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThat(service.recorrentes(user).treinosAnalisados()).isEqualTo(2);
    }
}
