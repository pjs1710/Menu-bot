package com.menubot.menubot.kakao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {

    @Builder.Default
    private List<Object> outputs = new ArrayList<>();

    @Builder.Default
    private List<Object> quickReplies = new ArrayList<>();

    public Template addOutput(Object output) {
        this.outputs.add(output);
        return this;
    }

    public Template addQuickReply(Object quickReply) {
        this.quickReplies.add(quickReply);
        return this;
    }
}