package net.taskwolf.team.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.UUID;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class SupportMailMessage {
  public static SupportMailMessage of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).stringValue(),
      row.findCell(4).longValue());
  }

  private final UUID id;
  private final String sender;
  private final String title;
  private final String body;
  private final long time;
}
