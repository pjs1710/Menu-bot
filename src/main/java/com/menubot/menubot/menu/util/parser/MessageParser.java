package com.menubot.menubot.menu.util.parser;

import com.menubot.menubot.menu.entity.MealType;
import com.menubot.menubot.menu.entity.Menu;
import com.menubot.menubot.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageParser {

    private final MenuService menuService;

    /**
     * 메시지에서 메뉴와 식사 타입 추출
     */
    public ParsedMeal parseMealMessage(String message) {
        log.debug("Parsing message: {}", message);

        // 1. 식사 타입 결정
        MealType mealType = determineMealType(message);

        // 2. 메뉴 이름 추출
        String menuName = extractMenuName(message);

        if (menuName == null) {
            log.debug("Could not extract menu name from message");
            return null;
        }

        // 3. DB에서 메뉴 찾기 (유사도 매칭 포함)
        Optional<Menu> matchedMenu = findBestMatchingMenu(menuName);

        String finalMenuName = matchedMenu.map(Menu::getName).orElse(menuName);

        log.debug("Parsed - MealType: {}, Menu: {}", mealType, finalMenuName);

        return new ParsedMeal(mealType, finalMenuName);
    }

    /**
     * 메시지에서 메뉴 이름 추출 (매우 유연하게)
     */
    private String extractMenuName(String message) {
        // 노이즈 제거
        String cleaned = message
                .replaceAll("먹었어|먹었다|먹음|드셨어|드셨다|드심|먹을래|먹자", "")
                .replaceAll("점심|저녁|아침|오늘|어제|내일", "")
                .replaceAll("에|을|를|이|가|은|는", "")
                .trim();

        // 1. DB의 모든 메뉴와 비교
        List<Menu> allMenus = menuService.getAllMenus();
        for (Menu menu : allMenus) {
            if (cleaned.contains(menu.getName()) || menu.getName().contains(cleaned)) {
                log.debug("Direct match found: {}", menu.getName());
                return menu.getName();
            }
        }

        // 2. 2글자 이상인 한글만 추출
        Pattern koreanPattern = Pattern.compile("[가-힣]{2,}");
        Matcher matcher = koreanPattern.matcher(cleaned);

        if (matcher.find()) {
            String extracted = matcher.group();
            log.debug("Extracted Korean text: {}", extracted);
            return extracted;
        }

        // 3. 원본 cleaned 사용
        if (cleaned.length() >= 2) {
            return cleaned;
        }

        return null;
    }

    /**
     * 메뉴 이름 유사도 매칭 (오타 허용)
     */
    private Optional<Menu> findBestMatchingMenu(String input) {
        List<Menu> allMenus = menuService.getAllMenus();

        // 정확히 일치하는 메뉴 우선
        Optional<Menu> exactMatch = allMenus.stream()
                .filter(menu -> menu.getName().equals(input))
                .findFirst();

        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // 부분 일치
        Optional<Menu> partialMatch = allMenus.stream()
                .filter(menu -> menu.getName().contains(input) || input.contains(menu.getName()))
                .findFirst();

        if (partialMatch.isPresent()) {
            log.debug("Partial match found: {} for input: {}", partialMatch.get().getName(), input);
            return partialMatch;
        }

        // 유사도 계산 (레벤슈타인 거리)
        Menu bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Menu menu : allMenus) {
            int distance = levenshteinDistance(input, menu.getName());
            if (distance < bestDistance && distance <= 2) { // 오타 2글자까지 허용
                bestDistance = distance;
                bestMatch = menu;
            }
        }

        if (bestMatch != null) {
            log.debug("Fuzzy match found: {} (distance: {}) for input: {}",
                    bestMatch.getName(), bestDistance, input);
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * 레벤슈타인 거리 계산 (문자열 유사도)
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 식사 타입 결정 (시간 기반 + 키워드)
     */
    private MealType determineMealType(String message) {
        // 명시적 키워드 우선
        if (message.contains("점심") || message.contains("런치")) {
            return MealType.LUNCH;
        }
        if (message.contains("저녁") || message.contains("디너")) {
            return MealType.DINNER;
        }

        // 시간 기반 자동 판단
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        if (hour >= 11 && hour < 15) {
            return MealType.LUNCH;
        } else {
            return MealType.DINNER;
        }
    }

    /**
     * 파싱 결과 클래스
     */
    public static class ParsedMeal {
        public final MealType mealType;
        public final String menuName;

        public ParsedMeal(MealType mealType, String menuName) {
            this.mealType = mealType;
            this.menuName = menuName;
        }
    }
}
