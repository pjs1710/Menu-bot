package com.menubot.menubot.kakao.controller;

import com.menubot.menubot.kakao.dto.request.KakaoRequest;
import com.menubot.menubot.kakao.dto.response.KakaoResponse;
import com.menubot.menubot.menu.dto.MenuRecommendationDto;
import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.MealType;
import com.menubot.menubot.menu.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/kakao")
@RequiredArgsConstructor
public class KakaoController {

    private final RecommendationService recommendationService;

    /**
     * 메뉴 추천 엔드포인트
     */
    @PostMapping(value = "/recommend", produces = "application/json;charset=UTF-8")
    public ResponseEntity<KakaoResponse> recommendMenu(@RequestBody KakaoRequest request) {
        String userId = request.getUserRequest().getUser().getId();
        String utterance = request.getUserRequest().getUtterance();

        log.info("Recommendation request - userId: {}, utterance: {}", userId, utterance);

        // 점심/저녁 구분
        MealType mealType = determineMealType(utterance);

        // 메뉴 추천
        List<MenuRecommendationDto> recommendations = recommendationService.recommendMenus(userId, 3);

        log.info("Received {} recommendations", recommendations.size());

        if (recommendations.isEmpty()) {
            log.warn("No recommendations available");
            return ResponseEntity.ok(
                    KakaoResponse.createSimpleText("죄송합니다. 추천할 메뉴가 없습니다.")
            );
        }

        // 추천 결과 포매팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("🍽️ %s 추천 메뉴입니다!\n\n", mealType.getDescription()));

        for (int i = 0; i < recommendations.size(); i++) {
            MenuRecommendationDto rec = recommendations.get(i);
            log.debug("Recommendation {}: {} ({})", i+1, rec.getMenuName(), rec.getCategory());
            response.append(String.format("%d. %s (%s)\n",
                    i + 1, rec.getMenuName(), rec.getCategory()));

            if (rec.getRecommendationReason() != null && !rec.getRecommendationReason().isEmpty()) {
                response.append(String.format("   💡 %s\n", rec.getRecommendationReason()));
            }
            response.append("\n");
        }

        String responseText = response.toString();
        log.info("Response text length: {}", responseText.length());
        log.debug("Response text: {}", responseText);

        try {
            KakaoResponse kakaoResponse = KakaoResponse.createSimpleText(responseText);
            log.info("KakaoResponse created successfully");
            return ResponseEntity.ok(kakaoResponse);
        } catch (Exception e) {
            log.error("Error creating KakaoResponse", e);
            return ResponseEntity.ok(
                    KakaoResponse.createSimpleText("응답 생성 중 오류가 발생했습니다.")
            );
        }
    }

    /**
     * 식사 기록 엔드포인트
     */
    @PostMapping(value = "/record", produces = "application/json;charset=UTF-8")
    public ResponseEntity<KakaoResponse> recordMeal(@RequestBody KakaoRequest request) {
        String userId = request.getUserRequest().getUser().getId();
        String utterance = request.getUserRequest().getUtterance();

        log.info("Record request - userId: {}, utterance: {}", userId, utterance);

        // 메시지 파싱: "점심 먹었어 김치찌개" 또는 "저녁에 파스타 먹음"
        ParsedMeal parsed = parseMealMessage(utterance);

        if (parsed == null || parsed.menuName == null) {
            return ResponseEntity.ok(
                    KakaoResponse.createSimpleText(
                            "어떤 메뉴를 드셨는지 알려주세요!\n" +
                                    "예: '점심 먹었어 김치찌개' 또는 '저녁에 파스타'"
                    )
            );
        }

        // 식사 기록 저장
        MealHistory history = recommendationService.recordMeal(
                userId,
                parsed.menuName,
                parsed.mealType,
                null // 평점은 나중에 추가 기능으로
        );

        String response = String.format(
                "✅ 기록했어요!\n%s에 '%s' 드셨군요.\n\n다음 추천에 반영할게요!",
                parsed.mealType.getDescription(),
                parsed.menuName
        );

        return ResponseEntity.ok(KakaoResponse.createSimpleText(response));
    }

    /**
     * 최근 식사 이력 조회
     */
    @PostMapping(value = "/history", produces = "application/json;charset=UTF-8")
    public ResponseEntity<KakaoResponse> getHistory(@RequestBody KakaoRequest request) {
        String userId = request.getUserRequest().getUser().getId();

        log.info("History request - userId: {}", userId);

        List<MealHistory> recentMeals = recommendationService.getRecentMeals(userId, 7);

        if (recentMeals.isEmpty()) {
            return ResponseEntity.ok(
                    KakaoResponse.createSimpleText("아직 기록된 식사가 없습니다.")
            );
        }

        StringBuilder response = new StringBuilder("📊 최근 7일 식사 기록\n\n");

        recentMeals.stream()
                .limit(10)
                .forEach(meal -> {
                    response.append(String.format("• %s - %s (%s)\n",
                            meal.getEatenAt().toLocalDate(),
                            meal.getMenu().getName(),
                            meal.getMealType().getDescription()
                    ));
                });

        return ResponseEntity.ok(KakaoResponse.createSimpleText(response.toString()));
    }

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Menu Bot is running!");
    }

    // === 유틸리티 메서드 ===

    private MealType determineMealType(String utterance) {
        if (utterance.contains("점심") || utterance.contains("런치")) {
            return MealType.LUNCH;
        } else if (utterance.contains("저녁") || utterance.contains("디너")) {
            return MealType.DINNER;
        }

        // 기본값: 현재 시간 기준으로 판단
        int hour = java.time.LocalTime.now().getHour();
        return (hour >= 11 && hour < 15) ? MealType.LUNCH : MealType.DINNER;
    }

    private ParsedMeal parseMealMessage(String utterance) {
        MealType mealType = determineMealType(utterance);

        // 패턴 매칭: "점심 먹었어 김치찌개", "저녁에 파스타 먹음" 등
        Pattern pattern1 = Pattern.compile("(점심|저녁).*?([가-힣]+)\\s*(먹|드)");
        Pattern pattern2 = Pattern.compile("(먹|드).*?([가-힣]{2,})");

        Matcher matcher1 = pattern1.matcher(utterance);
        if (matcher1.find()) {
            String menuName = matcher1.group(2).trim();
            if (menuName.length() >= 2) {
                return new ParsedMeal(mealType, menuName);
            }
        }

        Matcher matcher2 = pattern2.matcher(utterance);
        if (matcher2.find()) {
            String menuName = matcher2.group(2).trim();
            if (menuName.length() >= 2 && !menuName.equals("먹었") && !menuName.equals("드셨")) {
                return new ParsedMeal(mealType, menuName);
            }
        }

        return null;
    }

    private static class ParsedMeal {
        MealType mealType;
        String menuName;

        ParsedMeal(MealType mealType, String menuName) {
            this.mealType = mealType;
            this.menuName = menuName;
        }
    }
}