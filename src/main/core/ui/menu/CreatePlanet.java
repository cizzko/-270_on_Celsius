package core.ui.menu;

import core.Constants;
import core.UIMenus;
import core.World.WorldGenerator.WorldGenerator;
import core.ui.Align;
import core.ui.LayoutGroup;
import core.ui.Styles;
import core.ui.Table;
import core.ui.widget.Button;
import core.ui.widget.Console;
import core.ui.widget.Image;
import core.ui.widget.Panel;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

import static core.Global.atlas;
import static core.ui.widget.Widgets.*;

public class CreatePlanet extends core.ui.widget.Dialog {
    private Image planet;

    private final Panel upperPanel;
    private final Button generateWorld;
    private final Table basicParameters, generationParameters;
    private final Console console;
    private final GenerationParameters parameters = new GenerationParameters();

    private final Button general, generation, physics;

    public CreatePlanet() {
        setFillParent(true);
        pad(20);

        cell(console = console("Console"), cell -> {
            cell.padTop(20);
            cell.colspan(2);
            cell.fixedY(200f);
            cell.fillX(1);
        });

        row();

        upperPanel = panel("UpperPanel", Styles.SIMPLE_PANEL);
        generateWorld = buttonLocalized("Generate world", Styles.SIMPLE_TEXT_BUTTON, () -> {
            WorldGenerator.generateWorld(parameters);
        });

        basicParameters = table("BasicParameters")
                .pad(20);
        basicParameters.defaultCell()
                .align(Align.TOP_LEFT);
        basicParameters.cell(toggleButtonLocalized("Experimental worl gen", Styles.DEFAULT_TOGGLE_BUTTON,
                WorldGenerator.useExpGen,
                () -> { WorldGenerator.useExpGen = !WorldGenerator.useExpGen; }),
                cell -> {
                    cell.align(Align.TOP_LEFT);
                    cell.expand();
                });

        toggleDialog(basicParameters, true);

        generationParameters = table("GenerationParameters")
                .pad(20);
        generationParameters.defaultCell()
                .align(Align.TOP_LEFT);

        toggleDialog(generationParameters, false);
        generationParameters.cell(
                toggleButtonLocalized("Generate flat world", Styles.DEFAULT_TOGGLE_BUTTON, parameters.simple,
                () -> parameters.simple = !parameters.simple),
                cell -> {
                    cell.align(Align.TOP_LEFT);
                    cell.expand();
                });

        general = buttonLocalized("Basic", Styles.SIMPLE_TEXT_BUTTON, this::basicBtn);
        general.isClicked = true;
        general.isClickable = false;
        generation = buttonLocalized("Generation", Styles.SIMPLE_TEXT_BUTTON, this::generationBtn);
        physics = buttonLocalized("Physics", Styles.SIMPLE_TEXT_BUTTON, this::physicsBtn);

        cell(panel("Background"), cell -> {
            cell.grow();

            cell.widget.cell(stack("ToggleList"), c -> {
                c.grow();

                c.widget.add(basicParameters);
                c.widget.add(generationParameters);
            });
            cell.widget.row();

            cell.widget.cell(upperPanel, c -> {
                c.align(Align.TOP);
                c.expandX();
                c.growX();
                c.prefHeight(85);

                c.widget.defaultCell()
                        .padBottom(20)
                        .fixed(240, 65);

                c.widget.cell(buttonLocalized("Return", Styles.SIMPLE_TEXT_BUTTON, b -> {
                    open(UIMenus.mainMenu());
                }));

                c.widget.cell(general, b -> b.padLeft(360));
                c.widget.defaultCell().padLeft(20);
                c.widget.cell(generation);
                c.widget.cell(physics);
            });
        });

        cell(panel("SizePanel"), p -> {
            p.padLeft(20);
            p.prefWidth(460);
            p.growY();
            p.fillY(1);

            p.widget.cell(generateWorld, b -> {
                b.grow();
                b.fixedY(65);
                b.widget.oneShot = true;
                b.align(Align.BOTTOM);
            });
            p.widget.row();
            p.widget.cell(slider("WorldSize", Styles.DEFAULT_SLIDER), sl -> {
                sl.fixedY(20);
                sl.growX();
                sl.padTop(255);
                sl.padBottom(20);
                sl.align(Align.BOTTOM);

                sl.widget.bounds(Constants.World.MIN_WORLD_SIZE, Constants.World.MAX_WORLD_SIZE);
                sl.widget.updater = (size, max) -> {
                    String pic;
                    if (size >= max / 1.5f) {
                        pic = "planetBig";
                    } else if (size >= max / 3f) {
                        pic = "planetAverage";
                    } else {
                        pic = "planetMini";
                    }
                    planet.drawable(atlas.get("World/WorldGenerator/" + pic));

                    parameters.sizeX = size;
                    parameters.sizeY = size;
                };
            });
            p.widget.row();
            p.widget.cell(stack("PlanetBackground"), c -> {
                c.widget.add(atlasImage("World/WorldGenerator/skyBackgroundPlanet"));
                c.widget.add(planet = atlasImage("World/WorldGenerator/planetMini"));

                c.align(Align.TOP);
                c.growX();
            });
        });
    }

    private void toggleDialog(LayoutGroup<?> dialog, boolean state) {
        dialog.setVisible(state);
        dialog.setTouchable(state);
        dialog.setTouchableChildren(state);
    }

    private void physicsBtn() {
        toggleDialog(basicParameters, false);
        toggleDialog(generationParameters, false);

        physics.isClickable = false;
        general.reset();
        generation.reset();
    }

    private void basicBtn() {
        toggleDialog(basicParameters, true);
        toggleDialog(generationParameters, false);

        general.isClickable = false;
        physics.reset();
        generation.reset();
    }

    private void generationBtn() {
        toggleDialog(basicParameters, false);
        toggleDialog(generationParameters, true);

        generation.isClickable = false;
        physics.reset();
        general.reset();
    }

    public void appendText(String text) {
        console.add(text);
    }

    public void reset() {
        console.clearHistory();
        generateWorld.reset();

        general.isClicked = true;
        general.isClickable = false;

        for (var child : upperPanel.children()) {
            if (child instanceof Button b) {
                b.reset();
            }
        }
    }

    public static class GenerationParameters {
        public boolean simple;
        public int sizeX = Constants.World.MIN_WORLD_SIZE;
        public int sizeY = Constants.World.MIN_WORLD_SIZE;
        public long seed = ThreadLocalRandom.current().nextLong();
        public @Nullable String name, description;
    }
}
