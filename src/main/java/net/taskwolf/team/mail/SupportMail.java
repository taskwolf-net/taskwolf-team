package net.taskwolf.team.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

import java.util.List;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class SupportMail {
  public static SupportMail of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).listValue());
  }

  @Getter
  private final String requestMessageId;
  private final List<String> conversationMessageIds;

  public void addConversationMessage(String conversationMessageId) {
    conversationMessageIds.add(conversationMessageId);
  }

  public void removeConversationMessage(String conversationMessageId) {
    conversationMessageIds.remove(conversationMessageId);
  }

  public List<String> conversationMessageIds() {
    return List.copyOf(conversationMessageIds);
  }
}
