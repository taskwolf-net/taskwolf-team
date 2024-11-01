package com.dulno.team;

import com.dulno.core.bundle.BundleDatabaseTable;
import com.dulno.core.bundle.BundlePresetRepository;
import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.locale.Translation;
import com.dulno.core.log.Log;
import com.dulno.core.mail.Mail;
import com.dulno.core.mail.MailFactory;
import com.dulno.core.mail.OutgoingMailDatabaseTable;
import com.dulno.core.maintenance.MaintenanceDatabaseTable;
import com.dulno.core.offer.OfferDatabaseTable;
import com.dulno.core.organization.OrganizationDatabaseTable;
import com.dulno.core.question.QuestionDatabaseTable;
import com.dulno.core.question.QuestionMessageDatabaseTable;
import com.dulno.core.sale.SaleDatabaseTable;
import com.dulno.core.sale.SaleMessageDatabaseTable;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.core.stripe.TerminationDatabaseTable;
import com.dulno.core.template.TemplateDatabaseTable;
import com.dulno.core.ticket.TicketDatabaseTable;
import com.dulno.core.ticket.TicketMessageDatabaseTable;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.core.user.UserTargetDatabaseTable;
import com.dulno.core.user.activity.UserActivityDatabaseTable;
import com.dulno.core.whitelist.WhitelistConfiguration;
import com.dulno.core.workflow.operation.OperationDatabaseTable;
import com.dulno.core.workflow.throttle.WorkflowThrottleDatabaseTable;
import com.dulno.team.distribution.TeamDistributionConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamVerificationDatabaseTable;
import com.dulno.team.member.TeamWorkDatabaseTable;
import com.dulno.team.member.mfa.TeamMultiFactorAuthDatabaseTable;
import com.dulno.team.member.mfa.TeamMultiFactorAuthFactory;
import com.dulno.team.offer.TeamOfferConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.stripe.StripeClient;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.security.Key;

