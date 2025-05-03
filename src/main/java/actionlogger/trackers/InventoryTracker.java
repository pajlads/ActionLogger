package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class InventoryTracker {
    private static final String TYPE = "INVENTORY_CHANGED";
    private static final int INVENTORY_SIZE = 28;

    private final JsonWriter writer;

    private int[] inventoryItemIDs = filled(INVENTORY_SIZE, -1);
    private int[] inventoryQuantities = filled(INVENTORY_SIZE, 0);

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }

        var items = event.getItemContainer().getItems();
        var itemIDs = Arrays.stream(items).mapToInt(Item::getId).toArray();
        var itemQuantities = Arrays.stream(items).mapToInt(Item::getQuantity).toArray();

        if (!Arrays.equals(inventoryItemIDs, itemIDs) || !Arrays.equals(inventoryQuantities, itemQuantities)) {
            var data = new InventoryChangedData(
                this.inventoryItemIDs,
                this.inventoryQuantities,
                itemIDs,
                itemQuantities
            );
            this.writer.write(TYPE, data);
            this.inventoryItemIDs = itemIDs;
            this.inventoryQuantities = itemQuantities;
        }
    }

    private static int[] filled(int size, int value) {
        final int[] arr = new int[size];
        Arrays.fill(arr, value);
        return arr;
    }

    @Value
    private static class InventoryChangedData {
        int[] oldInventory;
        int[] oldQuantities;
        int[] newInventory;
        int[] newQuantities;
    }
}
