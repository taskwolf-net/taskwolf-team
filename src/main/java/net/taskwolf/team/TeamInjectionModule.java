package net.taskwolf.team;

import net.taskwolf.core.access.AccessInjectionModule;
import net.taskwolf.core.bundle.BundleInjectionModule;
import net.taskwolf.core.command.CommandInjectionModule;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseInjectionModule;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.environment.EnvironmentInjectionModule;
import net.taskwolf.core.error.ErrorInjectionModule;
import net.taskwolf.core.intro.IntroInjectionModule;
import net.taskwolf.core.locale.LocaleInjectionModule;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.mail.Mail;
import net.taskwolf.core.mail.MailConfiguration;
import net.taskwolf.core.mail.MailFactory;
import net.taskwolf.core.mail.MailInjectionModule;
import net.taskwolf.core.maintenance.MaintenanceInjectionModule;
import net.taskwolf.core.offer.OfferInjectionModule;
import net.taskwolf.core.organization.OrganizationInjectionModule;
import net.taskwolf.core.question.QuestionInjectionModule;
import net.taskwolf.core.sale.SaleInjectionModule;
import net.taskwolf.core.session.SessionInjectionModule;
import net.taskwolf.core.stripe.StripeInjectionModule;
import net.taskwolf.core.template.TemplateInjectionModule;
import net.taskwolf.core.ticket.TicketInjectionModule;
import net.taskwolf.core.user.UserInjectionModule;
import net.taskwolf.core.whitelist.WhitelistInjectionModule;
import net.taskwolf.team.cluster.ClusterInjectionModule;
import net.taskwolf.team.configuration.TeamConfiguration;
import net.taskwolf.team.distribution.TeamDistributionConfiguration;
import net.taskwolf.team.error.TeamErrorInjectionModule;
import net.taskwolf.team.group.GroupInjectionModule;
import net.taskwolf.team.hetzner.HetznerInjectionModule;
import net.taskwolf.team.member.TeamMemberInjectionModule;
import net.taskwolf.team.offer.TeamOfferInjectionModule;
import net.taskwolf.team.telegraf.TelegrafInjectionModule;
import net.taskwolf.workflow.operation.OperationDatabaseTable;
import net.taskwolf.workflow.throttle.WorkflowThrottleDatabaseTable;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.jsonwebtoken.SignatureAlgorithm;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@RequiredArgsConstructor(staticName = "create")
public final class TeamInjectionModule extends AbstractModule {
  @Override
  protected void configure() {
    install(IntroInjectionModule.create());
    install(DatabaseInjectionModule.create());
    install(UserInjectionModule.create());
    install(SessionInjectionModule.create());
    install(OrganizationInjectionModule.create());
    install(MailInjectionModule.create());
    install(BundleInjectionModule.create());
    install(TicketInjectionModule.create());
    install(QuestionInjectionModule.create());
    install(SaleInjectionModule.create());
    install(WhitelistInjectionModule.create());
    install(LocaleInjectionModule.create());
    install(AccessInjectionModule.create());
    install(StripeInjectionModule.create());
    install(OfferInjectionModule.create());
    install(TemplateInjectionModule.create());
    install(TeamOfferInjectionModule.create());
    install(CommandInjectionModule.create());
    install(TeamMemberInjectionModule.create());
    install(GroupInjectionModule.create());
    install(MaintenanceInjectionModule.create());
    install(ClusterInjectionModule.create());
    install(ErrorInjectionModule.create());
    install(TeamErrorInjectionModule.create());
    install(HetznerInjectionModule.create());
    install(TelegrafInjectionModule.create());
    install(EnvironmentInjectionModule.create());
  }

  @Provides
  @Singleton
  Log provideTeamLog() throws Exception {
    return Log.create("Team", "/logs/");
  }

  @Provides
  @Singleton
  TeamConfiguration provideTeamConfiguration() throws Exception {
    return TeamConfiguration.createAndLoad();
  }

  @Provides
  @Singleton
  TeamDistributionConfiguration provideTeamDistributionConfiguration() throws Exception {
    return TeamDistributionConfiguration.createAndLoad();
  }

  @Provides
  @Singleton
  @Named("panelKey")
  Key providePanelKey(TeamConfiguration configuration) {
    return new SecretKeySpec(configuration.panelSecret()
      .getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
  }

  @Provides
  @Singleton
  @Named("panelRefreshKey")
  Key provideRefreshKey(TeamConfiguration configuration) {
    return new SecretKeySpec(configuration.refreshSecret()
      .getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
  }

  @Provides
  @Singleton
  @Named("verificationMail")
  Mail provideVerificationMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("verification");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("supportMail")
  Mail provideSupportMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("support");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("saleMail")
  Mail provideSaleMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("sale");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("changeMail")
  Mail provideChangeMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("change");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  ApiClient provideKubernetesApiClient() throws Exception {
    var client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);
    return client;
  }

  @Provides
  @Singleton
  CoreV1Api provideKubernetesCoreApi(ApiClient client) {
    var coreApi = new CoreV1Api();
    coreApi.setApiClient(client);
    return coreApi;
  }

  @Provides
  @Singleton
  AppsV1Api provideKubernetesAppsApi(ApiClient client) {
    var appsApi = new AppsV1Api();
    appsApi.setApiClient(client);
    return appsApi;
  }

  @Provides
  @Singleton
  BatchV1Api provideKubernetesBatchApi(ApiClient client) {
    var batchApi = new BatchV1Api();
    batchApi.setApiClient(client);
    return batchApi;
  }

  @Provides
  @Singleton
  Exec provideKubernetesExecutionApi(ApiClient client) {
    var executionApi = new Exec();
    executionApi.setApiClient(client);
    executionApi.setOnUnhandledError(throwable -> {});
    return executionApi;
  }

  @Provides
  @Singleton
  Copy provideKubernetesCopyApi(ApiClient client) {
    var copyApi = new Copy();
    copyApi.setApiClient(client);
    copyApi.setOnUnhandledError(throwable -> {});
    return copyApi;
  }

  @Provides
  @Singleton
  OperationDatabaseTable provideOperationDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var operationDatabaseTable = OperationDatabaseTable.create(connection,
      keyspace);
    operationDatabaseTable.createIfNotExists();
    return operationDatabaseTable;
  }

  @Provides
  @Singleton
  WorkflowThrottleDatabaseTable provideWorkflowThrottleDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var workflowThrottleDatabaseTable = WorkflowThrottleDatabaseTable.create(
      connection, keyspace);
    workflowThrottleDatabaseTable.createIfNotExists();
    return workflowThrottleDatabaseTable;
  }
}
