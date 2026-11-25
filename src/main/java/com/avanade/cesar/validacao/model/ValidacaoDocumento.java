package com.avanade.cesar.validacao.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "validacoes_documentos")
public class ValidacaoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento = TipoDocumento.COMPROVANTE_RESIDENCIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusValidacao status;

    private String nomeExtraido;
    private String cpfExtraido;
    private String enderecoExtraido;
    private String cidadeExtraida;
    private String estadoExtraido;
    private LocalDate dataEmissaoExtraida;

    @Column(columnDefinition = "TEXT")
    private String textoOcrBruto;

    @Column(columnDefinition = "TEXT")
    private String jsonEstruturado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "validacao_motivos", joinColumns = @JoinColumn(name = "validacao_id"))
    @Column(name = "motivo", length = 500)
    private List<String> motivosRejeicao = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataAnalise = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public StatusValidacao getStatus() { return status; }
    public void setStatus(StatusValidacao status) { this.status = status; }

    public String getNomeExtraido() { return nomeExtraido; }
    public void setNomeExtraido(String nomeExtraido) { this.nomeExtraido = nomeExtraido; }

    public String getCpfExtraido() { return cpfExtraido; }
    public void setCpfExtraido(String cpfExtraido) { this.cpfExtraido = cpfExtraido; }

    public String getEnderecoExtraido() { return enderecoExtraido; }
    public void setEnderecoExtraido(String enderecoExtraido) { this.enderecoExtraido = enderecoExtraido; }

    public String getCidadeExtraida() { return cidadeExtraida; }
    public void setCidadeExtraida(String cidadeExtraida) { this.cidadeExtraida = cidadeExtraida; }

    public String getEstadoExtraido() { return estadoExtraido; }
    public void setEstadoExtraido(String estadoExtraido) { this.estadoExtraido = estadoExtraido; }

    public LocalDate getDataEmissaoExtraida() { return dataEmissaoExtraida; }
    public void setDataEmissaoExtraida(LocalDate dataEmissaoExtraida) { this.dataEmissaoExtraida = dataEmissaoExtraida; }

    public String getTextoOcrBruto() { return textoOcrBruto; }
    public void setTextoOcrBruto(String textoOcrBruto) { this.textoOcrBruto = textoOcrBruto; }

    public String getJsonEstruturado() { return jsonEstruturado; }
    public void setJsonEstruturado(String jsonEstruturado) { this.jsonEstruturado = jsonEstruturado; }

    public List<String> getMotivosRejeicao() { return motivosRejeicao; }
    public void setMotivosRejeicao(List<String> motivosRejeicao) { this.motivosRejeicao = motivosRejeicao; }

    public LocalDateTime getDataAnalise() { return dataAnalise; }
    public void setDataAnalise(LocalDateTime dataAnalise) { this.dataAnalise = dataAnalise; }
}