@Singleton
public class TeamContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final Log log;
  private final Key key;
  private final DatabaseConnection databaseConnection;
  private final DatabaseKeyspace databaseKeyspace;
  private final UserDatabaseTable userDatabaseTable;
  private final UserActivityDatabaseTable activityDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final OutgoingMailDatabaseTable outgoingMailDatabaseTable;
  private final com.dulno.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final WhitelistConfiguration whitelistConfiguration;
  private final TeamDatabaseTable teamDatabaseTable;
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;
  private final Translation translation;
  private final MailFactory mailFactory;
  private final Mail verificationMail;
  private final Mail supportMail;
  private final Mail saleMail;
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final TerminationDatabaseTable terminationDatabaseTable;
  private final OfferDatabaseTable offerDatabaseTable;
  private final TeamOfferConfiguration offerConfiguration;
  private final TemplateDatabaseTable templateDatabaseTable;
  private final MaintenanceDatabaseTable maintenanceDatabaseTable;
  private final TeamDistributionConfiguration teamDistributionConfiguration;
  private final UserTargetDatabaseTable userTargetDatabaseTable;
  private final OperationDatabaseTable operationDatabaseTable;
  private final WorkflowThrottleDatabaseTable workflowThrottleDatabaseTable;
  private final TeamMultiFactorAuthDatabaseTable teamMultiFactorAuthDatabaseTable;
  private final TeamMultiFactorAuthFactory teamMultiFactorAuthFactory;

  @Inject
  private TeamContextInitializer(
    Log log, @Named("teamKey") Key key, DatabaseConnection databaseConnection,
    DatabaseKeyspace databaseKeyspace, UserDatabaseTable userDatabaseTable,
    UserActivityDatabaseTable activityDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    com.dulno.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable,
    OutgoingMailDatabaseTable outgoingMailDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    WhitelistConfiguration whitelistConfiguration,
    TeamDatabaseTable teamDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, Translation translation,
    MailFactory mailFactory, @Named("verificationMail") Mail verificationMail,
    @Named("supportMail") Mail supportMail,
    @Named("saleMail") Mail saleMail,
    TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable,
    SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable, StripeClient stripeClient,
    TerminationDatabaseTable terminationDatabaseTable,
    OfferDatabaseTable offerDatabaseTable, TeamOfferConfiguration offerConfiguration,
    TemplateDatabaseTable templateDatabaseTable,
    MaintenanceDatabaseTable maintenanceDatabaseTable,
    TeamDistributionConfiguration teamDistributionConfiguration,
    UserTargetDatabaseTable userTargetDatabaseTable,
    OperationDatabaseTable operationDatabaseTable,
    WorkflowThrottleDatabaseTable workflowThrottleDatabaseTable,
    TeamMultiFactorAuthDatabaseTable teamMultiFactorAuthDatabaseTable,
    TeamMultiFactorAuthFactory teamMultiFactorAuthFactory
  ) {
    this.log = log;
    this.key = key;
    this.databaseConnection = databaseConnection;
    this.databaseKeyspace = databaseKeyspace;
    this.userDatabaseTable = userDatabaseTable;
    this.activityDatabaseTable = activityDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.organizationTeamDatabaseTable = organizationTeamDatabaseTable;
    this.outgoingMailDatabaseTable = outgoingMailDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.whitelistConfiguration = whitelistConfiguration;
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
    this.translation = translation;
    this.mailFactory = mailFactory;
    this.verificationMail = verificationMail;
    this.supportMail = supportMail;
    this.saleMail = saleMail;
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
    this.saleDatabaseTable = saleDatabaseTable;
    this.saleMessageDatabaseTable = saleMessageDatabaseTable;
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.terminationDatabaseTable = terminationDatabaseTable;
    this.offerDatabaseTable = offerDatabaseTable;
    this.offerConfiguration = offerConfiguration;
    this.templateDatabaseTable = templateDatabaseTable;
    this.maintenanceDatabaseTable = maintenanceDatabaseTable;
    this.teamDistributionConfiguration = teamDistributionConfiguration;
    this.userTargetDatabaseTable = userTargetDatabaseTable;
    this.operationDatabaseTable = operationDatabaseTable;
    this.workflowThrottleDatabaseTable = workflowThrottleDatabaseTable;
    this.teamMultiFactorAuthDatabaseTable = teamMultiFactorAuthDatabaseTable;
    this.teamMultiFactorAuthFactory = teamMultiFactorAuthFactory;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("log", log);
    beanFactory.registerSingleton("key", key);
    beanFactory.registerSingleton("databaseConnection", databaseConnection);
    beanFactory.registerSingleton("databaseKeyspace", databaseKeyspace);
    beanFactory.registerSingleton("userDatabaseTable", userDatabaseTable);
    beanFactory.registerSingleton("activityDatabaseTable", activityDatabaseTable);
    beanFactory.registerSingleton("organizationDatabaseTable",
      organizationDatabaseTable);
    beanFactory.registerSingleton("organizationTeamDatabaseTable",
      organizationTeamDatabaseTable);
    beanFactory.registerSingleton("outgoingMailDatabaseTable", outgoingMailDatabaseTable);
    beanFactory.registerSingleton("bundleDatabaseTable", bundleDatabaseTable);
    beanFactory.registerSingleton("bundlePresetRepository", bundlePresetRepository);
    beanFactory.registerSingleton(" whitelistConfiguration", whitelistConfiguration);
    beanFactory.registerSingleton("teamDatabaseTable", teamDatabaseTable);
    beanFactory.registerSingleton("teamVerificationDatabaseTable",
      teamVerificationDatabaseTable);
    beanFactory.registerSingleton("teamWorkDatabaseTable", teamWorkDatabaseTable);
    beanFactory.registerSingleton("groupDatabaseTable", groupDatabaseTable);
    beanFactory.registerSingleton("translation", translation);
    beanFactory.registerSingleton("mailFactory", mailFactory);
    beanFactory.registerSingleton("verificationMail", verificationMail);
    beanFactory.registerSingleton("supportMail", supportMail);
    beanFactory.registerSingleton("saleMail", saleMail);
    beanFactory.registerSingleton("ticketDatabaseTable", ticketDatabaseTable);
    beanFactory.registerSingleton("ticketMessageDatabaseTable",
      ticketMessageDatabaseTable);
    beanFactory.registerSingleton("questionDatabaseTable", questionDatabaseTable);
    beanFactory.registerSingleton("questionMessageDatabaseTable",
      questionMessageDatabaseTable);
    beanFactory.registerSingleton("saleDatabaseTable", saleDatabaseTable);
    beanFactory.registerSingleton("saleMessageDatabaseTable",
      saleMessageDatabaseTable);
    beanFactory.registerSingleton("stripeDatabaseTable", stripeDatabaseTable);
    beanFactory.registerSingleton("stripeClient", stripeClient);
    beanFactory.registerSingleton("terminationDatabaseTable",
      terminationDatabaseTable);
    beanFactory.registerSingleton("offerDatabaseTable", offerDatabaseTable);
    beanFactory.registerSingleton("offerConfiguration", offerConfiguration);
    beanFactory.registerSingleton("templateDatabaseTable", templateDatabaseTable);
    beanFactory.registerSingleton("maintenanceDatabaseTable", maintenanceDatabaseTable);
    beanFactory.registerSingleton("teamDistributionConfiguration",
      teamDistributionConfiguration);
    beanFactory.registerSingleton("userTargetDatabaseTable",
      userTargetDatabaseTable);
    beanFactory.registerSingleton("operationDatabaseTable",
      operationDatabaseTable);
    beanFactory.registerSingleton("workflowThrottleDatabaseTable",
      workflowThrottleDatabaseTable);
    beanFactory.registerSingleton("teamMultiFactorAuthDatabaseTable",
      teamMultiFactorAuthDatabaseTable);
    beanFactory.registerSingleton("teamMultiFactorAuthFactory",
      teamMultiFactorAuthFactory);
  }
}
