package com.example.aistudio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "langchain4j.mcp.enabled=false",
        "MCP_ENABLED=false"
})
class AiStudioApplicationTests {

    @Test
    void contextLoads() {
    }
}
