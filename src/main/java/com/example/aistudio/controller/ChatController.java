package com.example.aistudio.controller;

// =============================================================================
//  ChatController — endpoints REST demonstrando cada capacidade
// =============================================================================
//  Endpoints:
//    GET  /api/health                      — health check
//    GET  /api/info                        — info da app (LLM, MCP, RAG)
//    GET  /api/chat?message=...&session=...  — chat com RAG + tools + memória
//    POST /api/chat                        — body { sessionId, message }
//    POST /api/rag/search                  — busca explícita no embedding store
//    GET  /api/mcp/tools                   — lista tools do servidor MCP
//    POST /api/mcp/execute                 — executa uma tool MCP (sem LLM)
// =============================================================================

import com.example.aistudio.service.Assistant;
import com.example.aistudio.service.McpService;
import com.example.aistudio.service.RagService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final Assistant assistant;
    private final RagService ragService;
    private final McpService mcpService;

    public ChatController(Assistant assistant, RagService ragService, McpService mcpService) {
        this.assistant = assistant;
        this.ragService = ragService;
        this.mcpService = mcpService;
    }

    // -------------------------------------------------------------------------
    //  Health
    // -------------------------------------------------------------------------

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "app", "langchain4j-rag-mcp",
                "endpoints", List.of(
                        "GET  /api/health",
                        "GET  /api/info",
                        "GET  /api/chat?message=...&session=...",
                        "POST /api/chat  body: { sessionId, message }",
                        "POST /api/rag/search  body: { query, maxResults, minScore }",
                        "GET  /api/mcp/tools",
                        "POST /api/mcp/execute  body: { toolName, arguments }"
                ),
                "mcp-enabled", !mcpService.listTools().isEmpty()
        );
    }

    // -------------------------------------------------------------------------
    //  Chat (RAG + Tools + Memory)
    // -------------------------------------------------------------------------

    /**
     * Chat simples: GET para fácil teste via curl/browser.
     */
    @GetMapping("/chat")
    public Map<String, String> chatGet(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String session
    ) {
        String answer = assistant.chat(session, message);
        return Map.of("session", session, "message", message, "answer", answer);
    }

    /**
     * Chat com body JSON (mais adequado para produção).
     */
    @PostMapping("/chat")
    public Map<String, String> chatPost(@RequestBody ChatRequest request) {
        String session = (request.sessionId() == null || request.sessionId().isBlank())
                ? "default" : request.sessionId();
        String answer = assistant.chat(session, request.message());
        return Map.of("session", session, "message", request.message(), "answer", answer);
    }

    public record ChatRequest(String sessionId, String message) {}

    // -------------------------------------------------------------------------
    //  RAG explícito (sem LLM)
    // -------------------------------------------------------------------------

    /**
     * Mostra o que o RAG recuperaria para uma query.
     * Útil para entender a qualidade da indexação.
     */
    @PostMapping("/rag/search")
    public Map<String, Object> ragSearch(@RequestBody RagQueryRequest request) {
        int max = request.maxResults() == null ? 3 : request.maxResults();
        double min = request.minScore() == null ? 0.0 : request.minScore();

        List<EmbeddingMatch<TextSegment>> matches = ragService.search(request.query(), max, min);

        List<Map<String, Object>> results = matches.stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("score", m.score());
            r.put("text", m.embedded().text());
            r.put("metadata", m.embedded().metadata().toMap());
            return r;
        }).toList();

        return Map.of(
                "query", request.query(),
                "count", results.size(),
                "results", results
        );
    }

    public record RagQueryRequest(String query, Integer maxResults, Double minScore) {}

    // -------------------------------------------------------------------------
    //  MCP explícito (sem LLM)
    // -------------------------------------------------------------------------

    /**
     * Lista as tools que o servidor MCP está expondo.
     */
    @GetMapping("/mcp/tools")
    public Map<String, Object> listMcpTools() {
        List<ToolSpecification> specs = mcpService.listTools();
        List<Map<String, Object>> tools = specs.stream().map(s -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", s.name());
            t.put("description", s.description());
            // Serializa o JSON-schema dos parâmetros como string JSON legível
            t.put("parametersJson", s.parameters() == null ? null : s.parameters().toString());
            return t;
        }).toList();
        return Map.of(
                "mcp-enabled", !tools.isEmpty(),
                "count", tools.size(),
                "tools", tools
        );
    }

    /**
     * Executa uma tool MCP programaticamente.
     * Exemplo: { "toolName": "add", "arguments": "{\"a\":2,\"b\":3}" }
     */
    @PostMapping("/mcp/execute")
    public Map<String, Object> executeMcpTool(@RequestBody McpExecuteRequest request) {
        var result = mcpService.executeTool(request.toolName(), request.arguments());
        return Map.of(
                "tool", request.toolName(),
                "arguments", request.arguments(),
                "isError", result.isError(),
                "resultText", result.resultText()
        );
    }

    public record McpExecuteRequest(String toolName, String arguments) {}
}
