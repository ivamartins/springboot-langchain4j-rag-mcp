# Spring Boot + LangChain4j + Easy RAG + MCP

Demo **funcional e testada** integrando 4 stacks:

- **Spring Boot 3.5** (web + actuator)
- **LangChain4j 1.17** (AI Services, RAG, Tools, MCP)
- **Easy RAG** (bge-small-en-v1.5 ONNX, in-memory store)
- **MCP** (cliente stdio para qualquer servidor Model Context Protocol)

## Validação end-to-end (rodada real)

```
✅ 1.  /api/health          → {"status":"ok"}
✅ 2.  /api/info            → MCP: True, 7 endpoints
✅ 3.  /api/rag/search      → top-2 chunks (scores 0.859, 0.842)
✅ 4.  Chat + RAG           → "O MCP é um padrão aberto que define a comunicação..."
✅ 5.  Chat + @Tool local   → "Hoje é 30 de junho de 2026."
✅ 6.  Chat + @Tool local   → "99 vezes 88 é 8712."
✅ 7.  Chat + MCP tool      → "Java rocks"  (via echo)
✅ 8.  /api/mcp/tools       → 5 tools (echo, add, longRunningOperation, sampleLLM, getTinyImage)
✅ 9.  /api/mcp/execute     → add(17,25) = "The sum of 17 and 25 is 42."
✅ 10. Memória por sessão   → "Você tem 30 anos." (lembrou de turno anterior)
```

## Estrutura

```
langchain4j-rag-mcp/
├── pom.xml
├── README.md
├── scripts/start.sh
└── src/
    ├── main/
    │   ├── java/com/example/aistudio/
    │   │   ├── AiStudioApplication.java       @SpringBootApplication
    │   │   ├── config/
    │   │   │   ├── RagBeans.java              embedding model + store + retriever
    │   │   │   ├── RagIngestor.java           ApplicationRunner: carrega docs no startup
    │   │   │   ├── McpConfiguration.java      Transport + client + tool provider (condicional)
    │   │   │   └── AssistantConfiguration.java Constrói o Assistant via AiServices
    │   │   ├── controller/
    │   │   │   └── ChatController.java        /api/chat, /api/rag/search, /api/mcp/*
    │   │   ├── service/
    │   │   │   ├── Assistant.java             @AiService interface (system message + memory)
    │   │   │   ├── RagService.java            API explícita do RAG
    │   │   │   └── McpService.java            API explícita do MCP
    │   │   └── tool/
    │   │       └── LocalTools.java            @Tool: currentDateTime, addIntegers, multiplyIntegers
    │   └── resources/
    │       ├── application.yml
    │       └── documents/                     Pasta com docs para o RAG
    │           ├── langchain4j-overview.md
    │           └── spring-boot.md
    └── test/java/.../AiStudioApplicationTests.java
```

## Pré-requisitos

- Java 17+ (testado com OpenJDK 25)
- Maven 3.9+
- (Opcional) `npm i -g @modelcontextprotocol/server-everything` para usar um servidor MCP real.
  Sem ele, defina `MCP_ENABLED=false`.

## Configuração (variáveis de ambiente)

| Variável         | Default                                                              | Descrição                       |
|------------------|----------------------------------------------------------------------|---------------------------------|
| `LLM_BASE_URL`   | `http://langchain4j.dev/demo/openai/v1`                              | Endpoint OpenAI-compat          |
| `LLM_API_KEY`    | `demo`                                                               | API key (dummy p/ Ollama)       |
| `LLM_MODEL`      | `gpt-4o-mini`                                                        | Nome do modelo                  |
| `MCP_ENABLED`    | `true`                                                               | Liga/desliga o cliente MCP      |
| `MCP_COMMAND`    | `/home/iva/.npm-global/bin/mcp-server-everything`                    | Executável do server MCP        |
| `MCP_ARGS`       | (vazio)                                                              | Args extras (CSV)               |
| `RAG_DOCS_PATH`  | `classpath:documents`                                                | Pasta com docs para indexar      |

Exemplos para outros LLMs:

```bash
# OpenAI
LLM_BASE_URL=https://api.openai.com/v1 LLM_API_KEY=sk-... LLM_MODEL=gpt-4o-mini

# Ollama
LLM_BASE_URL=http://localhost:11434/v1 LLM_API_KEY=ollama LLM_MODEL=llama3.1

# LM Studio
LLM_BASE_URL=http://localhost:1234/v1 LLM_API_KEY=lm-studio LLM_MODEL=local-model
```

## Como rodar

```bash
mvn spring-boot:run
# ou
mvn package && java -jar target/langchain4j-rag-mcp-0.0.1-SNAPSHOT.jar
```

A app sobe em `http://localhost:8080` (cerca de 12s + 2s de ingest).

## Endpoints

