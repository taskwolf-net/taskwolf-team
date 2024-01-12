package net.taskwolf.team.mail;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class SupportMailDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "support_mail";

  public static SupportMailDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("requestMessage", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("sender", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("title", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("expirationTime", DatabaseDataType.BIGINT));
    columns.add(DatabaseListColumn.create("conversationMessages", DatabaseDataType.UUID));
    return new SupportMailDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private SupportMailDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertMail(SupportMail mail) {
    return insertMail(mail.requestMessage(), mail.sender(), mail.title(),
      mail.expirationTime(), mail.conversationMessages());
  }

  public CompletableFuture<Void> insertMail(
    UUID id,  String sender, String title, long expirationTime,
    List<UUID> conversationMessages
  ) {
    return insert(DatabaseRow.of(id, sender, title, expirationTime,
      conversationMessages));
  }

  public CompletableFuture<UUID> generateAvailableMailId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    mailExists(id).thenApply(exists -> exists ?
      generateAvailableMailId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public CompletableFuture<Void> addMailMessage(UUID id, UUID conversationMessage) {
    var futureResponse = new CompletableFuture<Void>();
    findMail(id).thenAccept(mail -> addMailMessage(mail, conversationMessage)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> addMailMessage(SupportMail mail, UUID conversationMessage) {
    mail.addConversationMessage(conversationMessage);
    return updateMail(mail);
  }

  public CompletableFuture<Void> removeMailMessage(UUID id, UUID conversationMessage) {
    var futureResponse = new CompletableFuture<Void>();
    findMail(id).thenAccept(mail -> removeMailMessage(mail, conversationMessage)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> removeMailMessage(SupportMail mail, UUID conversationMessage) {
    mail.removeConversationMessage(conversationMessage);
    return updateMail(mail);
  }

  public CompletableFuture<Void> resetMailExpirationTime(UUID id) {
    var futureResponse = new CompletableFuture<Void>();
    findMail(id).thenAccept(mail -> resetMailExpirationTime(mail)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> resetMailExpirationTime(SupportMail mail) {
    mail.resetExpirationTime();
    return updateMail(mail);
  }

  public CompletableFuture<Void> disableMailExpirationTime(UUID id) {
    var futureResponse = new CompletableFuture<Void>();
    findMail(id).thenAccept(mail -> disableMailExpirationTime(mail)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> disableMailExpirationTime(SupportMail mail) {
    mail.disableExpirationTime();
    return updateMail(mail);
  }

  public CompletableFuture<Void> updateMail(SupportMail supportMail) {
    return update(DatabaseCell.create(supportMail.requestMessage()),
      DatabaseRow.of(supportMail.requestMessage(), supportMail.sender(),
        supportMail.title(), supportMail.expirationTime(),
        supportMail.conversationMessages()));
  }

  public CompletableFuture<Void> deleteMail(UUID id) {
    return delete(DatabaseCell.create(id));
  }

  public CompletableFuture<Boolean> mailExists(UUID id) {
    return exists(DatabaseCell.create(id));
  }

  public CompletableFuture<SupportMail> findMail(UUID id) {
    return selectRow(DatabaseCell.create(id)).thenApply(SupportMail::of);
  }

  public CompletableFuture<List<SupportMail>> findMailsBySender(String sender) {
    return selectRows("sender='" + sender + "' ALLOW FILTERING")
      .thenApply(rows -> rows.stream().map(SupportMail::of).toList());
  }

  public CompletableFuture<List<SupportMail>> findAllMails() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(SupportMail::of).collect(Collectors.toList()));
  }
}
