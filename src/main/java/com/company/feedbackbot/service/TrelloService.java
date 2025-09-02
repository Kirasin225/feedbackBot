package com.company.feedbackbot.service;

import com.company.feedbackbot.domain.Feedback;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class TrelloService {

  private static final Logger log = LoggerFactory.getLogger(TrelloService.class);

  @Value("${trello.enabled:false}") private boolean enabled;
  @Value("${trello.key:}") private String key;
  @Value("${trello.token:}") private String token;
  @Value("${trello.listId:}") private String listId;

  private WebClient client;

  @PostConstruct
  private void init() {
    client = WebClient.builder().baseUrl("https://api.trello.com/1").build();

    if (!enabled) {
      log.info("Trello integration disabled (trello.enabled=false).");
      return;
    }
    if (isBlank(key) || isBlank(token) || isBlank(listId)) {
      log.error("Trello включён, но не заданы trello.key/trello.token/trello.listId — отключаю интеграцию.");
      enabled = false;
      return;
    }

    try {
      client.get()
              .uri(uri -> uri.path("/members/me")
                      .queryParam("key", key)
                      .queryParam("token", token)
                      .build())
              .retrieve()
              .toBodilessEntity()
              .block();
      log.info("Trello credentials validated");
    } catch (WebClientResponseException e) {
      log.error("Trello credentials validation failed: status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
      enabled = false;
    } catch (Exception e) {
      log.error("Ошибка при валидации Trello credentials", e);
      enabled = false;
    }
  }

  public void createCardForFeedback(Feedback fb) {
    if (!enabled) return;

    String role = "-";
    String branch = "-";
    if (fb.getStaffProfile() != null) {
      role = String.valueOf(fb.getStaffProfile().getRole());
      branch = fb.getStaffProfile().getBranch();
    }

    String name = "[CRIT=" + fb.getCriticality() + "] " + role + "@" + branch;
    if (name.length() > 100) name = name.substring(0, 100);

    String desc = (fb.getText() == null ? "" : fb.getText())
            + "\n\nResolution:\n" + (fb.getResolution() == null ? "-" : fb.getResolution());
    if (desc.length() > 16000) desc = desc.substring(0, 16000);

    try {
      client.post()
              .uri("/cards")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters.fromFormData("idList", listId)
                      .with("name", name)
                      .with("desc", desc)
                      .with("key", key)
                      .with("token", token))
              .retrieve()
              .toBodilessEntity()
              .block();
      log.info("Создана карточка в Trello для feedback id={}", fb.getId());
    } catch (WebClientResponseException e) {
      log.error("Trello API error when creating card: status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
    } catch (Exception e) {
      log.error("Ошибка при создании карточки в Trello", e);
    }
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}