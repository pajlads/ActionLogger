package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class VarTracker {
    private final static Set<Integer> IGNORED_VARBITS = Set.of(1, 2, 3);
    private static final Set<Integer> IGNORED_VARPS = Set.of(1, 2, 3);

    private final JsonWriter writer;
    private final Map<Integer, Integer> varbits = new HashMap<>();
    private final Map<Integer, Integer> varps = new HashMap<>();

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

    private void handleVarplayerChanged(VarbitChanged event, int id) {
        if (IGNORED_VARPS.contains(id)) {
            return;
        }

        var prevValue = varps.getOrDefault(id, 0);
        var value = event.getValue();
        varps.put(id, value);

        if (prevValue != value) {
            this.writer.write(VarplayerChangedData.TYPE, new VarplayerChangedData(id, prevValue, value));
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
            this.writer.write(VarbitChangedData.TYPE, new VarbitChangedData(id, prevValue, value));
        }
    }

    @Data
    static private class VarplayerChangedData {
        private static final String TYPE = "VARPLAYER_CHANGED";

        private final Integer id;
        private final Integer oldValue;
        private final Integer newValue;
    }

    @Data
    static private class VarbitChangedData {
        private static final String TYPE = "VARBIT_CHANGED";

        private final Integer id;
        private final Integer oldValue;
        private final Integer newValue;
    }
}
