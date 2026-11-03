package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.repository.ImagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

// O tipo da imagem sai dos primeiros bytes; tamanho e formato são barrados antes de salvar.
@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    @Mock private ImagemRepository repository;
    private ImageService service;

    @BeforeEach
    void setUp() {
        service = new ImageService(repository);
        lenient().when(repository.save(any(Imagem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void detectaPngJpegEWebp() {
        assertThat(service.salvar(PNG).getContentType()).isEqualTo("image/png");
        assertThat(service.salvar(JPEG).getContentType()).isEqualTo("image/jpeg");
        assertThat(service.salvar(WEBP).getContentType()).isEqualTo("image/webp");
    }

    @Test
    void recusaSvgEOutrosFormatos() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
            .getBytes(StandardCharsets.UTF_8);
        byte[] gif = "GIF89a....".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> service.salvar(svg)).isInstanceOf(ImageRuleException.class)
            .extracting("errorCode").isEqualTo("IMAGE_INVALID");
        assertThatThrownBy(() -> service.salvar(gif)).isInstanceOf(ImageRuleException.class);
    }

    @Test
    void recusaVaziaEGrandeDemais() {
        byte[] grande = Arrays.copyOf(PNG, ImageService.MAX_BYTES + 1);

        assertThatThrownBy(() -> service.salvar(new byte[0])).isInstanceOf(ImageRuleException.class);
        assertThatThrownBy(() -> service.salvar(grande)).isInstanceOf(ImageRuleException.class)
            .extracting("errorCode").isEqualTo("IMAGE_TOO_LARGE");
    }

    @Test
    void aceitaNoLimiteExato() {
        byte[] noLimite = Arrays.copyOf(PNG, ImageService.MAX_BYTES);

        assertThat(service.salvar(noLimite).getTamanho()).isEqualTo(ImageService.MAX_BYTES);
    }
}
