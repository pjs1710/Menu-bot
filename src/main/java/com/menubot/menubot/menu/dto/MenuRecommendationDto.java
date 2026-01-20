package com.menubot.menubot.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuRecommendationDto {

    private String menuName;
    private String category;
    private Integer calories;
    private Integer spicyLevel;
    private String recommendationReason;
    private Double score; // 추천 점수
}
