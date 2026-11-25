package com.avanade.cesar.validacao.service;

import com.avanade.cesar.validacao.dto.DadosDocumentoDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidacaoRegraServiceTest {

    private final ValidacaoRegraService service = new ValidacaoRegraService();
    private final String textoValido = "CONTA DE ENERGIA - COMPROVANTE DE RESIDENCIA COMPLETO E LEGIVEL";

    @Test
    void deveAprovarDocumentoQuandoTodosOsCriteriosForemAtendidos() {
        DadosDocumentoDTO dados = new DadosDocumentoDTO(
                "Maria Fernanda Silva",
                "52998224725",
                "Av. Boa Viagem, 1450",
                "Recife",
                "PE",
                "2026-10-20"
        );

        List<String> motivos = service.validarComprovanteResidencia(
                textoValido, dados, "52998224725", LocalDate.of(2026, 11, 25)
        );

        assertTrue(motivos.isEmpty(), "Era esperado documento aprovado, mas houve motivos: " + motivos);
    }

    @Test
    void deveRejeitarQuandoCampoObrigatorioEstiverAusenteOuCpfInvalido() {
        DadosDocumentoDTO dados = new DadosDocumentoDTO(
                "Carlos Henrique",
                "11111111111",
                null,
                "Olinda",
                "PE",
                "2026-11-10"
        );

        List<String> motivos = service.validarComprovanteResidencia(
                textoValido, dados, null, LocalDate.of(2026, 11, 25)
        );

        assertFalse(motivos.isEmpty());
        assertTrue(motivos.stream().anyMatch(m -> m.contains("Endereço")));
        assertTrue(motivos.stream().anyMatch(m -> m.contains("CPF inválido")));
    }

    @Test
    void deveRejeitarQuandoDataDeEmissaoForSuperiorA90Dias() {
        DadosDocumentoDTO dados = new DadosDocumentoDTO(
                "Joao Pedro Alves",
                "52998224725",
                "Rua da Aurora, 300",
                "Recife",
                "PE",
                "2026-06-01"
        );

        List<String> motivos = service.validarComprovanteResidencia(
                textoValido, dados, null, LocalDate.of(2026, 11, 25)
        );

        assertEquals(1, motivos.size());
        assertTrue(motivos.get(0).contains("vencido"));
    }
}
