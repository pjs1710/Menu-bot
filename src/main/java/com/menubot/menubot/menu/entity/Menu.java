package com.menubot.menubot.menu.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String category; // 한식, 중식, 일식, 양식, 분식 등

    @Column
    private Integer calories;

    @Column
    private Integer spicyLevel; // 0-5 (매운 정도)

    @Builder
    public Menu(String name, String category, Integer calories, Integer spicyLevel) {
        this.name = name;
        this.category = category;
        this.calories = calories;
        this.spicyLevel = spicyLevel;
    }
}