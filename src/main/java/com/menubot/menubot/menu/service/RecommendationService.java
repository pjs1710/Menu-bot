package com.menubot.menubot.menu.service;

import com.menubot.menubot.menu.algorithm.RecommendationAlgorithm;
import com.menubot.menubot.menu.dto.MenuRecommendationDto;
import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.MealType;
import com.menubot.menubot.menu.entity.Menu;
import com.menubot.menubot.menu.repository.MealHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final MealHistoryRepository mealHistoryRepository;
    private final MenuService menuService;
    private final RecommendationAlgorithm recommendationAlgorithm;

    /**
     * 사용자에게 메뉴를 추천합니다
     */
    public List<MenuRecommendationDto> recommendMenus(String userId, int count) {
        log.debug("Recommending {} menus for user: {}", count, userId);

        // 사용자의 식사 이력 조회
        List<MealHistory> histories = mealHistoryRepository.findByKakaoUserId(userId);

        // 전체 메뉴 조회
        List<Menu> allMenus = menuService.getAllMenus();

        if (allMenus.isEmpty()) {
            log.warn("No menus available in database");
            return List.of();
        }

        // 이력이 없으면 랜덤 추천
        if (histories.isEmpty()) {
            log.debug("No history found, returning random recommendation");
            MenuRecommendationDto random = recommendationAlgorithm.randomRecommend(allMenus);
            return random != null ? List.of(random) : List.of();
        }

        // 알고리즘으로 추천
        return recommendationAlgorithm.recommend(histories, allMenus, count);
    }

    /**
     * 사용자가 메뉴를 먹었다고 기록합니다
     */
    @Transactional
    public MealHistory recordMeal(String userId, String menuName, MealType mealType, Integer rating) {
        log.debug("Recording meal - userId: {}, menu: {}, type: {}", userId, menuName, mealType);

        // 메뉴 찾기 (없으면 생성)
        Menu menu = menuService.findByName(menuName)
                .orElseGet(() -> {
                    log.debug("Menu not found, creating new menu: {}", menuName);
                    return menuService.saveMenu(Menu.builder()
                            .name(menuName)
                            .category("기타") // 기본 카테고리
                            .build());
                });

        // 식사 이력 저장
        MealHistory history = MealHistory.builder()
                .kakaoUserId(userId)
                .menu(menu)
                .mealType(mealType)
                .eatenAt(LocalDateTime.now())
                .rating(rating)
                .build();

        return mealHistoryRepository.save(history);
    }

    /**
     * 사용자의 최근 식사 이력을 조회합니다
     */
    public List<MealHistory> getRecentMeals(String userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return mealHistoryRepository.findRecentMeals(userId, startDate);
    }

    /**
     * 사용자가 가장 자주 먹는 메뉴를 조회합니다
     */
    public List<Object[]> getMostEatenMenus(String userId) {
        return mealHistoryRepository.findMostEatenMenus(userId);
    }

    /**
     * 사용자의 전체 이력 조회
     */
    public List<MealHistory> getUserHistories(String userId) {
        return mealHistoryRepository.findByKakaoUserId(userId);
    }

    /**
     * 전체 메뉴 조회
     */
    public List<Menu> getAllMenus() {
        return menuService.getAllMenus();
    }
}