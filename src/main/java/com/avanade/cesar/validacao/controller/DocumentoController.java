package com.avanade.cesar.validacao.controller;

import com.avanade.cesar.validacao.dto.ResultadoValidacaoResponse;
import com.avanade.cesar.validacao.service.DocumentoPipelineService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    private final DocumentoPipelineService pipelineService;

    public DocumentoController(DocumentoPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoValidacaoResponse> validarDocumento(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "cpfEsperado", required = false) String cpfEsperado
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(pipelineService.processarEValidar(file, cpfEsperado));
    }

    @PostMapping("/validar-texto")
    public ResponseEntity<ResultadoValidacaoResponse> validarTextoSimulado(@RequestBody Map<String, String> payload) {
        String nomeArquivo = payload.getOrDefault("nomeArquivo", "comprovante-demo.txt");
        String textoOcr = payload.getOrDefault("textoOcr", "");
        String cpfEsperado = payload.get("cpfEsperado");
        return ResponseEntity.ok(pipelineService.validarTextoSimulado(nomeArquivo, textoOcr, cpfEsperado));
    }

    @GetMapping
    public ResponseEntity<List<ResultadoValidacaoResponse>> listarValidacoes() {
        return ResponseEntity.ok(pipelineService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultadoValidacaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pipelineService.buscarPorId(id));
    }
}
