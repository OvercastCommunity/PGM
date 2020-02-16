package tc.oc.pgm.rotation.vote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import tc.oc.pgm.api.map.MapInfo;

public class DefaultMapVotePicker implements MapVotePicker {

  private static final int VOTE_OPTIONS = 5;

  @Override
  public List<MapInfo> pickMaps(Map<MapInfo, Double> mapScores) {

    List<MapInfo> selectedMaps = new ArrayList<>();

    mapScores = new HashMap<>(mapScores);

    for (int i = 0; i < VOTE_OPTIONS; i++) {
      NavigableMap<Double, MapInfo> cumulativeScores = new TreeMap<>();
      double maxWeight = 0;
      for (Map.Entry<MapInfo, Double> map : mapScores.entrySet()) {
        cumulativeScores.put(maxWeight += weigh(map.getKey(), map.getValue()), map.getKey());
      }
      Map.Entry<Double, MapInfo> selectedMap =
          cumulativeScores.higherEntry(Math.random() * maxWeight);

      if (selectedMap == null) break; // No more maps to be picked
      selectedMaps.add(selectedMap.getValue());
      mapScores.remove(selectedMap.getValue());
    }

    return selectedMaps;
  }

  public Double weigh(MapInfo map, Double score) {
    return score == null || score <= 0 ? 0 : Math.max(Math.pow(score, 2), Double.MIN_VALUE);
  }
}
