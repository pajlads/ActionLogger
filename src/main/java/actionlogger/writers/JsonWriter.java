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
    @Nullable
    private volatile Path path = null;
    private volatile BufferedWriter fh = null;
    private volatile boolean writing = false;

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
        var oldFh = this.fh;
        var path = dir.toPath().resolve(String.format("%d-logs.txt", System.currentTimeMillis()));
        try {
            this.fh = Files.newBufferedWriter(path);
            this.path = path;

            if (oldFh != null) {
                oldFh.close();
            }
        } catch (IOException e) {
            log.warn("Could not cleanly create file at {}", path, e);
        }
    }

    public void write(@Nonnull String type, @Nonnull Object data) {
        if (this.fh == null) {
            return;
        }
        var payload = new Payload(this.client.getTickCount(), Utils.getTimestamp(), type, data);
        executor.execute(() -> {
            this.writing = true;
            try {
                var fh = this.fh;
                fh.write(gson.toJson(payload));
                fh.newLine();
                fh.flush();
            } catch (IOException e) {
                log.warn("Failed to write ActionLogger data", e);
            } finally {
                this.writing = false;
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
        this.path = null;

        while (this.writing) {
            Thread.onSpinWait(); // busy wait for existing write operation to complete before closing writer
        }

        try {
            currentFh.close();
        } catch (IOException e) {
            log.warn("Failed to close file at {}", currentPath, e);
        }
    }

    @Value
    private static class Payload {
        int tick;
        String ts;
        String type;
        Object data;
    }
}
