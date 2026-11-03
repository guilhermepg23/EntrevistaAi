package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.repository.ImagemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

// Guarda e serve as imagens pequenas (logo da empresa, foto dos recrutadores).
//
// O tipo é decidido pelos primeiros bytes, nunca pelo Content-Type ou pela
// extensão que o cliente manda: assim nada além de PNG, JPEG e WebP é aceito.
// SVG fica de fora de propósito, porque pode carregar script.
@Service
public class ImageService {

    public static final int MAX_BYTES = 300 * 1024;

    private final ImagemRepository repository;

    public ImageService(ImagemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Imagem salvar(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw ImageRuleException.vazia();
        }
        if (conteudo.length > MAX_BYTES) {
            throw ImageRuleException.grandeDemais(MAX_BYTES / 1024);
        }
        String tipo = detectarTipo(conteudo);
        if (tipo == null) {
            throw ImageRuleException.formatoInvalido();
        }
        Imagem imagem = new Imagem();
        imagem.setConteudo(conteudo);
        imagem.setContentType(tipo);
        imagem.setTamanho(conteudo.length);
        imagem.setCriadoEm(OffsetDateTime.now());
        return repository.save(imagem);
    }

    @Transactional(readOnly = true)
    public Imagem buscar(UUID id) {
        return repository.findById(id).orElseThrow(ImageRuleException::naoEncontrada);
    }

    // Chamado depois de desvincular a imagem do dono (a FK é "on delete set null").
    @Transactional
    public void apagar(UUID id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }

    static String detectarTipo(byte[] b) {
        if (comeca(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (comeca(b, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (b.length >= 12 && comeca(b, 'R', 'I', 'F', 'F')
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean comeca(byte[] b, int... prefixo) {
        if (b.length < prefixo.length) {
            return false;
        }
        for (int i = 0; i < prefixo.length; i++) {
            if ((b[i] & 0xFF) != prefixo[i]) {
                return false;
            }
        }
        return true;
    }
}
