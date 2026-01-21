package com.menubot.menubot.kakao.controller;

import com.menubot.menubot.kakao.dto.request.KakaoRequest;
import com.menubot.menubot.kakao.dto.response.KakaoResponse;
import com.menubot.menubot.menu.dto.MenuRecommendationDto;
import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.MealType;
import com.menubot.menubot.menu.service.RecommendationService;
import com.menubot.menubot.menu.util.parser.MessageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/kakao")
@RequiredArgsConstructor
public class KakaoController {

    private final RecommendationService recommendationService;
    private final MessageParser messageParser;

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
     * 식사 기록 엔드포인트 (개선된 파서 사용)
     */
    @PostMapping(value = "/record", produces = "application/json;charset=UTF-8")
    public ResponseEntity<KakaoResponse> recordMeal(@RequestBody KakaoRequest request) {
        String userId = request.getUserRequest().getUser().getId();
        String utterance = request.getUserRequest().getUtterance();

        log.info("Record request - userId: {}, utterance: {}", userId, utterance);

        try {
            // 개선된 파서 사용
            MessageParser.ParsedMeal parsed = messageParser.parseMealMessage(utterance);

            if (parsed == null || parsed.menuName == null) {
                return ResponseEntity.ok(
                        KakaoResponse.createSimpleText(
                                "메뉴 이름을 찾을 수 없어요 😅\n\n" +
                                        "이렇게 말씀해주세요:\n" +
                                        "• \"김치찌개 먹었어\"\n" +
                                        "• \"점심에 파스타\"\n" +
                                        "• \"저녁 먹었어 돈카츠\""
                        )
                );
            }

            // 식사 기록 저장
            MealHistory history = recommendationService.recordMeal(
                    userId,
                    parsed.menuName,
                    parsed.mealType,
                    null
            );

            String response = String.format(
                    "✅ 기록 완료!\n\n" +
                            "%s에 '%s' 드셨군요.\n" +
                            "다음 추천에 반영할게요! 😊",
                    parsed.mealType.getDescription(),
                    history.getMenu().getName()
            );

            return ResponseEntity.ok(KakaoResponse.createSimpleText(response));

        } catch (Exception e) {
            log.error("Error recording meal", e);
            return ResponseEntity.ok(
                    KakaoResponse.createSimpleText(
                            "기록 중 오류가 발생했어요 😭\n다시 시도해주세요!"
                    )
            );
        }
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
        return ResponseEntity.ok("Menu Bot is running! (Improved Version)");
    }

    // === 유틸리티 메서드 ===

    private MealType determineMealType(String utterance) {
        if (utterance.contains("점심") || utterance.contains("런치")) {
            return MealType.LUNCH;
        } else if (utterance.contains("저녁") || utterance.contains("디너")) {
            return MealType.DINNER;
        }

        // 기본값: 현재 시간 기준으로 판단
        int hour = LocalTime.now().getHour();
        return (hour >= 11 && hour < 15) ? MealType.LUNCH : MealType.DINNER;
    }
}