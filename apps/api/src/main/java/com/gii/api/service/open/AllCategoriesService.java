package com.gii.api.service.open;

import com.gii.api.model.response.CategoryResponse;
import com.gii.common.entity.course.Category;
import com.gii.common.repository.course.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllCategoriesService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> execute() {

        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}
