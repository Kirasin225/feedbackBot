package com.company.feedbackbot.service;

import com.company.feedbackbot.domain.Feedback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GoogleDocsService {

  private static final Logger log = LoggerFactory.getLogger(GoogleDocsService.class);

  @Value("${google.docs.documentId}")
  private String documentId;
  @Value("${google.docs.credentialsPath}")
  private String credentialsPath;

  private Docs buildDocs() throws IOException, GeneralSecurityException {
    var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    var jsonFactory = JacksonFactory.getDefaultInstance();
    var creds = GoogleCredentials.fromStream(new ClassPathResource(credentialsPath.replace("classpath:", "")).getInputStream())
            .createScoped(List.of("https://www.googleapis.com/auth/documents"));
    var requestInitializer = new HttpCredentialsAdapter(creds);
    return new Docs.Builder(httpTransport, jsonFactory, requestInitializer)
            .setApplicationName("FeedbackBot").build();
  }

  public void appendFeedbackRow(Feedback fb) {
    try {
      Docs docs = buildDocs();

      Document doc = docs.documents().get(documentId).execute();

      int insertIndex = 1;
      if (doc.getBody() != null && doc.getBody().getContent() != null && !doc.getBody().getContent().isEmpty()) {
        int maxEndIndex = doc.getBody().getContent().stream()
                .map(StructuralElement::getEndIndex)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        insertIndex = Math.max(1, maxEndIndex - 1);
      }

      String timestamp = Optional.ofNullable(fb.getCreatedAt())
              .map(dt -> {
                try {
                  return DateTimeFormatter.ISO_INSTANT.format(dt);
                } catch (Exception ex) {
                  return dt.toString();
                }
              })
              .orElse("");

      String role = Optional.ofNullable(fb.getStaffProfile())
              .map(sp -> Optional.ofNullable(sp.getRole()).map(Object::toString).orElse(""))
              .orElse("");

      String branch = Optional.ofNullable(fb.getStaffProfile())
              .map(sp -> Optional.ofNullable(sp.getBranch()).orElse(""))
              .orElse("");

      String sentiment = Optional.ofNullable(fb.getSentiment()).map(Object::toString).orElse("");

      String line = String.format("[%s] %s@%s | crit=%d | %s%n%s%n%n",
              timestamp,
              role,
              branch,
              Optional.ofNullable(fb.getCriticality()).orElse(0),
              sentiment,
              Optional.ofNullable(fb.getText()).orElse("")
      );

      Request insert = new Request().setInsertText(new InsertTextRequest()
              .setText(line)
              .setLocation(new Location().setIndex(insertIndex)));

      BatchUpdateDocumentRequest batch = new BatchUpdateDocumentRequest().setRequests(List.of(insert));
      docs.documents().batchUpdate(documentId, batch).execute();
    } catch (Exception e) {
      log.error("Не удалось добавить запись в Google Docs", e);
    }
  }
}

