package com.example.aistudio.service;

// =============================================================================
//  RagService — API explícita para inspecionar o RAG
// =============================================================================
//  Útil para entender o que está acontecendo "por baixo dos panos":
//  dado uma query, retorna os chunks mais similares do embedding store.
// =============================================================================

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel model;

    public RagService(EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        this.store = store;
        this.model = model;
    }

    /**
     * Retorna os top-K segmentos mais similares à query.
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults, double minScore) {
        Embedding queryEmbedding = model.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        return store.search(request).matches();
    }
}
