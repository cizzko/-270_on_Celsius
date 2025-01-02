package core.ui.GUI.Menu;

import core.UI;
import core.Utils.SimpleColor;
import core.World.WorldGenerator;
import core.ui.Dialog;
import core.ui.ImageElement;
import core.ui.Styles;

import static core.EventHandling.Logging.Json.getName;
import static core.Global.atlas;

public class CreatePlanet extends Dialog {
    private final ImageElement planet;
    public final GenerationParameters parameters = new GenerationParameters();
    public final Dialog basicParameters, generationParameters;

    public CreatePlanet() {
        addPanel(20, 20, 1880, 200);
        addPanel(20, 240, 1400, 820);
        addPanel(1440, 240, 460, 820);
        addPanel(40, 955, 1360, 85)
                .setSimple(true);
        addImage(1460, 620, atlas.byPath("World/WorldGenerator/skyBackgroundPlanet.png"));
        planet = addImage(1510, 670, atlas.byPath("World/WorldGenerator/planetMini.png"));

        addButton(this::returnBtn)
                .set(40, 975, 240, 65)
                .setName(getName("Return"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        addButton(this::basicBtn)
                .set(640, 975, 240, 65)
                .setName(getName("Basic"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        addButton(this::generationBtn)
                .set(900, 975, 240, 65)
                .setName(getName("Generation"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        addButton(this::physicsBtn)
                .set(1160, 975, 240, 65)
                .setName(getName("Physics"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        addButton(() -> WorldGenerator.generateWorld(parameters))
                .set(1460, 260, 420, 65)
                .setName(getName("GenerateWorld"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        addSlider(2500, (x, max) -> {
            String pic;
            if (x >= max / 1.5f) {
                pic = "planetBig.png";
            } else if (x >= max / 3f) {
                pic = "planetAverage.png";
            } else {
                pic = "planetMini.png";
            }
            planet.setImage(atlas.byPath("World/WorldGenerator/" + pic));
            parameters.size = x + 20;
        })
                .set(1460, 340, 420, 20)
                .setSliderColor(Styles.DEFAULT_PANEL_COLOR)
                .setDotColor(SimpleColor.DEFAULT_ORANGE);
        basicParameters = add(new Dialog() {{
            visible = true;
            addToggleButton(() -> parameters.creatures = !parameters.creatures)
                    .set(70, 890, 44, 44)
                    .setName(getName("GenerateCreatures"))
                    .setColor(SimpleColor.DIRTY_WHITE);
            addToggleButton(() -> parameters.randomSpawn = !parameters.randomSpawn)
                    .set(70, 820, 44, 44)
                    .setName(getName("RandomSpawn"))
                    .setColor(SimpleColor.DIRTY_WHITE);
        }});
        generationParameters = add(new Dialog() {{
            visible = false;
            addToggleButton(() -> parameters.simple = !parameters.simple)
                    .set(70, 890, 44, 44)
                    .setName(getName("GenerateSimpleWorld"))
                    .setColor(SimpleColor.DIRTY_WHITE);
        }});
    }

    private void returnBtn() {
        hide();
        UI.mainMenu().show();
    }

    private void basicBtn() {
        generationParameters.setVisible(false);
        basicParameters.setVisible(true);
    }

    private void generationBtn() {
        basicParameters.setVisible(false);
        generationParameters.setVisible(true);
    }

    private void physicsBtn() {
    }

    public static class GenerationParameters {
        public boolean randomSpawn;
        public boolean creatures;
        public boolean simple;
        public int size = 20; // TODO помню про планы убрать ограничение генерации на размеры
    }
}
