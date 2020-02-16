package tc.oc.pgm.rotation.vote;

import java.util.List;
import java.util.Map;
import tc.oc.pgm.api.map.MapInfo;

/**
 * Responsible for picking the set of maps that will be on the vote. It's able to apply any
 * arbitrary rule to how the maps are picked from the available ones.
 */
public interface MapVotePicker {
  List<MapInfo> pickMaps(Map<MapInfo, Double> scores);

  Double weigh(MapInfo map, Double score);
}
