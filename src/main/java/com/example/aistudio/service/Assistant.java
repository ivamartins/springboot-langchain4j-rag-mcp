package com.example.aistudio.service;

// =============================================================================
//  Assistant — interface "mágica" do LangChain4j
// =============================================================================
//  O LangChain4j gera uma implementação dinâmica desta interface em runtime.
//  Cada método vira uma chamada ao LLM com prompt, RAG, tools e memória.
//
//  Anotações importantes:
//    @MemoryId   → identifica a sessão de chat (memória separada por ID)
//    @UserMessage → marca o parâmetro como mensagem do usuário
//    @SystemMessage → instrução de sistema (pode ser literal ou via resource)
//
//  O método chat() automaticamente:
//    1. Carrega histórico da memória (MessageWindowChatMemory)
//    2. Busca contexto no ContentRetriever (RAG)
//    3. Concatena tudo no prompt
//    4. Chama o LLM
//    5. Se o LLM pedir uma tool, executa e repete
//    6. Retorna a resposta final
// =============================================================================

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

    @SystemMessage("""
            Você é um assistente técnico prestativo. Você tem acesso a:

            1) **Base de conhecimento (RAG)** — documentos indexados localmente.
               Use-os quando o usuário perguntar sobre LangChain4j, RAG, MCP
               ou qualquer coisa que pareça estar nos documentos.

            2) **Ferramentas locais** — operações determinísticas (data/hora, cálculos).

            3) **Ferramentas MCP** — tools expostas pelo servidor MCP configurado.

            Regras:
            - Responda no mesmo idioma da pergunta.
            - Seja conciso e direto.
            - Se não souber, diga que não sabe — não invente.
            - Quando usar uma tool, explique brevemente o que fez.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
