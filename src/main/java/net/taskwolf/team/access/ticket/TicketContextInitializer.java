package net.taskwolf.team.access.ticket;

import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.activity.UserActivityDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class TicketContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final UserActivityDatabaseTable activityDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("ticketDatabaseTable", ticketDatabaseTable);
    beanFactory.registerSingleton("ticketMessageDatabaseTable",
      ticketMessageDatabaseTable);
    beanFactory.registerSingleton("activityDatabaseTable", activityDatabaseTable);
    beanFactory.registerSingleton("teamWorkDatabaseTable", teamWorkDatabaseTable);
  }
}
