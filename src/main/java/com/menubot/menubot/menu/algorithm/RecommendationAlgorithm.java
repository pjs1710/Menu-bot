package com.menubot.menubot.menu.algorithm;

import com.menubot.menubot.menu.dto.MenuRecommendationDto;
import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.Menu;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationAlgorithm {

    /**
     * 사용자의 식사 이력을 바탕으로 메뉴를 추천합니다.
     * 알고리즘 로직:
     * 1. 최근 3일간 먹은 메뉴는 제외
     * 2. 자주 먹은 카테고리에서 추천 (선호도 반영)
     * 3. 높은 평점을 받은 메뉴 우선
     * 4. 점수 계산: 카테고리 선호도(40%) + 평점(30%) + 다양성(30%)
     */
    public List<MenuRecommendationDto> recommend(List<MealHistory> histories,
                                                 List<Menu> allMenus,
                                                 int count) {

        // 1. 최근 3일간 먹은 메뉴 ID 수집
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        Set<Long> recentMenuIds = histories.stream()
                .filter(h -> h.getEatenAt().isAfter(threeDaysAgo))
                .map(h -> h.getMenu().getId())
                .collect(Collectors.toSet());

        // 2. 카테고리별 선호도 계산 (빈도수 기반)
        Map<String, Long> categoryPreference = histories.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getMenu().getCategory(),
                        Collectors.counting()
                ));

        // 3. 메뉴별 평균 평점 계산
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

    private MenuRecommendationDto calculateScore(Menu menu,
                                                 Map<String, Long> categoryPreference,
                                                 Map<Long, Double> menuRatings,
                                                 List<MealHistory> histories) {

        double score = 0.0;
        StringBuilder reason = new StringBuilder();

        // 카테고리 선호도 점수 (40%)
        long categoryCount = categoryPreference.getOrDefault(menu.getCategory(), 0L);
        double categoryScore = (categoryCount / (double) histories.size()) * 40;
        score += categoryScore;

        if (categoryCount > 0) {
            reason.append(String.format("%s을(를) 자주 드셨네요! ", menu.getCategory()));
        }

        // 평점 점수 (30%)
        Double avgRating = menuRatings.get(menu.getId());
        if (avgRating != null) {
            double ratingScore = (avgRating / 5.0) * 30;
            score += ratingScore;
            reason.append(String.format("이전 평점 %.1f점. ", avgRating));
        }

        // 다양성 점수 (30%) - 안 먹어본 메뉴에 가산점
        boolean neverEaten = histories.stream()
                .noneMatch(h -> h.getMenu().getId().equals(menu.getId()));
        if (neverEaten) {
            score += 30;
            reason.append("새로운 메뉴 도전! ");
        }

        // 랜덤 요소 추가 (약간의 변동성)
        score += new Random().nextDouble() * 5;

        return MenuRecommendationDto.builder()
                .menuName(menu.getName())
                .category(menu.getCategory())
                .calories(menu.getCalories())
                .spicyLevel(menu.getSpicyLevel())
                .score(score)
                .recommendationReason(reason.toString().trim())
                .build();
    }

    /**
     * 랜덤으로 메뉴를 추천합니다 (이력이 없을 때)
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
                .recommendationReason("첫 추천이에요! 한번 드셔보세요 😊")
                .build();
    }
}