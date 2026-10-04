package net.taskwolf.team.access.verification;

import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.member.TeamEmailChangeDatabaseTable;
import net.taskwolf.team.member.TeamPasswordResetDatabaseTable;
import net.taskwolf.team.member.TeamVerificationDatabaseTable;
import net.taskwolf.team.member.session.TeamSessionDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.maxmind.geoip2.DatabaseReader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class VerificationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final UserDatabaseTable userDatabaseTable;
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final TeamEmailChangeDatabaseTable teamEmailChangeDatabaseTable;
  private final TeamPasswordResetDatabaseTable teamPasswordResetDatabaseTable;
  private final TeamSessionDatabaseTable teamSessionDatabaseTable;
  private final DatabaseReader databaseReader;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("userDatabaseTable", userDatabaseTable);
    beanFactory.registerSingleton("teamVerificationDatabaseTable",
      teamVerificationDatabaseTable);
    beanFactory.registerSingleton("teamEmailChangeDatabaseTable",
      teamEmailChangeDatabaseTable);
    beanFactory.registerSingleton("teamPasswordResetDatabaseTable",
      teamPasswordResetDatabaseTable);
    beanFactory.registerSingleton("teamSessionDatabaseTable",
      teamSessionDatabaseTable);
    beanFactory.registerSingleton("databaseReader", databaseReader);
  }
}
