package net.taskwolf.team.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class SupportMailMessage {
  public static SupportMailMessage of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue());
  }

  private final String messageId;
  private final String title;
  private final String body;
}
