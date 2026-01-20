package com.menubot.menubot.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTextWrapper {

    @JsonProperty("simpleText")
    private SimpleTextContent simpleText;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleTextContent {
        private String text;
    }
}
