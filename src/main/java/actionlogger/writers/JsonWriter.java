package actionlogger.writers;

import actionlogger.Utils;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
public class JsonWriter implements Closeable {
    private final Gson gson;
    private final Client client;
    private final ExecutorService executor;
    private final File dir;
    @Getter
    private @Nullable Path path = null;
    private @Nullable BufferedWriter fh = null;

    public JsonWriter(Gson gson, Client client, ExecutorService executor) {
        this.gson = gson;
        this.client = client;
        this.executor = executor;

        dir = new File(RuneLite.RUNELITE_DIR, "action-logger");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();

        this.restartFile();
    }

    public void restartFile() {
        if (this.fh != null) {
            this.close();
        }

        this.path = dir.toPath().resolve(String.format("%d-logs.txt", System.currentTimeMillis()));
        try {
            this.fh = Files.newBufferedWriter(this.path);
        } catch (IOException e) {
            log.warn("Could not create file at {}", this.path);
        }
    }

    public void write(@Nonnull String type, @Nonnull Object data) {
        if (this.fh == null) {
            return;
        }
        var payload = new Payload(this.client.getTickCount(), Utils.getTimestamp(), type, data);
        var currentFh = this.fh;
        executor.execute(() -> {
            try {
                currentFh.write(gson.toJson(payload));
                currentFh.newLine();
                currentFh.flush();
            } catch (IOException e) {
                log.warn("Failed to write ActionLogger data: {}", e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        if (this.fh == null) {
            return;
        }

        var currentFh = this.fh;
        var currentPath = this.path;

        this.fh = null;

        executor.execute(() -> {
            try {
                currentFh.close();
            } catch (IOException e) {
                log.warn("Failed to close file at {}", currentPath);
            }
        });
    }

    @Value
    private static class Payload {
        int tick;
        String ts;
        String type;
        Object data;
    }
}
