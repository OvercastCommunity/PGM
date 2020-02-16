package tc.oc.util.collection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.Permissions;

/** A collection that will cache total votes casted, premium votes counting double */
public class VoterSet {

  private Set<UUID> voters = new HashSet<>();
  private Integer votes = null;

  public boolean add(UUID uuid) {
    boolean modified = voters.add(uuid);
    if (modified) votes = null;
    return modified;
  }

  public void remove(UUID uuid) {
    voters.remove(uuid);
    votes = null;
  }

  public boolean contains(UUID uuid) {
    return voters.contains(uuid);
  }

  public int size() {
    return voters.size();
  }

  public int getVotes() {
    return votes != null ? votes : updateVotes();
  }

  public Stream<UUID> stream() {
    return voters.stream();
  }

  private int updateVotes() {
    return votes =
        voters.stream()
            .map(Bukkit::getPlayer)
            // Count disconnected players as 1, can't test for their perms
            .mapToInt(p -> p == null || !p.hasPermission(Permissions.PREMIUM) ? 1 : 2)
            .sum();
  }
}
