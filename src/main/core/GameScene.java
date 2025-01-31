package core;

import static core.Global.uiScene;
import static core.Global.scheduler;
import static core.util.DebugTools.rethrow;

// У этой магии есть такие свойства:
//  Данный объект сам по себе является ресурсом и проходит цикл загрузки с вызовом методов AssetLifecycle
//  Это значит, что поля с аннотацией @Load добавляются в ресурсы
//  В то же время, поля типа GameObject рекурсивно добавляются в ресурсы с учётом
//  конфигурации (владение, тип загрузки и т.п.)
public abstract class GameScene implements AssetLifecycle {
    private static int LOADER_ID = 1;
    private static int nextId() {
        return LOADER_ID++;
    }

    protected final AutoLoader objectLoader = new AutoLoader(getClass().getSimpleName() + ".loader." + nextId());

    protected State state = State.LOADING;

    // Забирает владение над объектом. После смены сцены ресурсы этого объекта будут выгружены
    public final void addObject(Object object) {
        objectLoader.add(object);
    }

    public final void addPreload(Object object) {
        objectLoader.addPreload(object);
    }

    public final void onPreloadCompletion(Runnable act) {
        objectLoader.onPreloadCompletion(act);
    }

    public final void init() {
        addObject(this);
        objectLoader.loadSync();
        onInit();
    }

    protected void onInit() {

    }

    public final void loop() {
        switch (state) {
            case LOADING -> {
                boolean loaded;
                try {
                    loaded = objectLoader.update();
                } catch (Exception e) {
                    state = State.FAILED;
                    unload();
                    rethrow(e);
                    return;
                }

                if (loaded) {
                    state = State.LOADED;
                    readyLoop();
                } else {
                    scheduler.executeAll();
                    uiScene.update();
                    drawLoading();
                }
            }
            case LOADED -> readyLoop();
        }
    }

    private void readyLoop() {
        scheduler.executeAll();
        objectLoader.updatePreload();
        uiScene.update();
        inputUpdate();
        update();
        draw();
    }

    @Override
    public void onLoaded() {}
    @Override
    public void onUnloaded() {}

    protected abstract void inputUpdate();
    protected abstract void update();

    // Рендер готовой сцены
    protected abstract void draw();
    // Рендер при загрузке сцены
    protected abstract void drawLoading();

    public final void unload() {
        objectLoader.unload();
    }

    void onTransition(GameScene prevScene) {
        objectLoader.onTransition(prevScene.objectLoader);
    }

    protected enum State {
        LOADING,
        FAILED,
        LOADED
    }
}
