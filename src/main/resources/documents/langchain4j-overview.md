# LangChain4j

LangChain4j é uma biblioteca Java para integrar aplicações com LLMs (Large Language Models).
Foi criada como o "LangChain para Java" e oferece uma API unificada para diversos provedores:
OpenAI, Azure OpenAI, Google Vertex AI, Anthropic, Mistral, Ollama (modelos locais) etc.

## Funcionalidades principais

- **Chat models** — interface única `ChatModel` para enviar mensagens e receber respostas.
- **Streaming** — `StreamingChatModel` para respostas em tempo real (token a token).
- **AI Services** — anotação `@AiService` que gera uma implementação dinâmica
  de uma interface Java. É o "açúcar sintático" do LangChain4j.
- **Tools / Function calling** — métodos `@Tool` em `@Component` que o LLM pode invocar.
- **RAG (Retrieval-Augmented Generation)** — 3 sabores: Easy, Naive e Advanced.
- **Embedding stores** — pgvector, Qdrant, Chroma, Milvus, Weaviate, Pinecone, in-memory, etc.
- **MCP (Model Context Protocol)** — cliente para servidores MCP stdio/HTTP/WebSocket.

## Easy RAG

O Easy RAG é o caminho mais curto para colocar RAG funcionando:

1. Adicionar a dependência `langchain4j-easy-rag`.
2. Carregar documentos: `FileSystemDocumentLoader.loadDocuments("/docs")`.
3. Ingerir: `EmbeddingStoreIngestor.ingest(documents, embeddingStore)`.
4. Usar: `EmbeddingStoreContentRetriever.from(embeddingStore)` num AI Service.

Por baixo dos panos:
- **Apache Tika** faz o parsing (PDF, DOCX, TXT, MD, HTML, ...).
- **bge-small-en-v1.5** (ONNX, ~24MB) gera os embeddings localmente.
- **InMemoryEmbeddingStore** guarda os vetores (trocar por um real em produção).

## MCP

O Model Context Protocol é um padrão aberto que define como LLMs se comunicam
com ferramentas externas. Um servidor MCP expõe:
- **Tools** — funções que o LLM pode chamar.
- **Prompts** — templates de prompt com argumentos.
- **Resources** — dados (arquivos, configs) que o LLM pode ler.

O LangChain4j suporta 3 transports:
- **stdio** — servidor roda como subprocesso, comunicação via stdin/stdout.
- **Streamable HTTP** — POST + SSE (recomendado em produção).
- **WebSocket** — bidirecional full-duplex.

## Onde usar

- Chatbots com memória por usuário.
- Agentes que chamam APIs externas.
- Análise de documentos privados (RAG).
- Assistentes de código.
- Workflows agentic (chains de LLMs + tools).
