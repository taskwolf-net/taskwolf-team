package net.taskwolf.team.group;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.List;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class Group {
  public static Group of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).listValue());
  }

  @Getter
  private final String name;
  private final List<String> permissions;

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
