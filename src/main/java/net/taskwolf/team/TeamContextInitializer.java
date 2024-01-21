package net.taskwolf.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
public class TeamContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;
  private final TaskwolfMail supportMail;
  private final SupportMailDatabaseTable supportMailDatabaseTable;
  private final SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  @Inject
  private TeamContextInitializer(
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    @Named("supportMail") TaskwolfMail supportMail,
    SupportMailDatabaseTable supportMailDatabaseTable,
    SupportMailMessageDatabaseTable supportMailMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
    this.supportMail = supportMail;
    this.supportMailDatabaseTable = supportMailDatabaseTable;
    this.supportMailMessageDatabaseTable = supportMailMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("teamDatabaseTable", teamDatabaseTable);
    beanFactory.registerSingleton("groupDatabaseTable", groupDatabaseTable);
    beanFactory.registerSingleton("supportMail", supportMail);
    beanFactory.registerSingleton("supportMailDatabaseTable", supportMailDatabaseTable);
    beanFactory.registerSingleton("supportMailMessageDatabaseTable",
      supportMailMessageDatabaseTable);
    beanFactory.registerSingleton("teamWorkDatabaseTable", teamWorkDatabaseTable);
  }
}
