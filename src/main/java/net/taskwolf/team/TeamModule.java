package net.taskwolf.team;

import com.google.inject.Injector;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.team.command.GroupCommand;
import net.taskwolf.team.command.PermissionCommand;
import net.taskwolf.team.command.TeamCommand;
import org.springframework.boot.SpringApplication;

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
  }

  @Override
  public void disable() {

  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Team", "", "", ModuleInformation.Type.HIDDEN);
  }
}
