package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateCategoryRequest;
import com.gii.api.model.request.admin.UpdateCategoryRequest;
import com.gii.api.model.response.admin.AdminCategoryResponse;
import com.gii.common.entity.course.Category;
import com.gii.common.repository.course.CategoryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCategoryManagementService {

  private final CategoryRepository categoryRepository;

  @Transactional(readOnly = true)
  public List<AdminCategoryResponse> list() {
    return categoryRepository.findAll().stream()
        .map(this::toResponse)
        .sorted(Comparator.comparing(AdminCategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public AdminCategoryResponse create(CreateCategoryRequest request) {
    String slug = requiredText(request.slug(), "Category slug is required");
    ensureSlugAvailable(slug, null);
    Category category =
        Category.builder()
            .name(requiredText(request.name(), "Bangla category name is required"))
            .nameEn(requiredText(request.nameEn(), "English category name is required"))
            .slug(slug)
            .parent(findParent(request.parentId()))
            .build();
    return toResponse(categoryRepository.save(category));
  }

  public AdminCategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
    Category category = findCategory(categoryId);
    if (request.getName() != null) {
      category.setName(requiredText(request.getName(), "Bangla category name is required"));
    }
    if (request.getNameEn() != null) {
      category.setNameEn(requiredText(request.getNameEn(), "English category name is required"));
    }
    if (request.getSlug() != null) {
      String slug = requiredText(request.getSlug(), "Category slug is required");
      ensureSlugAvailable(slug, categoryId);
      category.setSlug(slug);
    }
    if (request.isParentIdPresent()) {
      Category parent = findParent(request.getParentId());
      ensureValidParent(category, parent);
      category.setParent(parent);
    }
    return toResponse(categoryRepository.save(category));
  }

  private Category findCategory(UUID categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
  }

  private Category findParent(UUID parentId) {
    return parentId == null
        ? null
        : categoryRepository
            .findById(parentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Parent category not found"));
  }

  private void ensureValidParent(Category category, Category parent) {
    Category current = parent;
    while (current != null) {
      if (current.getId().equals(category.getId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Category cannot be its own ancestor");
      }
      current = current.getParent();
    }
  }

  private void ensureSlugAvailable(String slug, UUID currentCategoryId) {
    categoryRepository
        .findBySlug(slug)
        .filter(existing -> !existing.getId().equals(currentCategoryId))
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "Category slug already exists");
            });
  }

  private String requiredText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  private AdminCategoryResponse toResponse(Category category) {
    return AdminCategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .nameEn(category.getNameEn())
        .slug(category.getSlug())
        .parentId(category.getParent() != null ? category.getParent().getId() : null)
        .createdAt(category.getCreatedAt())
        .build();
  }
}