| Método | URL                          | O que faz                                                          |
|--------|------------------------------|--------------------------------------------------------------------|
| GET    | `/api/health`                | health check                                                       |
| GET    | `/api/info`                  | lista endpoints e status do MCP                                    |
| GET    | `/api/chat?message=...`      | chat (RAG + tools + memória). `&session=foo` para nova sessão     |
| POST   | `/api/chat`                  | body: `{ "sessionId": "...", "message": "..." }`                   |
| POST   | `/api/rag/search`            | body: `{ "query": "...", "maxResults": 3, "minScore": 0.0 }`       |
| GET    | `/api/mcp/tools`             | lista tools do servidor MCP                                        |
| POST   | `/api/mcp/execute`           | body: `{ "toolName": "...", "arguments": "{...}" }`                |

## Exemplos de uso (curl)

```bash
# 1) Health
curl http://localhost:8080/api/health

# 2) RAG puro: ver o que seria recuperado (sem LLM)
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{"query": "O que é Easy RAG?", "maxResults": 3, "minScore": 0.0}'

# 3) Chat usando RAG (o LLM recebe os chunks relevantes no prompt)
curl "http://localhost:8080/api/chat?message=O%20que%20%C3%A9%20Easy%20RAG%3F&session=demo"

# 4) Chat usando @Tool local (datetime)
curl "http://localhost:8080/api/chat?message=Que%20dia%20%C3%A9%20hoje%3F"

# 5) Chat usando @Tool local (multiplicação)
curl "http://localhost:8080/api/chat?message=Quanto%20%C3%A9%2099%20vezes%2088%3F"

# 6) Chat usando MCP tool (echo)
curl "http://localhost:8080/api/chat?message=Use%20a%20ferramenta%20echo%20para%20repetir%20'Java%20rocks'"

# 7) MCP: listar tools
curl http://localhost:8080/api/mcp/tools

# 8) MCP: executar tool diretamente (sem LLM)
curl -X POST http://localhost:8080/api/mcp/execute \
  -H "Content-Type: application/json" \
  -d '{"toolName": "add", "arguments": "{\"a\": 17, \"b\": 25}"}'

# 9) Memória (mesma sessionId em chamadas diferentes)
curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" \
  -d '{"sessionId":"bob","message":"Meu nome é Bob e tenho 30 anos"}'
curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" \
  -d '{"sessionId":"bob","message":"Quantos anos eu tenho?"}'
```

## Como funciona cada peça

### 1. LLM

O `langchain4j-open-ai-spring-boot-starter` lê `langchain4j.open-ai.chat-model.*`
no `application.yml` e cria um bean `ChatModel` automaticamente. Trocar de
provedor = trocar a URL (OpenAI, Ollama, LM Studio, vLLM etc.).

### 2. Easy RAG (RagBeans + RagIngestor)

- `RagBeans` define:
  - `BgeSmallEnV15QuantizedEmbeddingModel` — embedding ONNX local (~24MB).
  - `InMemoryEmbeddingStore<TextSegment>` — banco de vetores.
  - `EmbeddingStoreContentRetriever` — top-K com score mínimo.
- `RagIngestor` implementa `ApplicationRunner`: no startup,
  `FileSystemDocumentLoader` carrega a pasta, Apache Tika faz parsing,
  `EmbeddingStoreIngestor.ingest()` divide em chunks e gera embeddings.

### 3. Tools locais (LocalTools)

`@Component` com métodos `@Tool`. **Importante**: com `AiServices.builder()`
manual, as tools precisam ser passadas explicitamente via `.tools(localTools)`
(elas NÃO são auto-descobertas como na anotação `@AiService`).

### 4. MCP (McpConfiguration)

- `StdioMcpTransport` inicia o binário configurado como subprocesso.
- `DefaultMcpClient` faz handshake e expõe `listTools()` / `executeTool()`.
- `McpToolProvider` adapta o client para a interface `ToolProvider`.
- Em `AssistantConfiguration`, o `ToolProvider` é plugado se existir
  (caso `MCP_ENABLED=false`, é simplesmente ignorado).

### 5. Memória

`MessageWindowChatMemory.withMaxMessages(20)` por `sessionId` — cada
`@MemoryId` mantém um buffer isolado com as últimas 20 mensagens.

## Desabilitando o MCP

```bash
MCP_ENABLED=false mvn spring-boot:run
```

A app sobe normalmente; só as tools do MCP deixam de estar disponíveis.
Tools locais e RAG continuam funcionando.

## Adicionando seus próprios documentos

Coloque arquivos em `src/main/resources/documents/`
(`.txt`, `.md`, `.pdf`, `.docx`, `.html`, ...) OU aponte `RAG_DOCS_PATH`
para qualquer pasta do filesystem.

## Detalhes / pegadinhas conhecidas

- **Conflito de nomes de tool**: se o seu `@Tool` local tiver o mesmo nome
  de uma tool do MCP, dá `Duplicated definition for tool`. Solução: renomear
  a tool local (ex: `add` → `addIntegers`) ou filtrar no MCP:
  `McpToolProvider.builder().filterToolNames("echo", ...)`.
- **Ingest é assíncrono ao startup**: a app fica pronta antes do ingest
  terminar (cerca de 2s). Os primeiros requests de RAG podem vir vazios
  se chegarem muito cedo. Em produção, pré-compute o ingest offline.
- **LLM precisa suportar tool calling**: o `gpt-4o-mini` da OpenAI suporta.
  Modelos Ollama menores (ex: `llama3.2:1b`) podem não suportar.
