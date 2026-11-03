package com.guilherme.entrevistaia.exception;

import org.springframework.http.HttpStatus;

// Imagem recusada no upload ou não encontrada (ImageService). Mesmo esquema da
// CompanyRuleException: status HTTP junto e uma fábrica por regra.
public class ImageRuleException extends AppException {

    private final HttpStatus status;

    private ImageRuleException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, null);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ImageRuleException vazia() {
        return new ImageRuleException("IMAGE_INVALID", "Envie uma imagem.", HttpStatus.BAD_REQUEST);
    }

    public static ImageRuleException formatoInvalido() {
        return new ImageRuleException("IMAGE_INVALID",
            "Formato não aceito. Use PNG, JPEG ou WebP.", HttpStatus.BAD_REQUEST);
    }

    public static ImageRuleException grandeDemais(int maxKb) {
        return new ImageRuleException("IMAGE_TOO_LARGE",
            "A imagem passa de " + maxKb + " KB.", HttpStatus.BAD_REQUEST);
    }

    public static ImageRuleException naoEncontrada() {
        return new ImageRuleException("RESOURCE_NOT_FOUND", "Imagem não encontrada.", HttpStatus.NOT_FOUND);
    }
}
