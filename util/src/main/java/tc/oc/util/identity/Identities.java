package tc.oc.util.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public interface Identities {

  static Identity current(Player player) {
    return new RealIdentity(player.getUniqueId(), player.getName());
  }

  static Identity from(UUID playerId, String username, @Nullable String nickname) {
    return new RealIdentity(playerId, username);
  }
}
