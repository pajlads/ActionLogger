package actionlogger;

import actionlogger.trackers.DialogueTracker;
import actionlogger.trackers.VarTracker;
import actionlogger.writers.JsonWriter;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@PluginDescriptor(name = "Action Logger", description = "Log user & server actions to disk", tags = {"actionlogger"})
public class ActionLoggerPlugin extends Plugin {
    private @Inject KeyManager keyManager;
    private @Inject EventBus eventBus;
    private @Inject Client client;
    private @Inject Gson gson;

    private DialogueTracker dialogueTracker = null;
    private VarTracker varTracker = null;
    private FileOutputStream fh = null;
    private JsonWriter writer = null;

    @Override
    protected void startUp() throws FileNotFoundException {
        var dir = new File(RuneLite.RUNELITE_DIR, "action-logger");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();

        var path = Path.of(dir.getPath()).resolve(String.format("%d-logs.txt", System.currentTimeMillis())).toString();
        fh = new FileOutputStream(path);

        writer = new JsonWriter(gson, client, fh);

        dialogueTracker = new DialogueTracker(writer, client);
        eventBus.register(dialogueTracker);
        keyManager.registerKeyListener(dialogueTracker);

        varTracker = new VarTracker(writer);
        eventBus.register(varTracker);

        log.debug("Started up Action Logger");
    }

    @Override
    protected void shutDown() throws IOException {
        keyManager.unregisterKeyListener(dialogueTracker);
        eventBus.unregister(dialogueTracker);
        dialogueTracker = null;

        eventBus.unregister(varTracker);
        varTracker = null;

        writer = null;

        if (fh != null) {
            fh.flush();
            fh.close();
            fh = null;
        }

        log.debug("Shut down Action Logger");
    }
}
