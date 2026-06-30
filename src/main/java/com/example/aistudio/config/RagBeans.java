package com.example.aistudio.config;

// =============================================================================
//  Beans de RAG (sem dependências circulares)
// =============================================================================
//  Esta classe SÓ define os beans. A ingestão dos documentos acontece
//  em RagIngestor (depois que o contexto está pronto).
// =============================================================================

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagBeans {

    @Value("${rag.max-results:5}")
    private int maxResults;

    @Value("${rag.min-score:0.4}")
    private double minScore;

    /**
     * Modelo de embedding LOCAL (ONNX, ~24MB quantizado).
     * É o padrão carregado via SPI pelo langchain4j-easy-rag.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    /**
     * Banco de vetores em memória (reinicia a cada start).
     * Para produção, troque por pgvector, qdrant, chroma, milvus, etc.
     */
    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Retriever: top-K mais similares com score mínimo.
     * É injetado no @AiService via ContentRetriever.
     */
    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }
}
