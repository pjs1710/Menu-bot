package com.menubot.menubot.kakao.dto.response;

import com.menubot.menubot.kakao.dto.SimpleTextWrapper;
import com.menubot.menubot.kakao.dto.Template;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoResponse {

    private String version;
    private Template template;

    public static KakaoResponse createSimpleText(String text) {
        SimpleTextWrapper.SimpleTextContent content = SimpleTextWrapper.SimpleTextContent.builder()
                .text(text)
                .build();

        SimpleTextWrapper wrapper = SimpleTextWrapper.builder()
                .simpleText(content)
                .build();

        Template template = new Template();
        template.getOutputs().add(wrapper);

        return KakaoResponse.builder()
                .version("2.0")
                .template(template)
                .build();
    }
}
