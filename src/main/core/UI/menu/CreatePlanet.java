package core.UI.menu;

import core.Constants;
import core.Global;
import core.UI.*;
import core.UIMenus;
import core.World.WorldGenerator.WorldGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

import static core.Global.atlas;

public class CreatePlanet extends Dialog {
    private final ImageElement planet;
    private final Button generateWorld;
    private final Panel upperPanel;
    private final GenerationParameters parameters = new GenerationParameters();
    private final Dialog basicParameters, generationParameters;
    private final TextArea consoleBox;

    public CreatePlanet() {
        var console = addPanel(Styles.DEFAULT_PANEL, 20, 20, 1880, 200);
        consoleBox = console.append(gr -> new TextArea(gr, Styles.DEFAULT_TEXT))
                .set(40, 220, 1860, 200);

        var background = addPanel(Styles.DEFAULT_PANEL, 20, 240, 1400, 820);
        var sizePanel = addPanel(Styles.DEFAULT_PANEL, 1440, 240, 460, 820);
        sizePanel.addImage(1460, 620, atlas.get("World/WorldGenerator/skyBackgroundPlanet"));
        planet = sizePanel.addImage(1510, 670, atlas.get("World/WorldGenerator/planetMini"));
        // Панель с вкладками
        upperPanel = background.addPanel(Styles.SIMPLE_PANEL, 40, 955, 1360, 85);

        upperPanel.addButton(Styles.SIMPLE_TEXT_BUTTON, b -> {
            hide();
            UIMenus.mainMenu().show();
        })
        .set(40, 975, 240, 65)
        .setName(Global.lang.get("Return"));

        Panel.oneOf(
            // Поскольку сделать что-то с ресивером нельзя, то приходится страдать и тут указывать `upperPanel.`
            upperPanel.addButton(Styles.SIMPLE_TEXT_BUTTON, this::basicBtn)
                    .set(640, 975, 240, 65)
                    .setName(Global.lang.get("Basic")),
            upperPanel.addButton(Styles.SIMPLE_TEXT_BUTTON, this::generationBtn)
                    .set(900, 975, 240, 65)
                    .setName(Global.lang.get("Generation")),
            upperPanel.addButton(Styles.SIMPLE_TEXT_BUTTON, this::physicsBtn)
                    .set(1160, 975, 240, 65)
                    .setName(Global.lang.get("Physics"))
        );
        generateWorld = sizePanel.addButton(Styles.SIMPLE_TEXT_BUTTON, () -> WorldGenerator.generateWorld(parameters))
                .set(1460, 260, 420, 65)
                .setName(Global.lang.get("GenerateWorld"))
                .setOneShot(true);
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

        basicParameters = background.add(new Dialog() {{
            setId("basicParameters");
            toggleDialog(this, true);
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> parameters.creatures = !parameters.creatures)
                    .setPosition(70, 890)
                    .setName(Global.lang.get("GenerateCreatures"));
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> parameters.randomSpawn = !parameters.randomSpawn)
                    .setPosition(70, 820)
                    .setName(Global.lang.get("RandomSpawn"));
        }}).set(40, 270, 1360, 950 - 270);
        generationParameters = background.add(new Dialog() {{
            setId("generationParameters");
            toggleDialog(this, false);
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> parameters.simple = !parameters.simple)
                    .setPosition(70, 890)
                    .setName(Global.lang.get("GenerateSimpleWorld"));
        }}).set(40, 270, 1360, 950 - 270);;
    }

    private void toggleDialog(Dialog dialog, boolean state) {
        dialog.setVisible(state);
        dialog.setTouchable(state);
        dialog.setTouchableChildren(state);
    }

    private void physicsBtn() {
        toggleDialog(basicParameters, false);
        toggleDialog(generationParameters, false);
    }

    private void basicBtn(Button b) {
        toggleDialog(basicParameters, true);
        toggleDialog(generationParameters, false);
    }

    private void generationBtn(Button b) {
        toggleDialog(basicParameters, false);
        toggleDialog(generationParameters, true);
    }

    public void appendText(String text) {
        String prev = consoleBox.text == null ? "" : consoleBox.text;
        consoleBox.setText(prev + '\n' + text);
    }

    public void reset() {
        consoleBox.setText("");
        generateWorld.isClickable = true;
        for (Element child : upperPanel.children()) {
            if (child instanceof Button b) {
                b.isClickable = true;
                b.isClicked = false;
            }
        }
    }

    public static class GenerationParameters {
        public boolean randomSpawn;
        public boolean creatures;
        public boolean simple;
        public int sizeX = Constants.World.MIN_WORLD_SIZE;
        public int sizeY = Constants.World.MIN_WORLD_SIZE;
        public long seed = ThreadLocalRandom.current().nextLong();
        public @Nullable String name, description;
    }
}
