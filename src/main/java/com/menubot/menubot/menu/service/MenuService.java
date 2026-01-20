package com.menubot.menubot.menu.service;

import com.menubot.menubot.menu.entity.Menu;
import com.menubot.menubot.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    public Optional<Menu> findByName(String name) {
        return menuRepository.findByName(name);
    }

    public List<Menu> findByCategory(String category) {
        return menuRepository.findByCategory(category);
    }

    @Transactional
    public Menu saveMenu(Menu menu) {
        return menuRepository.save(menu);
    }

    public List<Menu> searchByName(String keyword) {
        return menuRepository.searchByName(keyword);
    }
}