package actionlogger.writers;

import actionlogger.Utils;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JsonWriter {
    private final Gson gson;
    private final Client client;
    private final FileOutputStream fh;

    public void write(@Nonnull String type, @Nonnull Object data) {
        try {
            var payload = new Payload(this.client.getTickCount(), Utils.getTimestamp(), type, data);
            fh.write(gson.toJson(payload).getBytes());
            fh.write('\n');
        } catch (IOException e) {
            log.warn("Failed to write DialogueTracker data: {}", e.getMessage());
        }
    }

    @RequiredArgsConstructor
    static private class Payload {
        private final Integer tickCount;
        private final String timestamp;
        private final String type;
        private final Object data;
    }
}
