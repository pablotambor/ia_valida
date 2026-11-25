package com.avanade.cesar.validacao.service;

import com.avanade.cesar.validacao.dto.DadosDocumentoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidacaoRegraService {

    @Value("${app.validacao.prazo-maximo-dias:90}")
    private long prazoMaximoDias = 90;

    @Value("${app.validacao.tamanho-minimo-ocr:20}")
    private int tamanhoMinimoOcr = 20;

    public List<String> validarComprovanteResidencia(
            String textoOcr,
            DadosDocumentoDTO dados,
            String cpfEsperado,
            LocalDate dataReferencia
    ) {
        List<String> motivos = new ArrayList<>();

        // Regra 1: Legibilidade mínima do documento
        if (textoOcr == null || textoOcr.trim().length() < tamanhoMinimoOcr) {
            motivos.add("Documento ilegível ou com texto insuficiente extraído pelo OCR.");
        }

        if (dados == null) {
            motivos.add("Não foi possível extrair os dados estruturados do documento.");
            return motivos;
        }

        // Regra 2: Presença dos campos obrigatórios
        if (isBlank(dados.nome())) {
            motivos.add("Campo obrigatório ausente: Nome completo.");
        }
        if (isBlank(dados.endereco())) {
            motivos.add("Campo obrigatório ausente: Endereço.");
        }
        if (isBlank(dados.cidade())) {
            motivos.add("Campo obrigatório ausente: Cidade.");
        }
        if (isBlank(dados.estado())) {
            motivos.add("Campo obrigatório ausente: Estado (UF).");
        }

        // Regra 3: CPF obrigatório, 11 dígitos e dígitos verificadores válidos
        if (isBlank(dados.cpf())) {
            motivos.add("Campo obrigatório ausente: CPF.");
        } else {
            String cpfLimpo = dados.cpf().replaceAll("\\D", "");
            if (!isCpfValido(cpfLimpo)) {
                motivos.add("CPF inválido (deve conter 11 dígitos e passar na verificação dos dígitos verificadores).");
            } else if (!isBlank(cpfEsperado)) {
                String esperadoLimpo = cpfEsperado.replaceAll("\\D", "");
                if (!cpfLimpo.equals(esperadoLimpo)) {
                    motivos.add("CPF extraído diverge do CPF informado pelo titular.");
                }
            }
        }

        // Regra 4: Data de emissão válida, não futura e dentro do prazo de 90 dias
        if (isBlank(dados.dataEmissao())) {
            motivos.add("Campo obrigatório ausente: Data de emissão.");
        } else {
            try {
                LocalDate emissao = LocalDate.parse(dados.dataEmissao().trim());
                if (emissao.isAfter(dataReferencia)) {
                    motivos.add("Data de emissão inválida: está no futuro (" + emissao + ").");
                } else {
                    long dias = ChronoUnit.DAYS.between(emissao, dataReferencia);
                    if (dias > prazoMaximoDias) {
                        motivos.add("Comprovante de residência vencido: emitido há " + dias
                                + " dias (prazo máximo: " + prazoMaximoDias + " dias).");
                    }
                }
            } catch (DateTimeParseException ex) {
                motivos.add("Data de emissão em formato inválido (esperado YYYY-MM-DD): " + dados.dataEmissao());
            }
        }

        return motivos;
    }

    /**
     * Validação oficial dos dois dígitos verificadores do CPF brasileiro.
     */
    public boolean isCpfValido(String cpf) {
        if (cpf == null) return false;
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }

        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += Character.getNumericValue(digits.charAt(i)) * (10 - i);
        }
        int dv1 = 11 - (soma1 % 11);
        if (dv1 >= 10) dv1 = 0;

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += Character.getNumericValue(digits.charAt(i)) * (11 - i);
        }
        int dv2 = 11 - (soma2 % 11);
        if (dv2 >= 10) dv2 = 0;

        return dv1 == Character.getNumericValue(digits.charAt(9))
                && dv2 == Character.getNumericValue(digits.charAt(10));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
