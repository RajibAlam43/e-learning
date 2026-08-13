package com.gii.api.model.request.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCategoryRequest {

  @Size(max = 150)
  private String name;

  @Size(max = 150)
  private String nameEn;

  @Size(max = 180)
  private String slug;

  @Setter(AccessLevel.NONE)
  private UUID parentId;

  @JsonIgnore private boolean parentIdPresent;

  @JsonSetter("parentId")
  public void setParentId(UUID parentId) {
    this.parentId = parentId;
    this.parentIdPresent = true;
  }
}
