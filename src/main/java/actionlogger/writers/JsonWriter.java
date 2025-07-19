package actionlogger.writers;

import actionlogger.Utils;
import com.google.gson.Gson;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class JsonWriter implements Closeable {
    private final Gson gson;
    private final Client client;
    private final ExecutorService executor;
    private final File dir;
    private Path path = null;
    private BufferedWriter fh = null;

    public JsonWriter(Gson gson, Client client, ExecutorService executor) {
        this.gson = gson;
        this.client = client;
        this.executor = executor;

        dir = new File(RuneLite.RUNELITE_DIR, "action-logger");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();

        this.restartFile();
    }

    public CompletableFuture<Map.Entry<Path, Path>> restartFile() {
        CompletableFuture<Map.Entry<Path, Path>> future = new CompletableFuture<>();
        executor.execute(() -> {
            var oldFh = this.fh;
            var oldPath = this.path;
            var path = dir.toPath().resolve(String.format("%d-logs.txt", System.currentTimeMillis()));
            try {
                this.fh = Files.newBufferedWriter(path);
                this.path = path;

                if (oldFh != null) {
                    try {
                        oldFh.close();
                    } catch (IOException e) {
                        log.warn("Failed to close old writer for {}", oldPath, e);
                    }
                }

                future.complete(Pair.of(oldPath, path));
            } catch (IOException e) {
                log.warn("Could not create file at {}", path, e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void write(@Nonnull String type, @Nonnull Object data) {
        var payload = new Payload(this.client.getTickCount(), Utils.getTimestamp(), type, data);
        executor.execute(() -> {
            var currentFh = this.fh;
            if (currentFh == null) {
                log.debug("Skipping write due to closed resource: {}", payload);
                return;
            }
            try {
                currentFh.write(gson.toJson(payload));
                currentFh.newLine();
                currentFh.flush();
            } catch (IOException e) {
                log.warn("Failed to write ActionLogger data", e);
            }
        });
    }

    @Override
    public void close() {
        executor.execute(() -> {
            var currentFh = this.fh;
            if (currentFh == null) {
                return;
            }
            var currentPath = this.path;

            try {
                currentFh.close();
                this.fh = null;
                this.path = null;
            } catch (IOException e) {
                log.warn("Failed to close file at {}", currentPath, e);
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
