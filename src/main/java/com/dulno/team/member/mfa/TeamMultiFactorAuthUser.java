package com.dulno.team.member.mfa;

import com.dulno.core.database.DatabaseRow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class TeamMultiFactorAuthUser {
  public static TeamMultiFactorAuthUser of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).stringValue(),
      row.findCell(2).listValue(), row.findCell(3).booleanValue());
  }

  private final UUID userId;
  private final String secret;
  private final List<String> recoveryCodes;
  private final boolean confirmed;
}
