package com.guilherme.entrevistaia.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.AiWeaknessGrouper;
import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse;
import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse.ProximoTreino;
import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse.Tema;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.InterviewStatus;
import com.guilherme.entrevistaia.entity.PontosFracosRecorrentes;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.repository.InterviewRepository;
import com.guilherme.entrevistaia.repository.PontosFracosRecorrentesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Pontos fracos que se repetem entre os treinos (v1.3, ideia 2).
//
// Junta os pontos fracos dos últimos MAX_TREINOS relatórios de treino e pede
// para a IA agrupá-los em temas (o mesmo problema vem escrito de jeitos
// diferentes em cada relatório). O resultado fica em cache por candidato e só
// é refeito quando muda o conjunto de relatórios: na prática, uma chamada de
// IA por relatório novo.
//
// Sem @Transactional de propósito: a chamada de IA leva segundos e não deve
// segurar uma conexão do banco. As leituras preguiçosas (report, pontosFracos)
// funcionam pelo open-in-view (ver application.yml), e o save do cache roda na
// transação do próprio repositório.
@Service
public class PontosFracosService {

    private static final Logger log = LoggerFactory.getLogger(PontosFracosService.class);

    static final int MAX_TREINOS = 10;
    static final int MIN_TREINOS = 2;
    static final int MAX_TEMAS = 5;
    // Quantos temas entram no foco do próximo treino sugerido.
    static final int TEMAS_NO_FOCO = 3;

    private final InterviewRepository interviewRepository;
    private final PontosFracosRecorrentesRepository cacheRepository;
    private final AiWeaknessGrouper grouper;
    private final ObjectMapper objectMapper;

    public PontosFracosService(InterviewRepository interviewRepository,
                               PontosFracosRecorrentesRepository cacheRepository,
                               AiWeaknessGrouper grouper,
                               ObjectMapper objectMapper) {
        this.interviewRepository = interviewRepository;
        this.cacheRepository = cacheRepository;
        this.grouper = grouper;
        this.objectMapper = objectMapper;
    }

