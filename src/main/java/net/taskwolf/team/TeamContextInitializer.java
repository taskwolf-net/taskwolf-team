package net.taskwolf.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.question.QuestionDatabaseTable;
import net.taskwolf.core.question.QuestionMessageDatabaseTable;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
public class TeamContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;
  private final TaskwolfMail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  @Inject
  private TeamContextInitializer(
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    @Named("supportMail") TaskwolfMail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("teamDatabaseTable", teamDatabaseTable);
    beanFactory.registerSingleton("groupDatabaseTable", groupDatabaseTable);
    beanFactory.registerSingleton("supportMail", supportMail);
    beanFactory.registerSingleton("questionDatabaseTable", questionDatabaseTable);
    beanFactory.registerSingleton("questionMessageDatabaseTable",
      questionMessageDatabaseTable);
    beanFactory.registerSingleton("teamWorkDatabaseTable", teamWorkDatabaseTable);
  }
}
