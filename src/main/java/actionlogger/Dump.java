package actionlogger;

import actionlogger.writers.JsonWriter;
import lombok.Data;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Dump {
    public static void handleDump(@Nonnull ActionLoggerPlugin plugin, @Nonnull Client client, @Nonnull JsonWriter writer, @Nonnull ItemManager itemManager, @Nonnull String[] args) {
        assert (client.isClientThread());

        var dumps = new HashSet<String>();
        // default dump everything
        dumps.add("grounditems");
        dumps.add("objects");
        dumps.add("npcs");
        dumps.add("widgets");

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
        List<ObjectData> decorativeObjectData = new ArrayList<>();
        List<ObjectData> wallObjectData = new ArrayList<>();
        List<ObjectData> gameObjectData = new ArrayList<>();
        List<ObjectData> groundObjectData = new ArrayList<>();
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
                        decorativeObjectData.add(new ObjectData(x, y, z, decorativeObjects.getId()));
                    }

                    var groundObject = tile.getGroundObject();
                    if (groundObject != null) {
                        groundObjectData.add(new ObjectData(x, y, z, groundObject.getId()));
                    }

                    var wallObject = tile.getWallObject();
                    if (wallObject != null) {
                        wallObjectData.add(new ObjectData(x, y, z, wallObject.getId()));
                    }

                    var gameObjects = tile.getGameObjects();
                    if (gameObjects != null) {
                        for (var gameObject : gameObjects) {
                            if (gameObject != null) {
                                gameObjectData.add(new ObjectData(x, y, z, gameObject.getId()));
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

        var widgets = new ArrayList<WidgetData>();

        for (var widgetRoot : client.getWidgetRoots()) {
            var widgetData = WidgetData.from(widgetRoot);
        }

        var dumpData = new DumpData(
            sceneData,
            worldView.getPlane(),
            dumps.contains("grounditems") ? groundItemData : null,
            dumps.contains("objects") ? decorativeObjectData : null,
            dumps.contains("objects") ? wallObjectData : null,
            dumps.contains("objects") ? gameObjectData : null,
            dumps.contains("objects") ? groundObjectData : null,
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
    }

    @Data
    private static class NPCData {
        private final int x;
        private final int y;
        private final int z;

        private final int id;
        private final String name;
    }

    @Value
    private static class WidgetData {
        int id;
        int type;
        int contentType;
        int parentId;
        String text;
        String name;
        int itemID;
        int modelID;
        int spriteID;
        Rectangle bounds;
        boolean hidden;
        List<WidgetData> dynamicChildren;
        List<WidgetData> staticChildren;
        List<WidgetData> nestedChildren;

        public static WidgetData from(@Nonnull Widget w) {
            return new WidgetData(
                w.getId(),
                w.getType(),
                w.getContentType(),
                w.getParentId(),
                w.getText(),
                w.getName(),
                w.getItemId(),
                w.getModelId(),
                w.getSpriteId(),
                w.getBounds(),
                w.isHidden(),
                Arrays.stream(w.getDynamicChildren()).filter(Objects::nonNull).map(WidgetData::from).collect(Collectors.toList()),
                Arrays.stream(w.getStaticChildren()).filter(Objects::nonNull).map(WidgetData::from).collect(Collectors.toList()),
                Arrays.stream(w.getNestedChildren()).filter(Objects::nonNull).map(WidgetData::from).collect(Collectors.toList())
            );
        }
    }

    @Data
    private static class DumpData {
        private final SceneData scene;
        private final int worldViewPlane;

        private final @Nullable List<GroundItemData> groundItems;
        private final @Nullable List<ObjectData> decorativeObjects;
        private final @Nullable List<ObjectData> wallObjects;
        private final @Nullable List<ObjectData> gameObjects;
        private final @Nullable List<ObjectData> groundObjects;
        private final @Nullable List<NPCData> npcs;
    }
}
