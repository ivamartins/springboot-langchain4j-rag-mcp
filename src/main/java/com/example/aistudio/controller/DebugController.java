package com.example.aistudio.controller;

// =============================================================================
//  DebugController — endpoints pra visualizar a FUSÃO RAG + LLM + Tools
// =============================================================================
//  /api/debug/chat-verbose mostra:
//    passo 1: o que o RAG recuperou (snippets injetados no prompt)
//    passo 2: a resposta final do Assistant (que pode ter chamado tools)
// =============================================================================

import com.example.aistudio.service.Assistant;
import com.example.aistudio.service.RagService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import dev.langchain4j.mcp.McpToolProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final RagService ragService;
    private final Assistant assistant;
    private final ObjectProvider<McpToolProvider> mcpToolProvider;

    public DebugController(
            RagService ragService,
            Assistant assistant,
            ObjectProvider<McpToolProvider> mcpToolProvider
    ) {
        this.ragService = ragService;
        this.assistant = assistant;
        this.mcpToolProvider = mcpToolProvider;
    }

    /**
     * Chat verbose: mostra, em ordem, o que o RAG recuperou e qual foi a
     * resposta final do Assistant. Útil pra entender a fusão.
     */
    @PostMapping("/chat-verbose")
    public Map<String, Object> chatVerbose(@RequestBody ChatRequestDto req) {
        String session = (req.sessionId() == null || req.sessionId().isBlank()) ? "default" : req.sessionId();
        String message = req.message();

        // ─── PASSO 1: RAG (recupera contexto, SEM chamar o LLM ainda) ───
        List<EmbeddingMatch<TextSegment>> matches = ragService.search(message, 3, 0.0);
        List<Map<String, Object>> retrieved = matches.stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("score", m.score());
            r.put("source", m.embedded().metadata().getString("file_name"));
            r.put("text", m.embedded().text());
            return r;
        }).toList();

        // ─── PASSO 2: LLM (com RAG injetado, tools disponíveis, memória) ───
        // Aqui o AiServices internamente:
        //   a) carrega histórico da memória (session)
        //   b) injeta os snippets do RAG no prompt
        //   c) manda pro ChatModel com lista de tools
        //   d) se LLM pedir tool: executa (local ou MCP) e repete
        //   e) retorna a resposta final
        String answer = assistant.chat(session, message);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session", session);
        out.put("message", message);
        out.put("step1_rag", Map.of(
                "explanation", "O RAG buscou esses trechos no embedding store. " +
                               "Eles são injetados no prompt do LLM antes da chamada.",
                "retrieved_count", retrieved.size(),
                "retrieved", retrieved
        ));
        out.put("step2_llm_call", Map.of(
                "explanation", "O Assistant envia ao LLM: system message + memória + " +
                               "snippets do RAG + tools disponíveis. Se o LLM pedir uma tool, " +
                               "ela é executada e o ciclo repete até a resposta final.",
                "tools_visible_to_llm", Map.of(
                        "local", List.of("currentDateTime", "addIntegers", "multiplyIntegers"),
                        "mcp", mcpToolProvider.getIfAvailable() != null ? "wired" : "disabled"
                ),
                "answer", answer
        ));
        return out;
    }

    public record ChatRequestDto(String sessionId, String message) {}
}
