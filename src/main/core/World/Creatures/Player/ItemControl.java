package core.World.Creatures.Player;

import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.assets.AssetsManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import static core.Application.log;
import static core.Global.assets;

public class ItemControl {
    public static Items[][] smallWorkbenchItems = new Items[10][20], mediumWorkbenchItems = new Items[10][20], largeWorkbenchItems = new Items[10][20], buildMenuItems = new Items[5][30];

    public static void create() {
        try (Stream<Path> stream = Files.walk(Paths.get(assets.assetsDir() + "/World/ItemsCharacteristics"))) {
            stream.filter(p -> p.toString().endsWith(".properties")).forEach(path -> {

                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(path)) {
                    props.load(is);

                    String createWith = props.getProperty("CreateWith");
                    String fileName = path.getParent().getFileName() + "/" + path.getFileName().toString().replace(".properties", "");

                    if (createWith == null) {
                        log.debug("null createWith " + fileName);
                        return;
                    }

                    switch (createWith) {
                        case "smallWorkbench" -> addItem(smallWorkbenchItems, Items.createItem(StaticWorldObjects.createStatic(fileName)));
                        case "mediumWorkbench" -> addItem(mediumWorkbenchItems, Items.createItem(fileName));
                        case "largeWorkbench" -> addItem(largeWorkbenchItems, Items.createItem(fileName));
                        case "player" -> put(fileName);
                    }

                } catch (IOException e) {
                    log.error(e);
                }
            });
        } catch (IOException e) {
            log.error(e);
        }
    }

    private static void put(String name) {
        if (name.startsWith("Blocks") || name.startsWith("Factories")) {
            addItem(buildMenuItems, Items.createItem(StaticWorldObjects.createStatic(AssetsManager.normalizePath(name))));
        } else if (name.startsWith("Weapons") || name.startsWith("Details") || name.startsWith("Tools")) {
            addItem(buildMenuItems, Items.createItem(AssetsManager.normalizePath(name)));
        }
    }

    private static void addItem(Items[][] grid, Items item) {
        for (int y = 0; y < grid[0].length; y++) {
            for (int x = 0; x < grid.length; x++) {
                if (grid[x][y] == null) {
                    grid[x][y] = item;
                    return;
                }
            }
        }
    }
}
