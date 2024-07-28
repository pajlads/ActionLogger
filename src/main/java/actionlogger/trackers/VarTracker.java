package actionlogger.trackers;

import actionlogger.Utils;
import ch.qos.logback.core.util.COWArrayList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class VarTracker {
    private final static Set<Integer> IGNORED_VARBITS = Set.of(1, 2, 3);
    private static final Set<Integer> IGNORED_VARPS = Set.of(1, 2, 3);

    private final FileOutputStream fh;
    private final Map<Integer, Integer> varbits = new HashMap<>();
    private final Map<Integer, Integer> varps = new HashMap<>();
    private final Client client;

    public VarTracker(FileOutputStream fh, Client client) {
        this.fh = fh;
        this.client = client;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        var id = event.getVarbitId();

        if (id == -1) {
            // varp changed
            this.handleVarplayerChanged(event, event.getVarpId());
        } else {
            // varbit changed
            this.handleVarbitChanged(event, id);
        }
    }

    private void write(String body) {
        if (fh == null) {
            return;
        }

        try {
            fh.write(String.format("[%d, %s] %s\n", client.getTickCount(), Utils.getTimestamp(), body).getBytes());
        } catch (IOException e) {
            log.warn("Failed to write VarTracker data: {}", e.getMessage());
        }
    }

    private void handleVarplayerChanged(VarbitChanged event, int id) {
        if (IGNORED_VARPS.contains(id)) {
            return;
        }

        var prevValue = varps.getOrDefault(id, 0);
        var value = event.getValue();
        varps.put(id, value);

        if (prevValue != value) {
            this.write(String.format("varp %d changed %d -> %d", id, prevValue, value));
        }
    }

    private void handleVarbitChanged(VarbitChanged event, int id) {
        if (IGNORED_VARBITS.contains(id)) {
            return;
        }

        var prevValue = varbits.getOrDefault(id, 0);
        var value = event.getValue();
        varbits.put(id, value);

        if (prevValue != value) {
            this.write(String.format("varbit %d changed %d -> %d", id, prevValue, value));
        }
    }
}
