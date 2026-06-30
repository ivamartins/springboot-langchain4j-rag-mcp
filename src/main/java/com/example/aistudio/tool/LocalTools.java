package com.example.aistudio.tool;

// =============================================================================
//  LocalTools — ferramentas LOCAIS expostas ao LLM
// =============================================================================
//  O LangChain4j descobre @Tool methods em qualquer @Component / @Service
//  e os injeta automaticamente no @AiService (mesmo sem ToolProvider).
//
//  Cada @Tool vira um JSON-schema na chamada ao LLM, que decide se
//  quer ou não invocá-la com base na descrição.
// =============================================================================

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@Component
public class LocalTools {

    /**
     * Retorna a data/hora atual em São Paulo.
     * A descrição é o que o LLM lê para decidir quando usar.
     */
    @Tool("Retorna a data e hora atual no fuso horário de São Paulo (UTC-3) em formato ISO-8601")
    public String currentDateTime() {
        return LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Soma dois inteiros. LLM pode usar para aritmética simples.
     * (nome 'addIntegers' para não colidir com a tool 'add' do MCP)
     */
    @Tool("Soma dois números inteiros e retorna o resultado")
    public long addIntegers(long a, long b) {
        return a + b;
    }

    /**
     * Multiplica dois inteiros.
     */
    @Tool("Multiplica dois números inteiros e retorna o resultado")
    public long multiplyIntegers(long a, long b) {
        return a * b;
    }
}
