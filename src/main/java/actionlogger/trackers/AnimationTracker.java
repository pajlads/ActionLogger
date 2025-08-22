package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

@RequiredArgsConstructor
public class AnimationTracker {
    private static final String ANIMATION_PLAYER_CHANGED = "ANIMATION_PLAYER_CHANGED";

    private final JsonWriter writer;
    private final Client client;

    private int interactionId = -1;
    private String interactionMenuOption = null;
    private String interactionMenuTarget = null;
    private WorldPoint interactionPosition = null;
    private int oldAnimation = -1;
    private int oldPoseAnimation = -1;

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        interactionId = event.getId();
        interactionMenuOption = event.getMenuOption();
        interactionMenuTarget = event.getMenuTarget();
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null) {
            return;
        }
        Tile tile = worldView.getSelectedSceneTile();
        if (tile == null) {
            return;
        }
        interactionPosition = tile.getWorldLocation();
    }

    @Subscribe(priority = -1) // late priority for player.getWorldLocation() to update
    public void onAnimationChanged(AnimationChanged event) {
        Player player = client.getLocalPlayer();
        if (player == null || !player.equals(event.getActor())) {
            return;
        }
        int animation = player.getAnimation();
        int poseAnimation = player.getPoseAnimation();
        if (animation == oldAnimation && poseAnimation == oldPoseAnimation) {
            return;
        }
        this.writer.write(ANIMATION_PLAYER_CHANGED, new PlayerAnimationChangedData(
            animation,
            poseAnimation,
            oldAnimation,
            oldPoseAnimation,
            player.getWorldLocation(),
            interactionId,
            interactionMenuOption,
            interactionMenuTarget,
            interactionPosition
        ));
        oldAnimation = animation;
        oldPoseAnimation = poseAnimation;
    }

    @Value
    private static class PlayerAnimationChangedData {
        int animation;
        int poseAnimation;
        int oldAnimation;
        int oldPoseAnimation;
        WorldPoint playerPosition;
        int interactionId;
        String interactionMenuOption;
        String interactionMenuTarget;
        WorldPoint interactionPosition;
    }
}
