package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.service.ImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

// GET /images/{id} — público: a logo aparece no convite de teste, que o
// candidato abre antes de entrar. O id é um UUID aleatório e muda a cada
// troca, então o cache pode ser longo e "immutable".
@RestController
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        Imagem imagem = imageService.buscar(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imagem.getContentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(imagem.getConteudo());
    }
}
