package com.example.aistudio.config;

// =============================================================================
//  RagIngestor — carrega e indexa os documentos no startup
// =============================================================================
//  Implementa ApplicationRunner: o método run() é chamado pelo Spring
//  ASSIM que o contexto está pronto, ANTES de aceitar requisições HTTP
//  (na verdade é chamado em paralelo, mas é determinístico o suficiente).
//
//  Fluxo do Easy RAG:
//    1. Carregar documentos de uma pasta (FileSystemDocumentLoader)
//    2. EmbeddingStoreIngestor:
//         a) divide em chunks (DocumentSplitter do SPI easy-rag)
//         b) gera embeddings (EmbeddingModel do SPI easy-rag)
//         c) grava no EmbeddingStore (InMemoryEmbeddingStore)
//    3. Na query, ContentRetriever busca os N chunks mais similares
//       e injeta no prompt antes de chamar o LLM.
// =============================================================================

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Order(0)  // roda antes dos outros runners (se houver)
public class RagIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagIngestor.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${rag.documents-path}")
    private String documentsPath;

    public RagIngestor(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=================================================");
        log.info("Easy RAG: ingesting documents from '{}'", documentsPath);
        log.info("=================================================");

        try {
            List<Document> documents = loadDocuments(documentsPath);
            if (documents.isEmpty()) {
                log.warn("No documents found at '{}' — RAG will have nothing to retrieve", documentsPath);
                return;
            }

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            var result = ingestor.ingest(documents);
            log.info("Ingested {} document(s), {} token(s) used",
                    documents.size(),
                    result.tokenUsage().totalTokenCount());

        } catch (Exception e) {
            log.error("Failed to ingest documents: {}", e.getMessage(), e);
        }
    }

    private List<Document> loadDocuments(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }

        // 1) "classpath:documents" → carrega de src/main/resources/documents
        if (path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length());
            log.info("Loading documents from classpath: {}", cp);
            return ClassPathDocumentLoader.loadDocuments(cp);
        }

        // 2) Caminho do filesystem
        Path p = Paths.get(path);
        File f = p.toFile();
        if (!f.exists() || !f.isDirectory()) {
            log.warn("Documents path does not exist or is not a directory: {}", path);
            return List.of();
        }
        log.info("Loading documents from filesystem: {}", path);
        return FileSystemDocumentLoader.loadDocuments(path);
    }
}
