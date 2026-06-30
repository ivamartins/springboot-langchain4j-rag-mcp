package com.example.aistudio.service;

// =============================================================================
//  McpService — wrapper para usar o MCP client explicitamente
// =============================================================================
//  Permite listar e executar tools do MCP SEM passar pelo LLM.
//  Bom para debugging e para entender o protocolo.
// =============================================================================

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class McpService {

    private final ObjectProvider<McpClient> mcpClientProvider;

    public McpService(ObjectProvider<McpClient> mcpClientProvider) {
        this.mcpClientProvider = mcpClientProvider;
    }

    private Optional<McpClient> client() {
        return Optional.ofNullable(mcpClientProvider.getIfAvailable());
    }

    /**
     * Lista as tools que o servidor MCP expõe.
     * Retorna lista vazia se o MCP estiver desabilitado.
     */
    public List<ToolSpecification> listTools() {
        return client().map(McpClient::listTools).orElse(List.of());
    }

    /**
     * Executa uma tool do MCP programaticamente (sem LLM).
     * Útil para testar o servidor.
     */
    public ToolExecutionResult executeTool(String toolName, String argumentsJson) {
        return client()
                .map(c -> c.executeTool(ToolExecutionRequest.builder()
                        .name(toolName)
                        .arguments(argumentsJson)
                        .build()))
                .orElseThrow(() -> new IllegalStateException("MCP is not enabled"));
    }
}
