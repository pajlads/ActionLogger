package actionlogger;

import actionlogger.trackers.DialogueTracker;
import actionlogger.trackers.InventoryTracker;
import actionlogger.trackers.VarTracker;
import actionlogger.writers.JsonWriter;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(name = "Action Logger", description = "Log user & server actions to disk", tags = {"actionlogger"})
public class ActionLoggerPlugin extends Plugin {
    private @Inject KeyManager keyManager;
    private @Inject EventBus eventBus;
    private @Inject Client client;
    private @Inject Gson gson;
    private @Inject ScheduledExecutorService executor;

    private DialogueTracker dialogueTracker = null;
    private VarTracker varTracker = null;
    private InventoryTracker inventoryTracker = null;
    private JsonWriter writer = null;

    @Override
    protected void startUp() throws IOException {
        writer = new JsonWriter(gson, client, executor);

        dialogueTracker = new DialogueTracker(writer, client);
        eventBus.register(dialogueTracker);
        keyManager.registerKeyListener(dialogueTracker);

        varTracker = new VarTracker(writer);
        eventBus.register(varTracker);

        inventoryTracker = new InventoryTracker(writer);
        eventBus.register(inventoryTracker);

        log.debug("Started up Action Logger");
    }

    @Override
    protected void shutDown() throws IOException {
        keyManager.unregisterKeyListener(dialogueTracker);
        eventBus.unregister(dialogueTracker);
        dialogueTracker = null;

        eventBus.unregister(varTracker);
        varTracker = null;

        eventBus.unregister(inventoryTracker);
        inventoryTracker = null;

        writer.close();
        writer = null;

        log.debug("Shut down Action Logger");
    }

    @Subscribe
    protected void onClientShutdown(ClientShutdown event) {
        try {
            writer.close();
        } catch (Exception e) {
            log.warn("Failed to close writer", e);
        }
    }
}
