package com.avanade.cesar.validacao.service;

import com.avanade.cesar.validacao.dto.DadosDocumentoDTO;
import com.avanade.cesar.validacao.dto.ResultadoValidacaoResponse;
import com.avanade.cesar.validacao.model.StatusValidacao;
import com.avanade.cesar.validacao.model.TipoDocumento;
import com.avanade.cesar.validacao.model.ValidacaoDocumento;
import com.avanade.cesar.validacao.repository.ValidacaoDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentoPipelineService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoPipelineService.class);

    private final ImagePreprocessingService preprocessingService;
    private final OcrService ocrService;
    private final AiExtractionService aiExtractionService;
    private final ValidacaoRegraService regraService;
    private final ValidacaoDocumentoRepository repository;
    private final ObjectMapper objectMapper;

    public DocumentoPipelineService(
            ImagePreprocessingService preprocessingService,
            OcrService ocrService,
            AiExtractionService aiExtractionService,
            ValidacaoRegraService regraService,
            ValidacaoDocumentoRepository repository,
            ObjectMapper objectMapper
    ) {
        this.preprocessingService = preprocessingService;
        this.ocrService = ocrService;
        this.aiExtractionService = aiExtractionService;
        this.regraService = regraService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Pipeline completo:
     * Upload -> OpenCV -> Tesseract OCR -> IA (JSON) -> Regras Spring Boot -> Banco de Dados
     */
    public ResultadoValidacaoResponse processarEValidar(MultipartFile file, String cpfEsperado) {
        ValidacaoDocumento registro = new ValidacaoDocumento();
        registro.setNomeArquivo(file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento");
        registro.setTipoDocumento(TipoDocumento.COMPROVANTE_RESIDENCIA);
        registro.setDataAnalise(LocalDateTime.now());

        try {
            // Etapa 1 e 2: Carregar PDF/Imagem + OpenCV
            BufferedImage imagemTratada = preprocessingService.carregarETratar(file);

            // Etapa 3: Tesseract OCR
            String textoOcr = ocrService.extrairTexto(imagemTratada);
            registro.setTextoOcrBruto(textoOcr);

            // Etapa 4: IA estrutura o texto em JSON
            DadosDocumentoDTO dados = aiExtractionService.extrairDadosEstruturados(textoOcr);
            preencherDadosExtraidos(registro, dados);

            // Etapa 5: Regras de negócio em Java / Spring Boot
            List<String> motivos = regraService.validarComprovanteResidencia(
                    textoOcr, dados, cpfEsperado, LocalDate.now()
            );

            registro.setMotivosRejeicao(motivos);
            registro.setStatus(motivos.isEmpty() ? StatusValidacao.APROVADO : StatusValidacao.REJEITADO);

        } catch (Exception ex) {
            log.error("Erro ao processar documento {}", registro.getNomeArquivo(), ex);
            List<String> motivos = new ArrayList<>();
            motivos.add("Falha técnica no processamento do arquivo: " + ex.getMessage());
            registro.setMotivosRejeicao(motivos);
            registro.setStatus(StatusValidacao.REJEITADO);
        }

        // Etapa 6: Persistência no banco de dados
        ValidacaoDocumento salvo = repository.save(registro);
        return ResultadoValidacaoResponse.fromEntity(salvo);
    }

    /**
     * Endpoint auxiliar para demonstrar a etapa de IA + Regras enviando texto OCR diretamente (útil em testes/demo).
     */
    public ResultadoValidacaoResponse validarTextoSimulado(String nomeArquivo, String textoOcr, String cpfEsperado) {
        ValidacaoDocumento registro = new ValidacaoDocumento();
        registro.setNomeArquivo(nomeArquivo != null && !nomeArquivo.isBlank() ? nomeArquivo : "simulacao-texto.txt");
        registro.setTipoDocumento(TipoDocumento.COMPROVANTE_RESIDENCIA);
        registro.setDataAnalise(LocalDateTime.now());
        registro.setTextoOcrBruto(textoOcr);

        DadosDocumentoDTO dados = aiExtractionService.extrairDadosEstruturados(textoOcr);
        preencherDadosExtraidos(registro, dados);

        List<String> motivos = regraService.validarComprovanteResidencia(
                textoOcr, dados, cpfEsperado, LocalDate.now()
        );
        registro.setMotivosRejeicao(motivos);
        registro.setStatus(motivos.isEmpty() ? StatusValidacao.APROVADO : StatusValidacao.REJEITADO);

        return ResultadoValidacaoResponse.fromEntity(repository.save(registro));
    }

    public List<ResultadoValidacaoResponse> listarTodos() {
        return repository.findAllByOrderByDataAnaliseDesc().stream()
                .map(ResultadoValidacaoResponse::fromEntity)
                .toList();
    }

    public ResultadoValidacaoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(ResultadoValidacaoResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Validação não encontrada para o ID: " + id));
    }

    private void preencherDadosExtraidos(ValidacaoDocumento registro, DadosDocumentoDTO dados) {
        if (dados == null) return;
        registro.setNomeExtraido(dados.nome());
        registro.setCpfExtraido(dados.cpf());
        registro.setEnderecoExtraido(dados.endereco());
        registro.setCidadeExtraida(dados.cidade());
        registro.setEstadoExtraido(dados.estado());
        if (dados.dataEmissao() != null && !dados.dataEmissao().isBlank()) {
            try {
                registro.setDataEmissaoExtraida(LocalDate.parse(dados.dataEmissao().trim()));
            } catch (Exception ignored) {
                // Mantém nulo; a regra de validação registrará o formato inválido.
            }
        }
        try {
            registro.setJsonEstruturado(objectMapper.writeValueAsString(dados));
        } catch (Exception ignored) {
        }
    }
}
