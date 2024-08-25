package actionlogger.writers;

import actionlogger.Utils;
import com.google.gson.Gson;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;

@Slf4j
public class JsonWriter implements Closeable {
    private final Gson gson;
    private final Client client;
    private final ExecutorService executor;
    private final BufferedWriter fh;

    public JsonWriter(Gson gson, Client client, ExecutorService executor) throws IOException {
        this.gson = gson;
        this.client = client;
        this.executor = executor;

        var dir = new File(RuneLite.RUNELITE_DIR, "action-logger");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();

        var path = dir.toPath().resolve(String.format("%d-logs.txt", System.currentTimeMillis()));
        this.fh = Files.newBufferedWriter(path);
    }

    public void write(@Nonnull String type, @Nonnull Object data) {
        var payload = new Payload(this.client.getTickCount(), Utils.getTimestamp(), type, data);
        executor.execute(() -> {
            try {
                fh.write(gson.toJson(payload));
                fh.newLine();
                fh.flush();
            } catch (IOException e) {
                log.warn("Failed to write DialogueTracker data: {}", e.getMessage());
            }
        });
    }

    @Override
    public void close() throws IOException {
        fh.close();
    }

    @Value
    private static class Payload {
        int tick;
        String ts;
        String type;
        Object data;
    }
}
