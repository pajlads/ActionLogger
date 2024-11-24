package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InventoryTracker {
    private static final String TYPE = "INVENTORY_CHANGED";

    private final JsonWriter writer;

    private List<Integer> inventoryItemIDs = List.of(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INVENTORY.getId()) {
            return;
        }

        var items = event.getItemContainer().getItems();
        var itemIDs = Arrays.stream(items).map(Item::getId).collect(Collectors.toList());

        if (!inventoryItemIDs.equals(itemIDs)) {
            this.writer.write(TYPE, new InventoryChangedData(this.inventoryItemIDs, itemIDs));
            this.inventoryItemIDs = itemIDs;
        }
    }

    @Value
    private static class InventoryChangedData {
        List<Integer> oldInventory;
        List<Integer> newInventory;
    }
}
