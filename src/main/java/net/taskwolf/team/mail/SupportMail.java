package net.taskwolf.team.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.List;
import java.util.UUID;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class SupportMail {
  public static SupportMail of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).listValue());
  }

  @Getter
  private final UUID requestMessage;
  @Getter
  private final String sender;
  @Getter
  private final String title;
  private final List<UUID> conversationMessages;

  public void addConversationMessage(UUID conversationMessage) {
    conversationMessages.add(conversationMessage);
  }

  public void removeConversationMessage(UUID conversationMessage) {
    conversationMessages.remove(conversationMessage);
  }

  public List<UUID> conversationMessages() {
    return List.copyOf(conversationMessages);
  }
}
