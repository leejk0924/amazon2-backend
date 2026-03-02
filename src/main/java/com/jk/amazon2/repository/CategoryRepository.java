package com.jk.amazon2.repository;

import com.jk.amazon2.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String>, CategoryQueryRepository {
    Optional<Category> findByCodeAndDeletedFalse(String code);
}
