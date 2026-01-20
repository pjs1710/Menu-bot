package com.menubot.menubot.kakao.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class KakaoRequest {

    @JsonProperty("userRequest")
    private UserRequest userRequest;

    @JsonProperty("bot")
    private Bot bot;

    @JsonProperty("action")
    private Action action;

    @Data
    public static class UserRequest {
        private String utterance;
        private User user;

        @Data
        public static class User {
            private String id;
            private Map<String, String> properties;
        }
    }

    @Data
    public static class Bot {
        private String id;
        private String name;
    }

    @Data
    public static class Action {
        private String id;
        private String name;
        private Map<String, Object> params;
    }
}