package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.ai.ResumeTextExtractor;
import com.guilherme.entrevistaia.exception.ResumeParseException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Implementação concreta de ResumeTextExtractor usando Apache PDFBox — a
// única dependência nova só pra isso (ver pom.xml). Qualquer falha de leitura
// (PDF corrompido, protegido por senha, arquivo que não é PDF de verdade)
// vira ResumeParseException, tratada pelo GlobalExceptionHandler como 400.
@Component
public class PdfBoxResumeTextExtractor implements ResumeTextExtractor {

    @Override
    public String extract(byte[] arquivoPdf) {
        try (PDDocument document = Loader.loadPDF(arquivoPdf)) {
            String texto = new PDFTextStripper().getText(document).trim();
            if (texto.isBlank()) {
                throw new ResumeParseException("Não foi possível extrair texto do PDF enviado (arquivo vazio ou só com imagens).");
            }
            return texto;
        } catch (IOException e) {
            throw new ResumeParseException("Não foi possível ler o arquivo enviado. Confirme que é um PDF válido.");
        }
    }
}
