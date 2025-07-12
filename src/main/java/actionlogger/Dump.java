package actionlogger;

import actionlogger.writers.JsonWriter;
import lombok.Data;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Dump {
    public static void handleDump(@Nonnull ActionLoggerPlugin plugin, @Nonnull Client client, @Nonnull JsonWriter writer, @Nonnull ItemManager itemManager, @Nonnull String[] args) {
        var dumps = new HashSet<String>();
        // default dump everything
        dumps.add("grounditems");
        dumps.add("objects");
        dumps.add("npcs");

        if (args.length > 1) {
            dumps.clear();
            for (var i = 1; i < args.length; i++) {
                dumps.add(args[i].toLowerCase());
            }
        }

        var worldView = client.getTopLevelWorldView();
        if (worldView == null) {
            plugin.addChatMessage("Dump failed: worldView is null");
            return;
        }

        var scene = worldView.getScene();
        if (scene == null) {
            plugin.addChatMessage("Dump failed: scene is null");
            return;
        }

        var sceneData = new SceneData(scene.getBaseX(), scene.getBaseY(), scene.isInstance());
        List<GroundItemData> groundItemData = new ArrayList<>();
        List<ObjectData> objectData = new ArrayList<>();
        List<NPCData> npcData = new ArrayList<>();
        var tiles = scene.getTiles();
        for (var z = 0; z < tiles.length; z++) {
            for (var x = 0; x < tiles[z].length; x++) {
                for (var y = 0; y < tiles[z][x].length; y++) {
                    var tile = tiles[z][x][y];

                    if (tile == null) {
                        continue;
                    }

                    var groundItems = tile.getGroundItems();
                    if (groundItems != null) {
                        for (var groundItem : groundItems) {
                            var itemComposition = itemManager.getItemComposition(groundItem.getId());
                            groundItemData.add(new GroundItemData(x, y, z, itemComposition.getId(), itemComposition.getName(), groundItem.getQuantity()));
                        }
                    }

                    var decorativeObjects = tile.getDecorativeObject();
                    if (decorativeObjects != null) {
                        objectData.add(new ObjectData(x, y, z, decorativeObjects.getId(), "DECORATIVE"));
                    }

                    var groundObject = tile.getGroundObject();
                    if (groundObject != null) {
                        objectData.add(new ObjectData(x, y, z, groundObject.getId(), "GROUND"));
                    }

                    var wallObject = tile.getWallObject();
                    if (wallObject != null) {
                        objectData.add(new ObjectData(x, y, z, wallObject.getId(), "WALL"));
                    }

                    var gameObjects = tile.getGameObjects();
                    if (gameObjects != null) {
                        for (var gameObject : gameObjects) {
                            if (gameObject != null) {
                                objectData.add(new ObjectData(x, y, z, gameObject.getId(), "GAME"));
                            }
                        }
                    }
                }
            }
        }

        for (var npc : worldView.npcs()) {
            var localLocation = npc.getLocalLocation();
            npcData.add(new NPCData(localLocation.getSceneX(), localLocation.getSceneY(), worldView.getPlane(), npc.getId(), npc.getName()));
        }

        var dumpData = new DumpData(
            sceneData,
            worldView.getPlane(),
            dumps.contains("grounditems") ? groundItemData : null,
            dumps.contains("objects") ? objectData : null,
            dumps.contains("npcs") ? npcData : null
        );

        writer.write("DUMP", dumpData);
    }

    @Data
    private static class SceneData {
        private final int baseX;
        private final int baseY;

        private final boolean isInstance;
    }

    @Data
    private static class GroundItemData {
        private final int x;
        private final int y;
        private final int z;

        private final int id;
        private final String name;
        private final int quantity;
    }

    @Data
    private static class ObjectData {
        private final int x;
        private final int y;
        private final int z;

        private final int id;
        private final String type;
    }

    @Data
    private static class NPCData {
        private final int x;
        private final int y;
        private final int z;

        private final int id;
        private final String name;
    }

    @Data
    private static class DumpData {
        private final SceneData scene;
        private final int worldViewPlane;

        private final @Nullable List<GroundItemData> groundItems;
        private final @Nullable List<ObjectData> objects;
        private final @Nullable List<NPCData> npcs;
    }
}
