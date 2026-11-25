package com.avanade.cesar.validacao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosDocumentoDTO(
        String nome,
        String cpf,
        String endereco,
        String cidade,
        String estado,
        String dataEmissao
) {}
