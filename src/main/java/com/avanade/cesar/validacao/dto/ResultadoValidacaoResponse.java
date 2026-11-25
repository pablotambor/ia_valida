package com.avanade.cesar.validacao.dto;

import com.avanade.cesar.validacao.model.StatusValidacao;
import com.avanade.cesar.validacao.model.TipoDocumento;
import com.avanade.cesar.validacao.model.ValidacaoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ResultadoValidacaoResponse(
        Long id,
        String nomeArquivo,
        TipoDocumento tipoDocumento,
        StatusValidacao status,
        DadosDocumentoDTO dadosExtraidos,
        List<String> motivosRejeicao,
        LocalDateTime dataAnalise
) {
    public static ResultadoValidacaoResponse fromEntity(ValidacaoDocumento entity) {
        LocalDate data = entity.getDataEmissaoExtraida();
        DadosDocumentoDTO dados = new DadosDocumentoDTO(
                entity.getNomeExtraido(),
                entity.getCpfExtraido(),
                entity.getEnderecoExtraido(),
                entity.getCidadeExtraida(),
                entity.getEstadoExtraido(),
                data != null ? data.toString() : null
        );
        return new ResultadoValidacaoResponse(
                entity.getId(),
                entity.getNomeArquivo(),
                entity.getTipoDocumento(),
                entity.getStatus(),
                dados,
                entity.getMotivosRejeicao(),
                entity.getDataAnalise()
        );
    }
}
