package tc.oc.pgm.rotation.vote;

import app.ashcon.intake.CommandException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.MapNameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.util.collection.VoterSet;
import tc.oc.util.components.Components;
import tc.oc.world.NMSHacks;

/** Represents a polling process, with a set of options. */
public class MapPoll {
  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  private static final int TITLE_LENGTH_CUTOFF = 15;

  private final WeakReference<Match> match;

  private final Map<MapInfo, VoterSet> votes = new LinkedHashMap<>();

  public MapPoll(Match match, List<MapInfo> maps) {
    this.match = new WeakReference<>(match);
    maps.forEach(m -> votes.put(m, new VoterSet()));
  }

  public void announceWinner(MatchPlayer viewer, MapInfo winner) {
    for (MapInfo pgmMap : votes.keySet())
      viewer.sendMessage(getMapChatComponent(viewer, pgmMap, pgmMap.equals(winner)));

    // Check if the winning map name's length suitable for the top title, otherwise subtitle
    boolean top = winner.getName().length() < TITLE_LENGTH_CUTOFF;
    Component mapName = winner.getStyledMapName(MapNameStyle.COLOR).bold(true);

    viewer.showTitle(
        top ? mapName : Components.blank(), top ? Components.blank() : mapName, 5, 60, 5);
  }

  private Component getMapChatComponent(MatchPlayer viewer, MapInfo map, boolean winner) {
    boolean voted = votes.get(map).contains(viewer.getId());
    return new PersonalizedText(
        new PersonalizedText("["),
        new PersonalizedText(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE, voted ? ChatColor.GREEN : ChatColor.DARK_RED),
        new PersonalizedText(" ").bold(!voted), // Fix 1px symbol diff
        new PersonalizedText("" + votes.get(map).getVotes(), ChatColor.YELLOW),
        new PersonalizedText("] "),
        map.getStyledMapName(
            winner ? MapNameStyle.HIGHLIGHT_WITH_AUTHORS : MapNameStyle.COLOR_WITH_AUTHORS));
  }

  public void sendBook(MatchPlayer viewer) {
    String title = ChatColor.GOLD + "" + ChatColor.BOLD;
    title += AllTranslations.get().translate("command.pool.vote.book.title", viewer.getBukkit());

    ItemStack is = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) is.getItemMeta();
    meta.setAuthor("PGM");
    meta.setTitle(title);

    List<Component> content = new ArrayList<>(votes.size() + 2);
    content.add(
        new PersonalizedText(
            new PersonalizedTranslatable("command.pool.vote.book.header"), ChatColor.DARK_PURPLE));
    content.add(new PersonalizedText("\n\n"));

    for (MapInfo pgmMap : votes.keySet()) content.add(getMapBookComponent(viewer, pgmMap));

    NMSHacks.setBookPages(meta, new PersonalizedText(content).render(viewer.getBukkit()));
    is.setItemMeta(meta);

    ItemStack held = viewer.getInventory().getItemInHand();
    if (held.getType() != Material.WRITTEN_BOOK
        || !title.equals(((BookMeta) is.getItemMeta()).getTitle())) {
      viewer.getInventory().setHeldItemSlot(2);
    }
    viewer.getInventory().setItemInHand(is);
    if (viewer.getSettings().getValue(SettingKey.VOTE) == SettingValue.VOTE_ON)
      NMSHacks.openBook(is, viewer.getBukkit());
  }

  private Component getMapBookComponent(MatchPlayer viewer, MapInfo map) {
    boolean voted = votes.get(map).contains(viewer.getId());
    return new PersonalizedText(
            new PersonalizedText(
                voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
                voted ? ChatColor.DARK_GREEN : ChatColor.DARK_RED),
            new PersonalizedText(" ").bold(!voted), // Fix 1px symbol diff
            new PersonalizedText(map.getName() + "\n", ChatColor.BOLD, ChatColor.GOLD))
        .hoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new PersonalizedText(
                    map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
                    ChatColor.YELLOW)
                .render())
        .clickEvent(ClickEvent.Action.RUN_COMMAND, "/votenext " + map.getName());
  }

  /**
   * Toggle the vote of a user for a certain map. Player is allowed to vote for several maps.
   *
   * @param vote The map to vote for/against
   * @param player The player voting
   * @return true if the player is now voting for the map, false otherwise
   * @throws CommandException If the map is not an option in the poll
   */
  public boolean toggleVote(MapInfo vote, UUID player) throws CommandException {
    VoterSet votes = this.votes.get(vote);
    if (votes == null) throw new CommandException(vote.getName() + " is not an option in the poll");

    if (votes.add(player)) return true;
    votes.remove(player);
    return false;
  }

  /** @return The map currently winning the vote, null if no vote is running. */
  private MapInfo getMostVotedMap() {
    return votes.entrySet().stream()
        .max(Comparator.comparingInt(e -> e.getValue().getVotes()))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Picks a winner and ends the vote, updating map scores based on votes
   *
   * @return The picked map to play after the vote
   */
  public MapInfo finishVote() {
    MapInfo picked = getMostVotedMap();
    Match match = this.match.get();
    if (match != null) {
      match.getPlayers().forEach(player -> announceWinner(player, picked));
    }

    return picked;
  }

  public Map<MapInfo, VoterSet> getVotes() {
    return votes;
  }
}
