package net.taskwolf.team.mail;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SupportMailMessageDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "support_mail_message";

  public static SupportMailMessageDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("message", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("title", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("body", DatabaseDataType.TEXT));
    return new SupportMailMessageDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private SupportMailMessageDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertMailMessage(SupportMailMessage mailMessage) {
    insertMailMessage(mailMessage.messageId(), mailMessage.title(),
      mailMessage.body());
  }

  public void insertMailMessage(
    String messageId, String title, String body
  ) {
    insert(DatabaseRow.of(messageId, title, body));
  }

  public void deleteMailMessage(String messageId) {
    delete(DatabaseCell.create(messageId));
  }

  public CompletableFuture<Boolean> mailMessageExists(String messageId) {
    return exists(DatabaseCell.create(messageId));
  }

  public CompletableFuture<SupportMailMessage> findMailMessage(String messageId) {
    return selectRow(DatabaseCell.create(messageId)).thenApply(SupportMailMessage::of);
  }
}
