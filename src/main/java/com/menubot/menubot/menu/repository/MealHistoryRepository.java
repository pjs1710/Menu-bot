package com.menubot.menubot.menu.repository;

import com.menubot.menubot.menu.entity.MealHistory;
import com.menubot.menubot.menu.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealHistoryRepository extends JpaRepository<MealHistory, Long> {

    List<MealHistory> findByKakaoUserId(String kakaoUserId);

    List<MealHistory> findByKakaoUserIdAndMealType(String kakaoUserId, MealType mealType);

    @Query("SELECT mh FROM MealHistory mh WHERE mh.kakaoUserId = :userId " +
            "AND mh.eatenAt >= :startDate ORDER BY mh.eatenAt DESC")
    List<MealHistory> findRecentMeals(@Param("userId") String userId,
                                      @Param("startDate") LocalDateTime startDate);

    @Query("SELECT mh FROM MealHistory mh WHERE mh.kakaoUserId = :userId " +
            "ORDER BY mh.eatenAt DESC")
    List<MealHistory> findAllByUserIdOrderByEatenAtDesc(@Param("userId") String userId);

    @Query("SELECT m.id, COUNT(mh) FROM MealHistory mh JOIN mh.menu m " +
            "WHERE mh.kakaoUserId = :userId GROUP BY m.id ORDER BY COUNT(mh) DESC")
    List<Object[]> findMostEatenMenus(@Param("userId") String userId);
}