package actionlogger;

import actionlogger.trackers.DialogueTracker;
import actionlogger.trackers.VarTracker;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
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

    private DialogueTracker dialogueTracker = null;
    private VarTracker varTracker = null;
    private FileOutputStream fh = null;

    @Override
    protected void startUp() throws FileNotFoundException {
        var dir = new File(RuneLite.RUNELITE_DIR, "action-logger");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();

        // var path = Path.of(dir.getPath()).resolve(String.format("%d-logs.txt", System.currentTimeMillis())).toString();
        var path = Path.of(dir.getPath()).resolve("logs.txt").toString();
        fh = new FileOutputStream(path);

        dialogueTracker = new DialogueTracker(fh, client);
        eventBus.register(dialogueTracker);
        keyManager.registerKeyListener(dialogueTracker);

        varTracker = new VarTracker(fh, client);
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

        if (fh != null) {
            fh.flush();
            fh.close();
            fh = null;
        }

        log.debug("Shut down Action Logger");
    }

    @Provides
    ActionLoggerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ActionLoggerConfig.class);
    }
}
