package net.taskwolf.team.mail;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SupportMailMessageDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "support_mail_message";

  public static SupportMailMessageDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("title", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("body", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("time", DatabaseDataType.BIGINT));
    return new SupportMailMessageDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private SupportMailMessageDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertMailMessage(SupportMailMessage mailMessage) {
    return insertMailMessage(mailMessage.id(), mailMessage.title(),
      mailMessage.body(), mailMessage.time());
  }

  public CompletableFuture<Void> insertMailMessage(
    UUID id, String title, String body, long time
  ) {
    return insert(DatabaseRow.of(id, title, body, time));
  }

  public CompletableFuture<UUID> generateAvailableMessageId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    mailMessageExists(id).thenApply(exists -> exists ?
      generateAvailableMessageId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public CompletableFuture<Void> deleteMailMessage(UUID id) {
    return delete(DatabaseCell.create(id));
  }

  public CompletableFuture<Boolean> mailMessageExists(UUID id) {
    return exists(DatabaseCell.create(id));
  }

  public CompletableFuture<SupportMailMessage> findMailMessage(UUID id) {
    return selectRow(DatabaseCell.create(id)).thenApply(SupportMailMessage::of);
  }
}
