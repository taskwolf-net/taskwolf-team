package net.taskwolf.team;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.taskwolf.core.command.CommandRegistry;
import net.taskwolf.core.command.CommandTask;
import net.taskwolf.core.command.implementation.ClearCommand;
import net.taskwolf.core.intro.Intro;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.command.GroupCommand;
import net.taskwolf.team.command.PermissionCommand;
import net.taskwolf.team.command.TeamCommand;
import net.taskwolf.team.configuration.TeamConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class TeamApplication {
  public static void main(String[] args) {
    var injector = Guice.createInjector(TeamInjectionModule.create());
    injector.getInstance(Intro.class).print();
    var log = injector.getInstance(Log.class);
    log.info("Initializing Taskwolf - Team");
    var commandRegistry = CommandRegistry.create();
    registerCommands(commandRegistry, injector);
    new Thread(() -> CommandTask.create(log, commandRegistry).start()).start();
    var application = new SpringApplication(TeamApplication.class);
    application.setDefaultProperties(Collections.singletonMap("server.port",
      injector.getInstance(TeamConfiguration.class).restPort()));
    application.addInitializers(injector.getInstance(TeamContextInitializer.class));
    application.run(args);
    initializeTaskwolfUser(injector);
    log.info("Successfully booted Taskwolf - Team");
  }

  private static void registerCommands(
    CommandRegistry registry, Injector injector
  ) {
    registry.register(injector.getInstance(ClearCommand.class));
    registry.register(injector.getInstance(TeamCommand.class));
    registry.register(injector.getInstance(GroupCommand.class));
    registry.register(injector.getInstance(PermissionCommand.class));
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static void initializeTaskwolfUser(Injector injector) {
    var executor = Executors.newSingleThreadScheduledExecutor();
    var userDatabaseTable = injector.getInstance(UserDatabaseTable.class);
    executor.schedule(() -> userDatabaseTable.findUserIfExists(TASKWOLF_USER_ID)
        .thenApply(user -> user.email().equalsIgnoreCase("Unknown") ?
          insertTaskwolfUser(userDatabaseTable) :
          CompletableFuture.completedFuture(null)),
      5, TimeUnit.SECONDS);
    executor.shutdown();
  }

  private static User insertTaskwolfUser(
    UserDatabaseTable userDatabaseTable
  ) {
    var password = Hashing.sha256().hashString(UUID.randomUUID().toString(),
      StandardCharsets.UTF_8).toString();
    var user = User.create(TASKWOLF_USER_ID, "Taskwolf", "support@taskwolf.net",
      password, "en", Lists.newArrayList());
    userDatabaseTable.insertUser(user);
    return user;
  }
}
