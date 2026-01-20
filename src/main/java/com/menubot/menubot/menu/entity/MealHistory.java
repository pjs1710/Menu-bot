package com.menubot.menubot.menu.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kakaoUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private LocalDateTime eatenAt;

    @Column
    private Integer rating; // 1-5 (만족도)

    @Builder
    public MealHistory(String kakaoUserId, Menu menu, MealType mealType,
                       LocalDateTime eatenAt, Integer rating) {
        this.kakaoUserId = kakaoUserId;
        this.menu = menu;
        this.mealType = mealType;
        this.eatenAt = eatenAt;
        this.rating = rating;
    }
}