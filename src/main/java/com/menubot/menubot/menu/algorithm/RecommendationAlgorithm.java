package com.menubot.menubot.menu.algorithm;

import com.menubot.menubot.menu.dto.MenuRecommendationDto;
import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.MealType;
import com.menubot.menubot.menu.entity.Menu;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationAlgorithm {

    /**
     * 사용자의 식사 이력을 바탕으로 메뉴를 추천합니다. (개선 버전)
     */
    public List<MenuRecommendationDto> recommend(List<MealHistory> histories,
                                                 List<Menu> allMenus,
                                                 int count) {

        // 1. 최근 5일간 먹은 메뉴 제외 (3일 → 5일로 확대)
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        Set<Long> recentMenuIds = histories.stream()
                .filter(h -> h.getEatenAt().isAfter(fiveDaysAgo))
                .map(h -> h.getMenu().getId())
                .collect(Collectors.toSet());

        // 2. 카테고리별 선호도 계산
        Map<String, Long> categoryPreference = histories.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getMenu().getCategory(),
                        Collectors.counting()
                ));

        // 3. 메뉴별 평균 평점
        Map<Long, Double> menuRatings = histories.stream()
                .filter(h -> h.getRating() != null)
                .collect(Collectors.groupingBy(
                        h -> h.getMenu().getId(),
                        Collectors.averagingInt(MealHistory::getRating)
                ));

        // 4. 후보 메뉴 점수 계산
        List<MenuRecommendationDto> candidates = allMenus.stream()
                .filter(menu -> !recentMenuIds.contains(menu.getId()))
                .map(menu -> calculateScore(menu, categoryPreference, menuRatings, histories))
                .sorted(Comparator.comparingDouble(MenuRecommendationDto::getScore).reversed())
                .limit(count)
                .collect(Collectors.toList());

        return candidates;
    }

    /**
     * 개선된 점수 계산
     */
    private MenuRecommendationDto calculateScore(Menu menu,
                                                 Map<String, Long> categoryPreference,
                                                 Map<Long, Double> menuRatings,
                                                 List<MealHistory> histories) {

        double score = 0.0;
        StringBuilder reason = new StringBuilder();

        // 1. 카테고리 선호도 (30%)
        long categoryCount = categoryPreference.getOrDefault(menu.getCategory(), 0L);
        if (histories.size() > 0) {
            double categoryScore = (categoryCount / (double) histories.size()) * 30;
            score += categoryScore;

            if (categoryCount > 0) {
                reason.append(String.format("%s 자주 드셨네요 ", menu.getCategory()));
            }
        }

        // 2. 평점 (25%)
        Double avgRating = menuRatings.get(menu.getId());
        if (avgRating != null) {
            double ratingScore = (avgRating / 5.0) * 25;
            score += ratingScore;
            reason.append(String.format("| 평점 %.1f점 ", avgRating));
        }

        // 3. 다양성 (30%) - 안 먹어본 메뉴 우대
        boolean neverEaten = histories.stream()
                .noneMatch(h -> h.getMenu().getId().equals(menu.getId()));
        if (neverEaten) {
            score += 30;
            reason.append("| 새로운 메뉴 도전! ");
        } else {
            // 오래 안 먹은 메뉴 가산점
            Optional<MealHistory> lastEaten = histories.stream()
                    .filter(h -> h.getMenu().getId().equals(menu.getId()))
                    .max(Comparator.comparing(MealHistory::getEatenAt));

            if (lastEaten.isPresent()) {
                long daysSince = java.time.temporal.ChronoUnit.DAYS.between(
                        lastEaten.get().getEatenAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                );
                if (daysSince > 10) {
                    score += 15;
                    reason.append(String.format("| %d일만에 추천 ", daysSince));
                } else if (daysSince > 7) {
                    score += 10;
                }
            }
        }

        // 4. 시간대 보너스 (15%)
        int hour = LocalDateTime.now().getHour();
        if (menu.getCalories() != null) {
            // 점심시간 (11-15시): 가벼운 메뉴 선호
            if (hour >= 11 && hour < 15 && menu.getCalories() < 500) {
                score += 10;
                reason.append("| 가벼운 점심 ");
            }
            // 저녁시간 (17-21시): 든든한 메뉴 선호
            else if (hour >= 17 && hour < 21 && menu.getCalories() > 500) {
                score += 10;
                reason.append("| 든든한 저녁 ");
            }
        }

        // 5. 랜덤 요소 (변동성)
        score += new Random().nextDouble() * 5;

        String finalReason = reason.toString().trim();
        if (finalReason.isEmpty()) {
            finalReason = "맛있게 드세요! 😊";
        }

        return MenuRecommendationDto.builder()
                .menuName(menu.getName())
                .category(menu.getCategory())
                .calories(menu.getCalories())
                .spicyLevel(menu.getSpicyLevel())
                .score(score)
                .recommendationReason(finalReason)
                .build();
    }

    /**
     * 랜덤 추천 (이력 없을 때)
     */
    public MenuRecommendationDto randomRecommend(List<Menu> allMenus) {
        if (allMenus.isEmpty()) {
            return null;
        }

        Menu randomMenu = allMenus.get(new Random().nextInt(allMenus.size()));

        return MenuRecommendationDto.builder()
                .menuName(randomMenu.getName())
                .category(randomMenu.getCategory())
                .calories(randomMenu.getCalories())
                .spicyLevel(randomMenu.getSpicyLevel())
                .score(50.0)
                .recommendationReason("첫 추천이에요! 맛있게 드세요 😊")
                .build();
    }
}