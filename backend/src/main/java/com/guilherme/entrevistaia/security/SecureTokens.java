package com.guilherme.entrevistaia.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Tokens de link enviados por email (recuperação de senha, convite de equipe).
// O token vai no link e só o hash dele é salvo no banco — vazar o banco não
// entrega links usáveis.
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokens() {}

    // 256 bits aleatórios, em Base64 URL-safe (pode ir direto num link).
    public static String gerar() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 em hex — o que fica salvo no banco.
    public static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 é garantido pela plataforma; se faltar, é erro de ambiente.
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
