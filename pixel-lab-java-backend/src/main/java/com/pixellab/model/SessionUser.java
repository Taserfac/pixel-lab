package com.pixellab.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SessionUser implements Serializable {
  private final long id;
  private final String username;
  private final String nickname;
  private final String avatar;
  private final String role;
  private final int status;

  public SessionUser(long id, String username, String nickname, String avatar, String role, int status) {
    this.id = id;
    this.username = username;
    this.nickname = nickname;
    this.avatar = avatar;
    this.role = role;
    this.status = status;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getNickname() {
    return nickname;
  }

  public String getAvatar() {
    return avatar;
  }

  public String getRole() {
    return role;
  }

  public int getStatus() {
    return status;
  }

  public boolean isAdmin() {
    return "admin".equals(role);
  }

  public Map<String, Object> toSafeMap() {
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("id", id);
    user.put("username", username);
    user.put("nickname", nickname);
    user.put("avatar", avatar);
    user.put("role", role);
    user.put("status", status);
    return user;
  }
}
