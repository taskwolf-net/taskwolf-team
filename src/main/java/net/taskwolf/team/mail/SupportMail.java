package net.taskwolf.team.mail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.List;
import java.util.UUID;

@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class SupportMail {
  public static SupportMail of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).longValue(),
      row.findCell(4).listValue());
  }

  @Getter
  private final UUID requestMessage;
  @Getter
  private final String sender;
  @Getter
  private final String title;
  @Getter
  private long expirationTime;
  private final List<UUID> conversationMessages;

  public void addConversationMessage(UUID conversationMessage) {
    conversationMessages.add(conversationMessage);
  }

  public void removeConversationMessage(UUID conversationMessage) {
    conversationMessages.remove(conversationMessage);
  }

  private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 14;

  public void resetExpirationTime() {
    expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
  }

  public void disableExpirationTime() {
    expirationTime = -1;
  }

  public List<UUID> conversationMessages() {
    return List.copyOf(conversationMessages);
  }
}
