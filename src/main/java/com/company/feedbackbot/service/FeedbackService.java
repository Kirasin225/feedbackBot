package com.company.feedbackbot.service;

import com.company.feedbackbot.domain.Feedback;
import com.company.feedbackbot.domain.StaffProfile;
import com.company.feedbackbot.repo.FeedbackRepository;
import com.company.feedbackbot.repo.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

  private final StaffProfileRepository profileRepo;
  private final FeedbackRepository feedbackRepo;
  private final OpenAIService openAIService;
  private final GoogleDocsService googleDocsService;
  private final TrelloService trelloService;

  @Transactional
  public Feedback saveFeedback(Long telegramUserId, String text) {
    StaffProfile p = profileRepo.findByTelegramUserId(telegramUserId)
        .orElseThrow(() -> new IllegalStateException("Профіль не налаштовано. Оберіть посаду та філію."));

    var analysis = openAIService.analyze(text);

    Feedback fb = Feedback.builder()
        .staffProfile(p)
        .text(text)
        .sentiment(analysis.getSentiment())
        .criticality(analysis.getCriticality())
        .resolution(analysis.getResolution())
        .build();

    fb = feedbackRepo.save(fb);
    googleDocsService.appendFeedbackRow(fb);

    if (analysis.getCriticality() != null && analysis.getCriticality() >= 4) {
      trelloService.createCardForFeedback(fb);
    }
    return fb;
  }
}
