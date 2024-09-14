package com.dulno.team;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.dulno.core.command.CommandRegistry;
import com.dulno.core.command.CommandTask;
import com.dulno.core.command.implementation.ClearCommand;
import com.dulno.core.intro.Intro;
import com.dulno.core.log.Log;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.team.command.GroupCommand;
import com.dulno.team.command.PermissionCommand;
import com.dulno.team.command.TeamCommand;
import com.dulno.team.configuration.TeamConfiguration;
import com.dulno.team.maintenance.TeamMaintenanceSchedule;
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
    log.info("Initializing Dulno - Team");
    var commandRegistry = CommandRegistry.create();
    registerCommands(commandRegistry, injector);
    new Thread(() -> CommandTask.create(log, commandRegistry).start()).start();
    var application = new SpringApplication(TeamApplication.class);
    application.setDefaultProperties(Collections.singletonMap("server.port",
      injector.getInstance(TeamConfiguration.class).restPort()));
    application.addInitializers(injector.getInstance(TeamContextInitializer.class));
    application.run(args);
    initializeDulnoUser(injector);
    injector.getInstance(TeamMaintenanceSchedule.class).start();
    log.info("Successfully booted Dulno - Team");
  }

  private static void registerCommands(
    CommandRegistry registry, Injector injector
  ) {
    registry.register(injector.getInstance(ClearCommand.class));
    registry.register(injector.getInstance(TeamCommand.class));
    registry.register(injector.getInstance(GroupCommand.class));
    registry.register(injector.getInstance(PermissionCommand.class));
  }

  private static final UUID DULNO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static void initializeDulnoUser(Injector injector) {
    var executor = Executors.newSingleThreadScheduledExecutor();
    var userDatabaseTable = injector.getInstance(UserDatabaseTable.class);
    executor.schedule(() -> userDatabaseTable.findUserIfExists(DULNO_USER_ID)
        .thenApply(user -> user.email().equalsIgnoreCase("Unknown") ?
          insertDulnoUser(userDatabaseTable) :
          CompletableFuture.completedFuture(null)),
      5, TimeUnit.SECONDS);
    executor.shutdown();
  }

  private static User insertDulnoUser(
    UserDatabaseTable userDatabaseTable
  ) {
    var password = Hashing.sha256().hashString(UUID.randomUUID().toString(),
      StandardCharsets.UTF_8).toString();
    var user = User.create(DULNO_USER_ID, "Dulno", "support@dulno.com",
      password, "en", Lists.newArrayList());
    userDatabaseTable.insertUser(user);
    return user;
  }
}
