package net.taskwolf.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.stripe.StripeClient;
import net.taskwolf.core.bundle.BundleDatabaseTable;
import net.taskwolf.core.bundle.BundlePresetRepository;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.offer.OfferDatabaseTable;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.question.QuestionDatabaseTable;
import net.taskwolf.core.question.QuestionMessageDatabaseTable;
import net.taskwolf.core.sale.SaleDatabaseTable;
import net.taskwolf.core.sale.SaleMessageDatabaseTable;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.stripe.TerminationDatabaseTable;
import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.core.user.activity.UserActivityDatabaseTable;
import net.taskwolf.core.whitelist.WhitelistConfiguration;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamVerificationDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import net.taskwolf.team.offer.TeamOfferConfiguration;
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
  private final net.taskwolf.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final WhitelistConfiguration whitelistConfiguration;
  private final TeamDatabaseTable teamDatabaseTable;
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;
  private final TaskwolfMail verificationMail;
  private final TaskwolfMail supportMail;
  private final TaskwolfMail saleMail;
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

  @Inject
  private TeamContextInitializer(
    Log log, @Named("teamKey") Key key, DatabaseConnection databaseConnection,
    DatabaseKeyspace databaseKeyspace, UserDatabaseTable userDatabaseTable,
    UserActivityDatabaseTable activityDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    net.taskwolf.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    WhitelistConfiguration whitelistConfiguration,
    TeamDatabaseTable teamDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Named("verificationMail") TaskwolfMail verificationMail,
    @Named("supportMail") TaskwolfMail supportMail,
    @Named("saleMail") TaskwolfMail saleMail,
    TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable,
    SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable, StripeClient stripeClient,
    TerminationDatabaseTable terminationDatabaseTable,
    OfferDatabaseTable offerDatabaseTable, TeamOfferConfiguration offerConfiguration
  ) {
    this.log = log;
    this.key = key;
    this.databaseConnection = databaseConnection;
    this.databaseKeyspace = databaseKeyspace;
    this.userDatabaseTable = userDatabaseTable;
    this.activityDatabaseTable = activityDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.organizationTeamDatabaseTable = organizationTeamDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.whitelistConfiguration = whitelistConfiguration;
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
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
    beanFactory.registerSingleton("bundleDatabaseTable", bundleDatabaseTable);
    beanFactory.registerSingleton("bundlePresetRepository", bundlePresetRepository);
    beanFactory.registerSingleton(" whitelistConfiguration", whitelistConfiguration);
    beanFactory.registerSingleton("teamDatabaseTable", teamDatabaseTable);
    beanFactory.registerSingleton("teamVerificationDatabaseTable",
      teamVerificationDatabaseTable);
    beanFactory.registerSingleton("teamWorkDatabaseTable", teamWorkDatabaseTable);
    beanFactory.registerSingleton("groupDatabaseTable", groupDatabaseTable);
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
  }
}
