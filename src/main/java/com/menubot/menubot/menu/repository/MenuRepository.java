package com.menubot.menubot.menu.repository;

import com.menubot.menubot.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByName(String name);

    List<Menu> findByCategory(String category);

    @Query("SELECT m FROM Menu m WHERE m.name LIKE %:keyword%")
    List<Menu> searchByName(@Param("keyword") String keyword);
}
