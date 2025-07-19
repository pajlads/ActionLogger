package actionlogger;

import actionlogger.trackers.DialogueTracker;
import actionlogger.trackers.InventoryTracker;
import actionlogger.trackers.VarTracker;
import actionlogger.writers.JsonWriter;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(name = "Action Logger", description = "Log user & server actions to disk", tags = {"actionlogger"})
public class ActionLoggerPlugin extends Plugin {
    private static final String USAGE = "Usage: ::ActionLogger <COMMAND>. Available commands: restart, dump";

    private @Inject KeyManager keyManager;
    private @Inject EventBus eventBus;
    private @Inject Client client;
    private @Inject Gson gson;
    private @Inject ScheduledExecutorService executor;
    private @Inject ChatMessageManager chatManager;
    private @Inject ItemManager itemManager;

    private DialogueTracker dialogueTracker = null;
    private VarTracker varTracker = null;
    private InventoryTracker inventoryTracker = null;
    private JsonWriter writer = null;

    @Override
    protected void startUp() {
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
    protected void shutDown() {
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
    public void onCommandExecuted(CommandExecuted event) {
        var cmd = event.getCommand();
        var args = event.getArguments();
        if ("ActionLogger".equalsIgnoreCase(cmd) || "ActLog".equalsIgnoreCase(cmd)) {
            if (args == null || args.length == 0) {
                this.addChatMessage(USAGE);
                return;
            }

            switch (args[0].toLowerCase()) {
                case "restart":
                    this.writer.restartFile()
                        .thenAccept(result -> {
                            var oldPath = result.getKey();
                            var newPath = result.getValue();
                            this.addChatMessage(String.format("Closed file at %s, now writing to %s", oldPath, newPath));
                        })
                        .exceptionally(e -> {
                            this.addChatMessage("Failed to rotate files; try again later or report to our issue tracker");
                            return null;
                        });
                    break;

                case "dump":
                    Dump.handleDump(this, this.client, this.writer, this.itemManager, args);
                    break;

                default:
                    this.addChatMessage(String.format("Unknown command %s", args[0]));
                    this.addChatMessage(USAGE);
                    break;
            }
        }
    }

    @Subscribe
    protected void onClientShutdown(ClientShutdown event) {
        try {
            writer.close();
        } catch (Exception e) {
            log.warn("Failed to close writer", e);
        }
    }

    void addChatMessage(@Nonnull String message) {
        String formatted = String.format("[ActionLogger] %s", message);

        chatManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.CONSOLE)
            .runeLiteFormattedMessage(formatted)
            .build()
        );
    }
}
