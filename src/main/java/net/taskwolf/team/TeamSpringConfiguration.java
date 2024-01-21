package net.taskwolf.team;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
public class TeamSpringConfiguration {
  @Autowired
  UserDatabaseTable userDatabaseTable;
  @Autowired
  ProfilePictureDatabaseTable profilePictureDatabaseTable;
  private User taskwolfUser;

  @Bean
  @Qualifier("taskwolfUser")
  User provideTaskwolfUser() {
    return taskwolfUser;
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @PostConstruct
  private void initializeTaskwolfUser() {
    userDatabaseTable.findUserIfExists(TASKWOLF_USER_ID).thenAccept(user ->
      taskwolfUser = (user.email().equalsIgnoreCase("Unknown") ?
        insertTaskwolfUser() : user));
  }

  private User insertTaskwolfUser() {
    var password = Hashing.sha256().hashString(UUID.randomUUID().toString(),
      StandardCharsets.UTF_8).toString();
    var user = User.create(TASKWOLF_USER_ID, "Taskwolf", "support@taskwolf.net",
      password, "en", Lists.newArrayList());
    userDatabaseTable.insertUser(user);
    profilePictureDatabaseTable.insertProfilePicture(TASKWOLF_USER_ID, "");
    return user;
  }
}
