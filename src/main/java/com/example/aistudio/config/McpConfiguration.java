package com.example.aistudio.config;

// =============================================================================
//  MCP (Model Context Protocol) — configuração do cliente
// =============================================================================
//  Arquitetura:
//    StdioMcpTransport  →  DefaultMcpClient  →  McpToolProvider  →  @AiService
//
//  • Transport: como nos comunicamos com o servidor.
//      stdio = subprocesso (lê stdin, escreve stdout) ← usado aqui
//      streamable-http = HTTP POST + SSE
//      websocket = WebSocket
//  • Client: gerencia handshake, lista tools/prompts/resources, executa tools.
//  • ToolProvider: interface do LangChain4j que entrega as tools ao LLM.
// =============================================================================

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
// Permite desligar o MCP inteiro com: MCP_ENABLED=false
@ConditionalOnProperty(prefix = "langchain4j.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpConfiguration.class);

    @Value("${langchain4j.mcp.stdio.command}")
    private String command;

    @Value("${langchain4j.mcp.stdio.args:}")
    private List<String> args;

    @Value("${langchain4j.mcp.stdio.log-events:false}")
    private boolean logEvents;

    // Guardamos referência para fechar no shutdown
    private McpClient mcpClient;

    // -------------------------------------------------------------------------
    //  Transport
    // -------------------------------------------------------------------------

    /**
     * StdioMcpTransport inicia o comando como subprocesso e conversa via stdin/stdout.
     * Exemplo de uso no application.yml:
     *   MCP_COMMAND=/home/iva/.npm-global/bin/mcp-server-everything
     */
    @Bean
    public McpTransport mcpTransport() {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);
        if (args != null && !args.isEmpty()) {
            fullCommand.addAll(args);
        }
        log.info("Starting MCP stdio subprocess: {}", String.join(" ", fullCommand));
        return StdioMcpTransport.builder()
                .command(fullCommand)
                .logEvents(logEvents)
                .build();
    }

    // -------------------------------------------------------------------------
    //  Client
    // -------------------------------------------------------------------------

    /**
     * Cliente MCP de alto nível. ListTools(), executeTool(), listResources(), etc.
     * A "key" é útil quando temos vários clientes (desambiguação de tools).
     */
    @Bean
    public McpClient mcpClient(McpTransport transport) {
        this.mcpClient = DefaultMcpClient.builder()
                .key("stdio-mcp-client")
                .transport(transport)
                .build();
        return this.mcpClient;
    }

    // -------------------------------------------------------------------------
    //  ToolProvider — pluga as tools do MCP no @AiService
    // -------------------------------------------------------------------------

    /**
     * Expõe as tools do servidor MCP para o LangChain4j.
     * Pode-se filtrar por nome com .filterToolNames(...) para evitar tool explosion.
     */
    @Bean
    public ToolProvider mcpToolProvider(McpClient client) {
        log.info("Building McpToolProvider from client '{}'", "stdio-mcp-client");
        return McpToolProvider.builder()
                .mcpClients(client)
                .build();
    }

    // -------------------------------------------------------------------------
    //  Shutdown
    // -------------------------------------------------------------------------

    @PreDestroy
    public void close() {
        if (mcpClient != null) {
            try {
                log.info("Closing MCP client (and subprocess)");
                mcpClient.close();
            } catch (Exception e) {
                log.warn("Error closing MCP client: {}", e.getMessage());
            }
        }
    }
}
