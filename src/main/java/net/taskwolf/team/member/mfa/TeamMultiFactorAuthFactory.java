package net.taskwolf.team.member.mfa;

import net.taskwolf.team.member.TeamDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class TeamMultiFactorAuthFactory {
  private final TeamMultiFactorAuthDatabaseTable multiFactorAuthDatabaseTable;
  private final TeamDatabaseTable teamDatabaseTable;

  public TeamMultiFactorAuth createAuth(UUID userId) {
    return TeamMultiFactorAuth.create(multiFactorAuthDatabaseTable,
      teamDatabaseTable, userId);
  }
}
