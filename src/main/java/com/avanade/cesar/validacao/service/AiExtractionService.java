package com.avanade.cesar.validacao.service;

import com.avanade.cesar.validacao.dto.DadosDocumentoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(AiExtractionService.class);

    private static final String SYSTEM_PROMPT = """
            Você é um especialista em extração de dados de documentos brasileiros (comprovante de residência).
            Analise o texto OCR fornecido e retorne APENAS um objeto JSON válido, sem markdown ou explicações extras, com os campos exatos:
            {
              "nome": "string ou null",
              "cpf": "apenas 11 dígitos ou null",
              "endereco": "logradouro e número ou null",
              "cidade": "string ou null",
              "estado": "UF com 2 letras ou null",
              "dataEmissao": "YYYY-MM-DD ou null"
            }
            Regras obrigatórias:
            - Se uma informação não for encontrada claramente no texto, retorne null.
            - Nunca invente ou suponha dados ausentes.
            """;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ai.provider:fallback}")
    private String provider;

    @Value("${app.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.ai.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    public AiExtractionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public DadosDocumentoDTO extrairDadosEstruturados(String textoOcr) {
        if (textoOcr == null || textoOcr.isBlank()) {
            return new DadosDocumentoDTO(null, null, null, null, null, null);
        }

        try {
            if ("openai".equalsIgnoreCase(provider) && !openaiApiKey.isBlank()) {
                return chamarOpenAI(textoOcr);
            }
            if ("gemini".equalsIgnoreCase(provider) && !geminiApiKey.isBlank()) {
                return chamarGemini(textoOcr);
            }
        } catch (Exception ex) {
            log.error("Falha na chamada ao provedor de IA ({}). Aplicando extração fallback local.", provider, ex);
        }

        log.info("Executando extração estruturada em modo fallback local (regex).");
        return extrairComFallbackRegex(textoOcr);
    }

    private DadosDocumentoDTO chamarOpenAI(String textoOcr) throws Exception {
        Map<String, Object> body = Map.of(
                "model", openaiModel,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "Texto OCR:\n" + textoOcr)
                )
        );

        String response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + openaiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        return objectMapper.readValue(limparJson(content), DadosDocumentoDTO.class);
    }

    private DadosDocumentoDTO chamarGemini(String textoOcr) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", SYSTEM_PROMPT + "\n\nTexto OCR:\n" + textoOcr)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"
                )
        );

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("candidates").get(0)
                .path("content").path("parts").get(0).path("text").asText();
        return objectMapper.readValue(limparJson(content), DadosDocumentoDTO.class);
    }

    /**
     * Fallback determinístico útil para testes locais sem chave de API configurada.
     */
    public DadosDocumentoDTO extrairComFallbackRegex(String texto) {
        String nome = capturarGrupo(texto, "(?i)(?:nome|titular|cliente)\\s*[:\\-]\\s*([A-ZÀ-Úa-zà-ú ]{3,80})");
        String cpf = capturarGrupo(texto, "(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})");
        if (cpf != null) {
            cpf = cpf.replaceAll("\\D", "");
        }
        String endereco = capturarGrupo(texto, "(?i)(?:endere[cç]o|logradouro)\\s*[:\\-]\\s*([^\\n\\r]+)");
        String cidade = capturarGrupo(texto, "(?i)(?:cidade|munic[ií]pio)\\s*[:\\-]\\s*([A-ZÀ-Úa-zà-ú ]{2,50})");
        String estado = capturarGrupo(texto, "(?i)(?:estado|uf)\\s*[:\\-]\\s*([A-Z]{2})");

        String dataRaw = capturarGrupo(texto, "(?i)(?:emiss[aã]o|data\\s*de\\s*emiss[aã]o|data)\\s*[:\\-]\\s*(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})");
        String dataIso = normalizarDataParaIso(dataRaw);

        return new DadosDocumentoDTO(
                limparCampo(nome),
                limparCampo(cpf),
                limparCampo(endereco),
                limparCampo(cidade),
                limparCampo(estado),
                dataIso
        );
    }

    private String capturarGrupo(String texto, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String normalizarDataParaIso(String dataRaw) {
        if (dataRaw == null || dataRaw.isBlank()) return null;
        try {
            if (dataRaw.contains("/")) {
                LocalDate parsed = LocalDate.parse(dataRaw.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return parsed.toString();
            }
            return LocalDate.parse(dataRaw.trim()).toString();
        } catch (Exception e) {
            return dataRaw.trim();
        }
    }

    private String limparCampo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private String limparJson(String raw) {
        return raw.replaceAll("(?s)```json|```", "").trim();
    }
}
