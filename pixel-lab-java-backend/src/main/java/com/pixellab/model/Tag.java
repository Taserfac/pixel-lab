package com.pixellab.model;

public class Tag {
  private int id;
  private String name;
  private int usageCount;

  public Tag() {
  }

  public Tag(int id, String name, int usageCount) {
    this.id = id;
    this.name = name;
    this.usageCount = usageCount;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public void setUsageCount(int usageCount) {
    this.usageCount = usageCount;
  }
}
