package com.company.feedbackbot.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramUpdate {
    @JsonProperty("update_id")
    private Long updateId;

    private TelegramMessage message;

    @Data
    public static class TelegramMessage {
        private Long message_id;
        private TelegramChat chat;
        private TelegramUser from;
        private String text;

        @Data
        public static class TelegramChat {
            private Long id;
        }

        @Data
        public static class TelegramUser {
            private Long id;
            private String first_name;
            private String username;
        }
    }
}

