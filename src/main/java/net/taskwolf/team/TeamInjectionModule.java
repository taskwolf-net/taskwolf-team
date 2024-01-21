package net.taskwolf.team;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailConfiguration;
import net.taskwolf.team.group.GroupInjectionModule;
import net.taskwolf.team.mail.SupportMailInjectionModule;
import net.taskwolf.team.member.TeamMemberInjectionModule;

@RequiredArgsConstructor(staticName = "create")
public final class TeamInjectionModule extends AbstractModule {
  @Override
  protected void configure() {
    install(TeamMemberInjectionModule.create());
    install(GroupInjectionModule.create());
    install(SupportMailInjectionModule.create());
  }

  @Provides
  @Singleton
  @Named("supportMail")
  TaskwolfMail provideSupportMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("support");
    return TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}
