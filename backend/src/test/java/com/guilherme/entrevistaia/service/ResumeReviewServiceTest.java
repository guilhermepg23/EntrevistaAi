package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.ai.AiResumeReviewResult;
import com.guilherme.entrevistaia.ai.AiResumeReviewer;
import com.guilherme.entrevistaia.ai.ResumeTextExtractor;
import com.guilherme.entrevistaia.entity.ResumeReview;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.entity.VeredictoCurriculo;
import com.guilherme.entrevistaia.exception.ResumeParseException;
import com.guilherme.entrevistaia.repository.ResumeReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testa a lógica do ResumeReviewService com repository, extrator de PDF e o
// reviewer de IA mockados — só nos interessa que ele orquestra as três coisas
// na ordem certa, copia o resultado da IA pra entidade e persiste.
@ExtendWith(MockitoExtension.class)
class ResumeReviewServiceTest {

    @Mock private ResumeReviewRepository resumeReviewRepository;
    @Mock private ResumeTextExtractor resumeTextExtractor;
    @Mock private AiResumeReviewer resumeReviewer;

    private ResumeReviewService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ResumeReviewService(resumeReviewRepository, resumeTextExtractor, resumeReviewer);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("candidato@teste.com");
    }

    @Test
    void review_extraiTextoChamaIaCopiaCamposESalva() {
        when(resumeTextExtractor.extract(any())).thenReturn("texto extraido do pdf");
        when(resumeReviewer.review("texto extraido do pdf")).thenReturn(new AiResumeReviewResult(
            7, VeredictoCurriculo.BOM, "resumo da leitura",
            List.of("ponto forte"), List.of("melhoria 1", "melhoria 2")));

        ResumeReview review = service.review(user, "pdf-bytes".getBytes());

        assertThat(review.getUser()).isEqualTo(user);
        assertThat(review.getNota()).isEqualTo(7);
        assertThat(review.getVeredito()).isEqualTo(VeredictoCurriculo.BOM);
        assertThat(review.getResumo()).isEqualTo("resumo da leitura");
        assertThat(review.getPontosFortes()).containsExactly("ponto forte");
        assertThat(review.getMelhorias()).containsExactly("melhoria 1", "melhoria 2");
        assertThat(review.getCriadoEm()).isNotNull();
        verify(resumeReviewRepository).save(review);
    }

    @Test
    void review_propagaResumeParseExceptionENaoChamaIaNemSalva() {
        when(resumeTextExtractor.extract(any()))
            .thenThrow(new ResumeParseException("Não foi possível ler o arquivo enviado. Confirme que é um PDF válido."));

        assertThatThrownBy(() -> service.review(user, "nao-e-pdf".getBytes()))
            .isInstanceOf(ResumeParseException.class);

        verify(resumeReviewer, never()).review(any());
        verify(resumeReviewRepository, never()).save(any());
    }

    @Test
    void listByUser_delegaProRepositoryOrdenadoPorData() {
        ResumeReview r1 = new ResumeReview();
        ResumeReview r2 = new ResumeReview();
        when(resumeReviewRepository.findByUserOrderByCriadoEmDesc(user)).thenReturn(List.of(r1, r2));

        assertThat(service.listByUser(user)).containsExactly(r1, r2);
    }
}
