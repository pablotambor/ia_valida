package com.avanade.cesar.validacao.repository;

import com.avanade.cesar.validacao.model.ValidacaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidacaoDocumentoRepository extends JpaRepository<ValidacaoDocumento, Long> {
    List<ValidacaoDocumento> findAllByOrderByDataAnaliseDesc();
}
