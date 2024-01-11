package net.taskwolf.team.mail;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class SupportMailDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "support_mail";

  public static SupportMailDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("requestMessage", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("conversationMessages", DatabaseDataType.TEXT));
    return new SupportMailDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private SupportMailDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertMail(SupportMail mail) {
    insertMail(mail.requestMessageId(), mail.conversationMessageIds());
  }

  public void  insertMail(
    String requestMessageId, List<String> conversationMessageIds
  ) {
    insert(DatabaseRow.of(requestMessageId, conversationMessageIds));
  }

  public void addMailMessage(String requestMessageId, String conversationMessageId) {
    findMail(requestMessageId).thenAccept(group ->
      addMailMessage(group, conversationMessageId));
  }

  private void addMailMessage(SupportMail mail, String conversationMessageId) {
    mail.addConversationMessage(conversationMessageId);
    updateMail(mail);
  }

  public void removeMailMessage(String requestMessageId, String conversationMessageId) {
    findMail(requestMessageId).thenAccept(group ->
      removeMailMessage(group, conversationMessageId));
  }

  private void removeMailMessage(SupportMail mail, String conversationMessageId) {
    mail.removeConversationMessage(conversationMessageId);
    updateMail(mail);
  }

  private void updateMail(SupportMail supportMail) {
    update(DatabaseCell.create(supportMail.requestMessageId()),
      DatabaseRow.of(supportMail.requestMessageId(),
        supportMail.conversationMessageIds()));
  }

  public void deleteMail(String requestMessageId) {
    delete(DatabaseCell.create(requestMessageId));
  }

  public CompletableFuture<Boolean> mailExists(String requestMessageId) {
    return exists(DatabaseCell.create(requestMessageId));
  }

  public CompletableFuture<SupportMail> findMail(String requestMessageId) {
    return selectRow(DatabaseCell.create(requestMessageId)).thenApply(SupportMail::of);
  }

  public CompletableFuture<List<SupportMail>> findAllMails() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(SupportMail::of).collect(Collectors.toList()));
  }
}
