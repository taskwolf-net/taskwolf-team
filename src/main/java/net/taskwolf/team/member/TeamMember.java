package net.taskwolf.team.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.List;
import java.util.UUID;

@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class TeamMember {
  public static TeamMember of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).listValue());
  }

  @Getter
  private final UUID userId;
  @Getter
  private String group;
  private final List<String> permissions;

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