    public PontosFracosRecorrentesResponse recorrentes(User user) {
        // Do mais recente ao mais antigo: o "Treino 1" para a IA é o último feito.
        List<Interview> treinos = interviewRepository.findByUserOrderByCriadoEmDesc(user).stream()
            .filter(i -> !i.deConvite() && i.getStatus() == InterviewStatus.FINALIZADA)
            .filter(i -> i.getReport() != null && !i.getReport().getPontosFracos().isEmpty())
            .sorted(Comparator.comparing(PontosFracosService::dataDeTermino).reversed())
            .limit(MAX_TREINOS)
            .toList();

        if (treinos.size() < MIN_TREINOS) {
            return new PontosFracosRecorrentesResponse(treinos.size(), List.of(), null);
        }

        String chave = chave(treinos);
        Optional<PontosFracosRecorrentes> cache = cacheRepository.findById(user.getId());
        if (cache.isPresent() && cache.get().getChave().equals(chave)) {
            try {
                return objectMapper.readValue(cache.get().getResultado(), PontosFracosRecorrentesResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("[WEAKNESS_CACHE_UNREADABLE] userId={} refazendo", user.getId());
            }
        }

        log.info("[AI_WEAKNESS_GROUP_REQUEST] userId={} treinos={}", user.getId(), treinos.size());
        List<AiWeaknessGrouper.Treino> entrada = IntStream.range(0, treinos.size())
            .mapToObj(i -> {
                Interview t = treinos.get(i);
                return new AiWeaknessGrouper.Treino(i + 1, t.getStack(), t.getNivel(), t.getReport().getPontosFracos());
            })
            .toList();
        PontosFracosRecorrentesResponse resposta = montar(treinos, grouper.agrupar(entrada));
        log.info("[AI_WEAKNESS_GROUPED] userId={} temas={}", user.getId(), resposta.temas().size());

        salvarCache(user, chave, resposta);
        return resposta;
    }

    // A IA pode errar: tema sem nome, número de treino inexistente ou repetido,
    // tema que aparece num treino só, o mesmo tema duas vezes. Aqui só passa o
    // que é recorrente de fato, e cada nome aparece uma vez só.
    static PontosFracosRecorrentesResponse montar(List<Interview> treinos, List<AiWeaknessGrouper.Tema> brutos) {
        // Temas com o mesmo nome (sem diferenciar maiúsculas) viram um só: vale
        // o primeiro nome e a primeira descrição não vazia, e os treinos somam.
        Map<String, String> nomes = new LinkedHashMap<>();
        Map<String, String> descricoes = new LinkedHashMap<>();
        Map<String, TreeSet<Integer>> numeros = new LinkedHashMap<>();
        for (AiWeaknessGrouper.Tema b : brutos) {
            String nome = b.tema() == null ? "" : b.tema().trim();
            if (nome.isEmpty()) continue;
            String k = nome.toLowerCase(Locale.ROOT);
            nomes.putIfAbsent(k, nome);
            String descricao = b.descricao() == null ? "" : b.descricao().trim();
            if (!descricao.isEmpty()) descricoes.putIfAbsent(k, descricao);
            TreeSet<Integer> nums = numeros.computeIfAbsent(k, x -> new TreeSet<>());
            if (b.treinos() != null) {
                b.treinos().stream().filter(n -> n != null && n >= 1 && n <= treinos.size()).forEach(nums::add);
            }
        }

        List<Tema> temas = nomes.keySet().stream()
            .map(k -> {
                List<UUID> ids = numeros.get(k).stream().map(n -> treinos.get(n - 1).getId()).toList();
                return new Tema(nomes.get(k), descricoes.getOrDefault(k, ""), ids.size(), ids);
            })
            .filter(t -> t.ocorrencias() >= MIN_TREINOS)
            // Ordenação estável: em caso de empate, vale a ordem da IA.
            .sorted(Comparator.comparingInt(Tema::ocorrencias).reversed())
            .limit(MAX_TEMAS)
            .toList();

        return new PontosFracosRecorrentesResponse(treinos.size(), temas, proximoTreino(treinos, temas));
    }

    // Stack mais frequente entre os treinos do tema principal (no empate, a do
    // treino mais recente); nível e quantidade de perguntas vêm do último
    // treino dessa stack. O foco junta os principais temas.
    private static ProximoTreino proximoTreino(List<Interview> treinos, List<Tema> temas) {
        if (temas.isEmpty()) return null;

        Map<UUID, Interview> porId = treinos.stream().collect(Collectors.toMap(Interview::getId, i -> i));
        Map<String, Long> contagem = new LinkedHashMap<>();
        temas.get(0).interviewIds().forEach(id ->
            contagem.merge(chaveStack(porId.get(id).getStack()), 1L, Long::sum));
        long maximo = Collections.max(contagem.values());
        String stack = contagem.entrySet().stream()
            .filter(e -> e.getValue() == maximo).findFirst().orElseThrow().getKey();

        Interview base = treinos.stream()
            .filter(t -> chaveStack(t.getStack()).equals(stack))
            .findFirst().orElseThrow();

        String foco = temas.stream().limit(TEMAS_NO_FOCO)
            .map(t -> t.descricao().isEmpty() ? t.tema() : t.tema() + ": " + t.descricao())
            .collect(Collectors.joining("; "));

        return new ProximoTreino(base.getStack().trim(), base.getNivel(), base.getTotalPerguntas(), foco);
    }

    private void salvarCache(User user, String chave, PontosFracosRecorrentesResponse resposta) {
        try {
            PontosFracosRecorrentes cache = new PontosFracosRecorrentes();
            cache.setUserId(user.getId());
            cache.setChave(chave);
            cache.setResultado(objectMapper.writeValueAsString(resposta));
            cache.setCriadoEm(OffsetDateTime.now());
            cacheRepository.saveAndFlush(cache);
        } catch (JsonProcessingException | DataIntegrityViolationException e) {
            // Duas abas pedindo ao mesmo tempo: a outra já salvou. A resposta
            // desta continua válida; na próxima, o cache é lido.
            log.warn("[WEAKNESS_CACHE_NOT_SAVED] userId={} motivo={}", user.getId(), e.getClass().getSimpleName());
        }
    }

    private static OffsetDateTime dataDeTermino(Interview i) {
        return i.getFinalizadoEm() != null ? i.getFinalizadoEm() : i.getCriadoEm();
    }

    private static String chaveStack(String stack) {
        return stack.trim().toLowerCase(Locale.ROOT);
    }

    // Hash dos ids dos relatórios, na ordem: muda quando entra um relatório novo.
    static String chave(List<Interview> treinos) {
        String ids = treinos.stream().map(t -> String.valueOf(t.getReport().getId())).collect(Collectors.joining(","));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(ids.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
