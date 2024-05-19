package net.taskwolf.team;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.inject.Injector;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.command.GroupCommand;
import net.taskwolf.team.command.PermissionCommand;
import net.taskwolf.team.command.TeamCommand;
import org.springframework.boot.SpringApplication;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleDescription(name = "team", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.HIGH)
public class TeamModule extends Module {
  private Log log;

  public TeamModule(Injector injector) {
    super(injector.createChildInjector(TeamInjectionModule.create()));
  }

  @Override
  public void enable() {
    log = injector().getInstance(Log.class).subLog("Team");
    injector().getInstance(SpringApplication.class).addInitializers(
      injector().getInstance(TeamContextInitializer.class));
    registerCommand(injector().getInstance(TeamCommand.class));
    registerCommand(injector().getInstance(GroupCommand.class));
    registerCommand(injector().getInstance(PermissionCommand.class));
    initializeTaskwolfUser();
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private void initializeTaskwolfUser() {
    var executor = Executors.newSingleThreadScheduledExecutor();
    var userDatabaseTable = injector().getInstance(UserDatabaseTable.class);
    var profileDatabaseTable = injector().getInstance(
      ProfilePictureDatabaseTable.class);
    executor.schedule(() -> userDatabaseTable.findUserIfExists(TASKWOLF_USER_ID)
      .thenApply(user -> user.email().equalsIgnoreCase("Unknown") ?
        insertTaskwolfUser(userDatabaseTable, profileDatabaseTable) :
          CompletableFuture.completedFuture(null)),
      5, TimeUnit.SECONDS);
    executor.shutdown();
  }

  private User insertTaskwolfUser(
    UserDatabaseTable userDatabaseTable,
    ProfilePictureDatabaseTable profilePictureDatabaseTable
  ) {
    var password = Hashing.sha256().hashString(UUID.randomUUID().toString(),
      StandardCharsets.UTF_8).toString();
    var user = User.create(TASKWOLF_USER_ID, "Taskwolf", "support@taskwolf.net",
      password, "en", Lists.newArrayList());
    userDatabaseTable.insertUser(user);
    profilePictureDatabaseTable.insertProfilePicture(TASKWOLF_USER_ID, "");
    return user;
  }

  @Override
  public void disable() {

  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Team", "", "", ModuleInformation.Type.HIDDEN);
  }
}
