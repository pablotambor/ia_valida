# Validação Automatizada de Documentos — MVP

Sistema MVP em **Java 17 + Spring Boot** para validação automatizada de comprovantes de residência (imagens PNG/JPG ou PDF) utilizando **OpenCV (JavaCV)**, **Tesseract OCR (Tess4J)**, **Inteligência Artificial (OpenAI / Gemini)** e **PostgreSQL**.

## Fluxo da Arquitetura

```text
Upload (Imagem/PDF)
  -> OpenCV / JavaCV (escala de cinza, remoção de ruído, binarização)
  -> Tesseract OCR (extração de texto bruto)
  -> IA / LLM (estruturação em JSON rígido)
  -> Java / Spring Boot (validação de campos obrigatórios, CPF e prazo < 90 dias)
  -> Banco de Dados (PostgreSQL / H2)
  -> API REST + Tela Web (APROVADO ou REJEITADO com motivos)
```

## Pré-requisitos

- **Java 17+** e **Maven 3.9+**
- **Tesseract OCR** instalado e arquivo `por.traineddata` dentro da pasta `./tessdata` (ou configurável via `TESSDATA_PREFIX`)
- (Opcional) **Docker** para subir o PostgreSQL via `docker-compose up -d`
- (Opcional) Chave de API da **OpenAI** ou **Gemini** (sem chave configurada, o projeto opera em modo `fallback` com extração local por regex para facilitar testes imediatos)

## Como executar

### 1. Subir em modo rápido (H2 em memória + Fallback local)
```bash
mvn spring-boot:run
```
Acesse a interface web de demonstração em: `http://localhost:8080`
Acesse a documentação Swagger em: `http://localhost:8080/swagger-ui.html`

### 2. Subir com PostgreSQL e IA (OpenAI ou Gemini)
```bash
docker-compose up -d
export AI_PROVIDER=gemini
export GEMINI_API_KEY=sua_chave_aqui
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Endpoints Principais

- `POST /api/documentos/validar` (`multipart/form-data`, campo `file` e opcional `cpfEsperado`) — executa o pipeline completo
- `POST /api/documentos/validar-texto` (`application/json`) — testa a camada de IA + Regras enviando texto OCR simulado
- `GET /api/documentos` — lista o histórico de validações
- `GET /api/documentos/{id}` — consulta o resultado detalhado de uma validação

## Regras de Validação do MVP

1. **Legibilidade:** o OCR deve extrair quantidade mínima de caracteres válidos.
2. **Campos obrigatórios:** nome completo, CPF, endereço, cidade, estado (UF) e data de emissão.
3. **CPF:** 11 dígitos e validação algorítmica dos dígitos verificadores (com conferência opcional do CPF informado).
4. **Prazo de emissão:** data válida, não futura e inferior a 90 dias em relação à data da análise.

## Testes Automatizados

```bash
mvn test
```
Os testes cobrem os três cenários principais de demonstração: documento aprovado, documento com campo ausente/CPF inválido e documento vencido (> 90 dias).
