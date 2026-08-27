package com.guilherme.entrevistaia.exception;

// Token de compartilhamento inválido, revogado, ou de uma entrevista que nunca
// gerou link público. Vira HTTP 404 — usado só pelo endpoint público
// (GET /interviews/public/{shareToken}/report), que não exige autenticação.
public class ShareLinkNotFoundException extends AppException {
    public ShareLinkNotFoundException() {
        super("SHARE_LINK_NOT_FOUND", "Link de relatório inválido ou expirado.", null);
    }
}
