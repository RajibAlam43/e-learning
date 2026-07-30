package com.gii.api.model.request.admin;

public enum ThumbnailOwnerType {
  COURSE("courses"),
  COLLECTION("collections"),
  MEDIA_ASSET("media-assets");

  private final String path;

  ThumbnailOwnerType(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }
}
