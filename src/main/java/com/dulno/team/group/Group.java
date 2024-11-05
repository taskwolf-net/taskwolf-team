package com.dulno.team.group;

import com.dulno.core.database.DatabaseRow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class Group {
  public static Group of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).integerValue(),
      row.findCell(2).stringValue(), row.findCell(3).listValue());
  }

  @Getter
  private String name;
  @Getter
  private int priority;
  @Getter
  private String color;
  private final List<String> permissions;

  public void rename(String newName) {
    name = newName;
  }

  public void changePriority(int newPriority) {
    priority = newPriority;
  }

  public void changeColor(String newColor) {
    color = newColor;
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
