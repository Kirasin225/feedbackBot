package com.company.feedbackbot.service;

import com.company.feedbackbot.domain.Sentiment;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
public class OpenAIService {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${openai.apiUrl}")
  private String apiUrl;
  @Value("${openai.apiKey}")
  private String apiKey;

  private OpenAIClient openAIClient;
  @PostConstruct
    private void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key не задан. Укажите openai.apiKey в application.properties");
        }
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(apiUrl)
                .apiKey(apiKey)
                .organization("org-FINQB38ggGCCKA8RhzbpYQP1")
                .project("proj_gXfirfOFKOCzzkje3VPHWxeI")
                .build();
    }

  private WebClient client() {
    return WebClient.builder()
        .baseUrl(apiUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Data
    public static class Analysis {
        @JsonPropertyDescription("The sentiment can be NEGATIVE, NEUTRAL, POSITIVE")
        private Sentiment sentiment;
        @JsonPropertyDescription("The criticality can be 1 - 5")
        private Integer criticality;
        @JsonPropertyDescription("как можно решить данне питання")
        private String resolution;
    }

    public Analysis analyze(String text) {
        StructuredChatCompletionCreateParams<Analysis> params = ChatCompletionCreateParams.builder()
                .addUserMessage("Ти HR-асистент. Поверни JSON строго за схемою.")
                .addUserMessage(text)
                .model(ChatModel.GPT_4O_MINI)
                .responseFormat(Analysis.class)
                .build();

        int maxAttempts = 3;
        long delayMs = 2000;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ChatCompletion chatCompletion = openAIClient.chat().completions().create(params).rawChatCompletion();
                String content = String.valueOf(chatCompletion.choices().getFirst().message().content());
                return objectMapper.readValue(content, Analysis.class);
            } catch (com.openai.errors.RateLimitException ex) {
                if (attempt == maxAttempts) break;
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                delayMs *= 2;
            } catch (Exception ex) {
                break;
            }
        }

        Analysis fallback = new Analysis();
        fallback.setSentiment(Sentiment.NEUTRAL);
        fallback.setCriticality(4);
        fallback.setResolution("Порадьтеся з керівником, визначте відповідального та терміни.");
        return fallback;
  }
}
