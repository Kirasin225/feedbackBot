package com.company.feedbackbot.component;

import com.company.feedbackbot.domain.Role;
import com.company.feedbackbot.domain.StaffProfile;
import com.company.feedbackbot.repo.StaffProfileRepository;
import com.company.feedbackbot.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TelegramLongPollingBotImpl extends TelegramLongPollingBot {

    private final StaffProfileRepository profileRepo;
    private final FeedbackService feedbackService;

    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message msg = update.getMessage();
                Long uid = msg.getFrom().getId();
                Optional<StaffProfile> profileOpt = profileRepo.findByTelegramUserId(uid);
                if (msg.hasText()) {
                    String txt = msg.getText().trim();
                    if (profileOpt.isEmpty()) {
                        sendWithKeyboard(uid, "Привет! Выбери роль:", roleKeyboard());
                        return;
                    } else {
                        StaffProfile profile = profileOpt.get();
                        String branch = profile.getBranch();
                        if (branch == null || branch.isBlank() || "UNKNOWN".equals(branch)) {
                            String branchToSave = txt.isEmpty() ? "UNKNOWN" : txt;
                            profile.setBranch(branchToSave);
                            profileRepo.save(profile);
                            sendSimple(uid, "Филиал сохранён: " + branchToSave + ". Спасибо! Теперь вы можете отправлять анонимные отзывы.");
                        } else {
                            feedbackService.saveFeedback(uid, txt);
                            sendSimple(uid, "Спасибо! Твой отзыв получен и анонимно обработан ✅");
                        }
                    }
                }
            }

            if (update.hasCallbackQuery()) {
                CallbackQuery cb = update.getCallbackQuery();
                Long uid = cb.getFrom().getId();
                String data = cb.getData();
                if (data != null && data.startsWith("ROLE_")) {
                    Role role = switch (data) {
                        case "ROLE_MECHANIC" -> Role.MECHANIC;
                        case "ROLE_ELECTRICIAN" -> Role.ELECTRICIAN;
                        default -> Role.MANAGER;
                    };
                    StaffProfile p = StaffProfile.builder()
                            .telegramUserId(uid)
                            .role(role)
                            .branch("UNKNOWN")
                            .build();
                    profileRepo.save(p);
                    sendSimple(uid, "Роль сохранена: " + role + ". Пожалуйста, пришлите название вашей филии (коротким сообщением).");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSimple(Long chatId, String text) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText(text);
        try { execute(m); } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText(text);
        m.setReplyMarkup(keyboard);
        try { execute(m); } catch (Exception e) { e.printStackTrace(); }
    }

    private InlineKeyboardMarkup roleKeyboard() {
        InlineKeyboardButton b1 = InlineKeyboardButton.builder().text("Механик").callbackData("ROLE_MECHANIC").build();
        InlineKeyboardButton b2 = InlineKeyboardButton.builder().text("Электрик").callbackData("ROLE_ELECTRICIAN").build();
        InlineKeyboardButton b3 = InlineKeyboardButton.builder().text("Менеджер").callbackData("ROLE_MANAGER").build();
        List<InlineKeyboardButton> row1 = List.of(b1, b2);
        List<InlineKeyboardButton> row2 = List.of(b3);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1); rows.add(row2);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}

