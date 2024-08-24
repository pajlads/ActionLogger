package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class VarTracker {
    private static final String VARB_TYPE = "VARBIT_CHANGED";
    private static final String VARP_TYPE = "VARPLAYER_CHANGED";

    private static final Set<Integer> IGNORED_VARBITS = Set.of(1, 2, 3);
    private static final Set<Integer> IGNORED_VARPS = Set.of(1, 2, 3);

    private final JsonWriter writer;
    private final Map<Integer, Integer> varbits = new HashMap<>();
    private final Map<Integer, Integer> varps = new HashMap<>();

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        var id = event.getVarbitId();

        if (id == -1) {
            // varp changed
            this.handleChange(event.getVarpId(), event.getValue(), IGNORED_VARPS, varps, VARP_TYPE);
        } else {
            // varbit changed
            this.handleChange(id, event.getValue(), IGNORED_VARBITS, varbits, VARB_TYPE);
        }
    }

    private void handleChange(int id, int value, Set<Integer> ignored, Map<Integer, Integer> map, String type) {
        if (ignored.contains(id)) {
            return;
        }

        var previous = map.put(id, value);
        var prevValue = previous != null ? previous : 0;

        if (prevValue != value) {
            this.writer.write(type, new VarChangedData(id, prevValue, value));
        }
    }

    @Value
    private static class VarChangedData {
        int id;
        int oldValue;
        int newValue;
    }
}
