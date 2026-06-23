package core;

import core.util.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static core.Application.*;
import static core.Global.*;

public final class GameSettings {
    public static final boolean DEFAULT_VERTICAL_SYNC = true;
    public static final int DEFAULT_TARGET_FPS        = -1;
    public static final String DEFAULT_LANGUAGE       = "en";

    public boolean verticalSync;
    public int targetFps; // -1 - uncapped
    public Render render;
    public String language;

    public static final class Render {
        public static final boolean DEFAULT_DSA               = true;
        public static final boolean DEFAULT_BINDLESS_TEXTURES = true;
        public static final boolean DEFAULT_BATCH_TILE_RENDER = false;

        public boolean dsa;
        public boolean bindlessTextures;
        public boolean batchTileRender;

        public Render() {
            this(DEFAULT_DSA, DEFAULT_BINDLESS_TEXTURES, DEFAULT_BATCH_TILE_RENDER);
        }

        public Render(boolean dsa, boolean bindlessTextures, boolean batchTileRender) {
            this.dsa = dsa;
            this.bindlessTextures = bindlessTextures;
            this.batchTileRender = batchTileRender;
        }
    }

    public GameSettings() {
        this(DEFAULT_VERTICAL_SYNC, DEFAULT_TARGET_FPS, new Render(), DEFAULT_LANGUAGE);
    }

    public GameSettings(boolean verticalSync, int targetFps, Render render, String language) {
        this.verticalSync = verticalSync;
        this.targetFps = targetFps;
        this.render = render;
        this.language = language;
    }

    public void set(GameSettings current) {
        verticalSync = current.verticalSync;
        targetFps = current.targetFps;
        language = current.language;
    }

    public void save() {
        Path worDir = assets.workingDir();

        var file = worDir.resolve("settings.json");
        var backup = worDir.resolve("settings.json.bak");
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save backup file for settings '{}' to '{}'", file, backup, e);
            return;
        }

        try (var os = Files.newOutputStream(file)) {
            Config.json.writerWithDefaultPrettyPrinter()
                    .writeValue(os, this);
        } catch (IOException e) {
            log.error("Failed to serialize settings to file '{}'. Restoring backup...", file, e);
            try {
                Files.move(backup, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                log.error("Failed to restore backup file '{}' :(", backup, e);
            }
            // TODO удалять?????
            return;
        }
    }
}
