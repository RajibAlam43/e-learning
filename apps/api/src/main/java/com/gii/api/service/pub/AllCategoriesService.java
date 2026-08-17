package com.gii.api.service.pub;

import com.gii.api.model.response.CategoryResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.course.Category;
import com.gii.common.repository.course.CategoryRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllCategoriesService {

  private final CategoryRepository categoryRepository;
  private final LocalizedContentService localizedContentService;

  @Transactional(readOnly = true)
  public List<CategoryResponse> execute() {
    return categoryRepository.findAll().stream()
        .map(this::toResponse)
        .sorted(Comparator.comparing(CategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private CategoryResponse toResponse(Category category) {
    return CategoryResponse.builder()
        .id(category.getId())
        .name(localizedContentService.text(category.getName(), category.getNameEn()))
        .slug(category.getSlug())
        .parentId(category.getParent() != null ? category.getParent().getId() : null)
        .build();
  }
}
