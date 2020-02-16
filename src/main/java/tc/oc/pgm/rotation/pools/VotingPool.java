package tc.oc.pgm.rotation.pools;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.vote.DefaultMapVotePicker;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.MapVotePicker;
import tc.oc.util.collection.VoterSet;

public class VotingPool extends MapPool {

  // Arbitrary default of 1 in 5 players liking each map
  private static final double DEFAULT_SCORE = 0.2;

  // The algorithm used to pick the maps for next vote.
  // Eventually should allow other algorithms via config.
  public static MapVotePicker MAP_PICKER = new DefaultMapVotePicker();

  // How much score to add/remove on a map every cycle
  private final double ADJUST_FACTOR;
  // The current rating of maps. Eventually should be persisted elsewhere.
  private final Map<MapInfo, Double> mapScores = new HashMap<>();

  private MapPoll currentPoll;

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    ADJUST_FACTOR = DEFAULT_SCORE / maps.size(); // Make maps tend towards default slowly

    for (MapInfo map : maps) {
      mapScores.put(map, DEFAULT_SCORE);
    }
  }

  public MapPoll getCurrentPoll() {
    return currentPoll;
  }

  public double getMapScore(MapInfo map) {
    return mapScores.get(map);
  }

  /** Ticks scores for all maps, making them go slowly towards DEFAULT_WEIGHT. */
  private void tickScores(MapInfo currentMap) {
    // If the current map isn't from this pool, ignore ticking
    if (!mapScores.containsKey(currentMap)) return;
    mapScores.replaceAll(
        (mapScores, value) ->
            value > DEFAULT_SCORE
                ? Math.max(value - ADJUST_FACTOR, DEFAULT_SCORE)
                : Math.min(value + ADJUST_FACTOR, DEFAULT_SCORE));
    mapScores.put(currentMap, 0d);
  }

  private void updateScores(Map<MapInfo, VoterSet> votes) {
    double voters = votes.values().stream().flatMap(VoterSet::stream).distinct().count();
    if (voters == 0) return;
    votes.forEach((m, v) -> mapScores.put(m, Math.max(v.size() / voters, Double.MIN_VALUE)));
  }

  @Override
  public MapInfo popNextMap() {
    if (currentPoll == null) return getRandom();

    MapInfo map = currentPoll.finishVote();
    updateScores(currentPoll.getVotes());
    currentPoll = null;
    return map != null ? map : getRandom();
  }

  @Override
  public MapInfo getNextMap() {
    return null;
  }

  @Override
  public void setNextMap(MapInfo map) {
    currentPoll = null;
  }

  @Override
  public void unloadPool(Match match) {
    tickScores(match.getMap());
  }

  @Override
  public void matchEnded(Match match) {
    tickScores(match.getMap());
    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            20 * 5,
            () -> {
              // Start poll here, to avoid starting it if you set next another map.
              if (manager.getOverriderMap() != null) return;
              // If there is a restart queued, don't start a vote
              if (RestartManager.isQueued()) return;
              currentPoll = new MapPoll(match, MAP_PICKER.pickMaps(mapScores));
              match.getPlayers().forEach(currentPoll::sendBook);
            });
  }
}
