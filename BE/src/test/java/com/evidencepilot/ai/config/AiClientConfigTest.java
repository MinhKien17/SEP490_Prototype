package com.evidencepilot.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiClientConfig.class);

    @Test
    @SuppressWarnings("unchecked")
    void aiModelBaseUrlsPreservesConfiguredFallbackOrder() {
        contextRunner
                .withPropertyValues(
                        "ai.model.local-base-url=http://127.0.0.1:8000",
                        "ai.model.ngrok-base-url=https://good-lumpish-headstone.ngrok-free.dev",
                        "ai.model.base-url=https://configured-ai.example.test"
                )
                .run(context -> assertThat(context.getBean("aiModelBaseUrls", List.class))
                        .containsExactly(
                                "http://127.0.0.1:8000",
                                "https://good-lumpish-headstone.ngrok-free.dev",
                                "https://configured-ai.example.test"
                        ));
    }
}
