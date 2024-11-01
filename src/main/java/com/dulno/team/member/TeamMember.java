package com.dulno.team.member;

import com.dulno.core.database.DatabaseRow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class TeamMember {
  public static TeamMember of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).stringValue(),
      row.findCell(4).stringValue(), row.findCell(5).stringValue(),
      row.findCell(6).listValue());
  }

  @Getter
  private final UUID id;
  @Getter
  private String name;
  @Getter
  private String email;
  @Getter
  private String passwordHash;
  @Getter
  private String language;
  @Getter
  private String group;
  private final List<String> permissions;

  public void changeName(String newName) {
    name = newName;
  }

  public void changeEmail(String newEmail) {
    email = newEmail;
  }

  public void changePassword(String newPasswordHash) {
    passwordHash = newPasswordHash;
  }

  public void changeLanguage(String newLanguage) {
    language = newLanguage;
  }

  public void changeGroup(String newGroup) {
    group = newGroup;
  }

  public void addPermission(String permission) {
    permissions.add(permission);
  }

  public void removePermission(String permission) {
    permissions.remove(permission);
  }

  public List<String> permissions() {
    return List.copyOf(permissions);
  }
}
