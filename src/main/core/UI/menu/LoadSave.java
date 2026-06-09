package core.UI.menu;

import core.Application;
import core.Constants;
import core.Global;
import core.UI.*;
import core.UIMenus;
import core.World.WorldGenerator.WorldGenerator;

import java.io.IOException;
import java.nio.file.Files;

import static core.Global.assets;
import static core.Global.atlas;

public final class LoadSave extends Dialog {
    private final ImageElement planet;
    private final Button loadWorld;
    private final Panel upperPanel;
    private final CreatePlanet.GenerationParameters parameters = new CreatePlanet.GenerationParameters();
    private final TextArea consoleBox;

    public LoadSave() {
        var console = addPanel(Styles.DEFAULT_PANEL, 20, 20, 1880, 200)
                .setId("console");

        consoleBox = console.append(gr -> new TextArea(gr, Styles.DEFAULT_TEXT))
                .set(40, 220, 1860, 200);

        var background = addPanel(Styles.DEFAULT_PANEL, 20, 240, 1400, 820)
                .setId("background");
        var sizePanel = addPanel(Styles.DEFAULT_PANEL, 1440, 240, 460, 820)
                .setId("sizePanel");
        sizePanel.addImage(1460, 620, atlas.get("World/WorldGenerator/skyBackgroundPlanet"));
        planet = sizePanel.addImage(1510, 670, atlas.get("World/WorldGenerator/planetMini"));
        // Панель с вкладками
        upperPanel = background.addPanel(Styles.SIMPLE_PANEL, 40, 955, 1360, 85);

        var list = new ToggleList()
                .setOnlyOne(true);

        var dir = assets.workingDir();
        try (var dirstr = Files.newDirectoryStream(dir, "*.json")) {
            int k = 1;
            for (var path : dirstr) {
                String fileName = path.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex);
                }

                //todo последний сохраненный в самом верху спика
                //todo и отделен полоской от остальных
                int lineY = 955 - 80 * k;
                var panel = new Panel(null, Styles.DUMMY_PANEL)
                        .setId(fileName)
                        .set(40 + 20 - 10, lineY, 600, 60);
                var image = panel.addImage(28, lineY + 15, atlas.get("World/white"))
                        .setColor(Styles.DEFAULT_BRIGHT_ORANGE)
                        .setSize(6, 60/2f)
                        .setVisible(false);
                panel.add(new TextArea(null, Styles.DEFAULT_TEXT))
                        .set(40 + 40, lineY + 20, 600, 60)
                        .setText(fileName);
                list.add(panel, image::setVisible);
                k++;
            }
        } catch (IOException e) {
            Application.log.error("", e);
        }

        background.add(list);

        upperPanel.addButton(Styles.SIMPLE_TEXT_BUTTON, b -> {
                    hide();
                    UIMenus.mainMenu().show();
                })
                .set(40, 975, 240, 65)
                .setName(Global.lang.get("Return"));

        loadWorld = sizePanel.addButton(Styles.SIMPLE_TEXT_BUTTON, () -> WorldGenerator.generateWorld(parameters))
                .set(1460, 260, 420, 65)
                .setName(Global.lang.get("Load world"))
                .setOneShot(true);

        //todo тут подгружать метадату размера
        sizePanel.addSlider(Styles.DEFAULT_SLIDER, Constants.World.MIN_WORLD_SIZE, Constants.World.MAX_WORLD_SIZE, (size, max) -> {
            String pic;
            if (size >= max / 1.5f) {
                pic = "planetBig";
            } else if (size >= max / 3f) {
                pic = "planetAverage";
            } else {
                pic = "planetMini";
            }
            planet.setImage(atlas.get("World/WorldGenerator/" + pic));
            parameters.sizeX = size;
            parameters.sizeY = size;
        }).set(1460, 340, 420, 20);
        //todo и не забыть сделать скроллабле список-панель
    }

    private void toggleDialog(Dialog dialog, boolean state) {
        dialog.setVisible(state);
        dialog.setTouchable(state);
        dialog.setTouchableChildren(state);
    }
    public void appendText(String text) {
        String prev = consoleBox.text == null ? "" : consoleBox.text;
        consoleBox.setText(prev + '\n' + text);
    }

    public void reset() {
        consoleBox.setText("");
        for (Element child : upperPanel.children()) {
            if (child instanceof Button b) {
                b.isClickable = true;
                b.isClicked = false;
            }
        }
    }

    public static class GenerationParameters {

    }
}
