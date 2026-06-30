package com.example.aistudio.config;

// =============================================================================
//  Cria o bean Assistant (a "fachada" do LLM) via AiServices builder
// =============================================================================
//  Porque não usamos a anotação @AiService?
//  - Precisamos montar o ToolProvider *condicionalmente* (só se MCP estiver up).
//  - Com o builder explícito, evitamos conflito de auto-wiring
//    quando há múltiplos beans do mesmo tipo.
//
//  IMPORTANTE: AiServices.builder() NÃO auto-descobre @Tool methods
//  em @Component (isso só acontece com @AiService). Precisamos passar
//  o bean que contém os @Tool explicitamente via .tools(bean).
// =============================================================================

import com.example.aistudio.service.Assistant;
import com.example.aistudio.tool.LocalTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AssistantConfiguration.class);

    /**
     * O Assistant é a interface Java que define o que o LLM pode fazer.
     * O LangChain4j gera uma implementação dinâmica em runtime que:
     *   1. Pega a mensagem do método chat(...)
     *   2. Consulta o ContentRetriever (RAG) e injeta contexto relevante
     *   3. Envia para o ChatModel
     *   4. Se o LLM pedir para chamar uma Tool, executa e responde de novo
     *   5. Retorna a resposta final como String
     */
    @Bean
    public Assistant assistant(
            ChatModel chatModel,
            ContentRetriever contentRetriever,
            LocalTools localTools,                              // @Tool methods
            ObjectProvider<ToolProvider> toolProviderProvider  // MCP (opcional)
    ) {
        AiServices<Assistant> builder = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                // Tools LOCAIS (sempre disponíveis)
                .tools(localTools)
                // Memória por sessão: cada sessionId tem até 20 mensagens
                .chatMemoryProvider(sessionId ->
                        MessageWindowChatMemory.withMaxMessages(20));

        ToolProvider toolProvider = toolProviderProvider.getIfAvailable();
        if (toolProvider != null) {
            log.info("Assistant: wiring ToolProvider (MCP tools will be available)");
            builder.toolProvider(toolProvider);
        } else {
            log.info("Assistant: no ToolProvider — only local @Tool methods will be available");
        }

        return builder.build();
    }
}
