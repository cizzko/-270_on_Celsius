package core;

import core.util.DebugTools;
import core.assets.AssetsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.concurrent.Future;

public final class AutoLoader {
    private static final Logger log = LogManager.getLogger("AutoLoader");

    private static final MethodHandles.Lookup theLookup = MethodHandles.lookup();

    private final String name;

    private final IdentityHashMap<Class<?>, TypeInfo> bindingsByType = new IdentityHashMap<>();
    private final ArrayDeque<AssetState> loading = new ArrayDeque<>();
    private final ArrayDeque<AssetState> preloading = new ArrayDeque<>();
    private final ArrayList<AssetState> loaded = new ArrayList<>();
    private final ArrayList<AssetState> preloaded = new ArrayList<>();

    private Runnable onPreloadCompletion;

    public AutoLoader(String name) {
        this.name = name;
    }

    private TypeInfo computeTypeInfo(Class<?> type) {
        // TODO может сначала проверять что за тип пришёл? Ведь можно засунуть и тип без ресурсов :')
        MethodHandles.Lookup lookup;
        try {
            lookup = MethodHandles.privateLookupIn(type, theLookup);
        } catch (IllegalAccessException exc) {
            throw new RuntimeException("Failed to acquire private lookup in " + type, exc);
        }
        var assetBindings = new ArrayList<AssetBindings>();
        var inners = new ArrayList<InnerAssetBinding>();

        boolean allOwned = true;
        for (Field field : type.getDeclaredFields()) {
            String fieldName = field.getName();
            // не хочу ломать семантику языка. final значит нельзя трогать
            boolean isFinalField = Modifier.isFinal(field.getModifiers());
            var fieldType = field.getType();
            if (isFinalField && // TODO это жёсткое требование, но мне так удобно
                    GameObject.class.isAssignableFrom(fieldType)) {
                TypeInfo innerTypeInfo = computeTypeInfo(fieldType);
                allOwned &= innerTypeInfo.allOwned;

                VarHandle handle;
                try {
                    handle = lookup.unreflectVarHandle(field);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // TODO а может ли случиться?
                }

                inners.add(new InnerAssetBinding(fieldName, innerTypeInfo, handle));
                continue;
            }
            if (isFinalField) {
                continue;
            }

            var load = field.getAnnotation(Load.class);
            if (load == null) {
                continue;
            }
            var loadType = load.load();
            String name = load.value();
            boolean owning = load.owned();
            MethodHandle assetNameProviderHandle = null;
            if (name.isEmpty()) {
                // TODO придумать как ассоциировать метод
                String assetNameProvider = fieldName + "Name";
                try {
                    assetNameProviderHandle = lookup.findVirtual(type, assetNameProvider, MethodType.methodType(String.class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            VarHandle handle;
            try {
                handle = lookup.unreflectVarHandle(field);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e); // TODO а может ли случиться?
            }

            allOwned &= owning;
            assetBindings.add(new AssetBindings(fieldType, fieldName, name, assetNameProviderHandle, handle, loadType, owning));
        }

        var assetBindingsArr = assetBindings.toArray(new AssetBindings[0]);
        var innersArr = inners.toArray(new InnerAssetBinding[0]);

        return new TypeInfo(assetBindingsArr, innersArr, allOwned);
    }

    private TypeInfo getTypeInfo(Class<?> type) {
        return bindingsByType.computeIfAbsent(type, this::computeTypeInfo);
    }

    public void onTransition(AutoLoader from) {
        loaded.addAll(from.preloaded);
    }

    public void onPreloadCompletion(Runnable act) {
        onPreloadCompletion = act;
    }

    public void loadSync() {
        loading.removeIf(this::processLoading);
    }

    static final class AssetState {

        final Object inst;
        final TypeInfo typeInfo;
        final Future<?>[] futures;
        final String[] actualNames; // TODO что можно придумать получше?
        final Type type;
        final AssetState[] depends;
        State state;

        AssetState(Object inst, TypeInfo typeInfo,
                   Future<?>[] futures, String[] actualNames,
                   Type type, AssetState[] depends,
                   State state) {
            this.inst = inst;
            this.typeInfo = typeInfo;
            this.futures = futures;
            this.actualNames = actualNames;
            this.type = type;
            this.depends = depends;
            this.state = state;
        }

        @Override
        public String toString() {
            return "AssetState{" +
                    "inst=" + inst +
                    ", actualNames=" + Arrays.toString(actualNames) +
                    ", type=" + type +
                    ", depends=" + Arrays.toString(depends) +
                    ", state=" + state +
                    '}';
        }

        enum Type { OWNED, PRELOAD, CHILD }

        enum State {
            LOADING,
            LOADED,
            UNLOADED
        }
    }

    public boolean update() {
        return processUpdate(false);
    }

    public boolean updatePreload() {
        return processUpdate(true);
    }

    final boolean[] allLoaded0 = {true}; // :///

    private boolean processUpdate(boolean preload) {
        var allLoaded = allLoaded0;
        allLoaded[0] = true;

        var deque = dequeFor(preload);
        deque.removeIf(assetState -> {
            boolean loaded = processLoading(assetState);
            allLoaded[0] &= loaded;
            return loaded;
        });

        if (preload && allLoaded[0]) {
            Runnable act = onPreloadCompletion;
            if (act != null) {
                onPreloadCompletion = null;
                act.run();
            }
        }

        return allLoaded[0];
    }

    private ArrayDeque<AssetState> dequeFor(boolean preload) {
        return preload ? preloading : loading;
    }

    private boolean processLoading(AssetState assetState) {
        return processLoading(assetState, null);
    }

    private boolean processLoading(AssetState assetState, String fieldName) {
        if (assetState.state == AssetState.State.LOADED) {
            return true;
        }

        boolean allAssetsLoaded = true;
        var inst = assetState.inst;
        var typeInfo = assetState.typeInfo;
        var futures = assetState.futures;
        for (int i = 0; i < futures.length; i++) {
            var asset = futures[i];
            if (asset == null) {
                continue; // этот ресурс уже обработан
            }
            boolean isDone = asset.isDone();
            if (isDone) {
                var binding = typeInfo.bindings[i];
                switch (asset.state()) {
                    case SUCCESS -> {
                        var res = asset.resultNow();

                        if (log.isDebugEnabled()) {
                            log.debug("[{}] {} {}.{}", name,
                                    assetState.type == AssetState.Type.OWNED ? "Loaded" : "Preloaded",
                                    inst.getClass().getSimpleName(), binding.fieldName);
                        }


                        futures[i] = null;
                        binding.set(inst, res);
                    }
                    case FAILED -> {
                        if (log.isDebugEnabled()) {
                            var actualNames = assetState.actualNames;
                            log.debug("[{}] Failed to load {}.{} that requires a {}('{}')",
                                    name, inst.getClass().getSimpleName(), binding.fieldName,
                                    binding.type, actualNames[i]);
                        }
                        DebugTools.rethrow(asset.exceptionNow());
                        return false;
                    }
                }
            }
            allAssetsLoaded &= isDone;
        }

        AssetState[] depends = assetState.depends;
        for (int i = 0; i < depends.length; i++) {
            AssetState depend = depends[i];
            allAssetsLoaded &= processLoading(depend, typeInfo.inners[i].fieldName);
        }

        if (allAssetsLoaded) {
            assetState.state = AssetState.State.LOADED;
            switch (assetState.type) {
                case OWNED -> loaded.add(assetState);
                case PRELOAD -> preloaded.add(assetState); // подготовим для передачи в другого владельца
            }

            if (inst instanceof AssetLifecycle l) {
                try {
                    l.onLoaded();
                } catch (Exception e) {
                    log.error("[{}] Exception while loading {}", name, l);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("[{}] {} {}{}",
                        name, assetState.type == AssetState.Type.OWNED ? "Loaded" : "Preloaded",
                        inst.getClass().getSimpleName(), fieldName != null ? "." + fieldName : "");
            }
            return true;
        }
        return false;
    }

    public void add(Object obj) {
        enqueueInstance(obj, false, 0);
    }

    public void addPreload(Object obj) {
        enqueueInstance(obj, true, 0);
    }

    public void unload() {
        cancelLoading(false);
        cancelLoading(true);
        for (AssetState assetState : loaded) {
            unload(assetState);
        }

        bindingsByType.clear(); // TODO придумать отчистку получше
        loaded.clear();
        loading.clear();
        preloading.clear();
        preloaded.clear();
    }

    private void unload(AssetState assetState) {
        var actualNames = assetState.actualNames;
        var typeInfo = assetState.typeInfo;
        var bindings = typeInfo.bindings;
        for (int i = 0; i < bindings.length; i++) {
            var bind = bindings[i];
            if (bind.owning) {
                Global.assets.unload(bind.type, actualNames[i]);
            }
        }
        for (AssetState depend : assetState.depends) {
            unload(depend);
        }
        if (assetState.inst instanceof AssetLifecycle l) {
            try {
                l.onUnloaded();
            } catch (Exception e) {
                log.error("[{}] Exception while unloading {}", name, l);
            }

            if (log.isDebugEnabled())
                log.debug("[{}] Unloaded {}", name, l.getClass().getSimpleName());
        }
    }

    private void cancelLoading(boolean preload) {
        var deque = dequeFor(preload);
        for (AssetState assetState : deque) {
            cancelLoading(assetState);
        }
    }

    private void cancelLoading(AssetState assetState) {
        // TODO вообще, тут очень простая рекурсия, но у меня нет под рукой готового итератора
        for (Future<?> future : assetState.futures) {
            if (future != null) {
                future.cancel(false);
            }
        }
        for (AssetState depend : assetState.depends) {
            cancelLoading(depend);
        }
    }

    private AssetState enqueueInstance(Object obj, boolean preload, int recursionLevel) {
        for (AssetState assetState : loaded) {
            if (assetState.inst == obj) // проверка на дубликаты. Это также обрабатывает передачу владением preloaded очереди
                return assetState;
        }

        if (log.isDebugEnabled())
            log.debug("[{}] {}{} {}",
                    name, " ".repeat(recursionLevel),
                    preload ? "Preloading" : "Loading", obj.getClass().getSimpleName());

        var deque = dequeFor(preload);
        var typeInfo = getTypeInfo(obj.getClass());
        var inners = typeInfo.inners;
        var depends = new AssetState[inners.length];
        for (int i = 0; i < inners.length; i++) {
            var inner = inners[i];
            var innerValue = inner.handle.get(obj);
            depends[i] = enqueueInstance(innerValue, preload, recursionLevel + 1);
        }

        var bindings = typeInfo.bindings;
        var futures = new Future<?>[bindings.length];
        var actualNames = new String[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            AssetBindings binding = bindings[i];
            String name = binding.getName(obj);

            actualNames[i] = name;
            futures[i] = Global.assets.load(binding.type, name, binding.loadType);
        }

        AssetState.Type type;
        if (preload) {
            type = AssetState.Type.PRELOAD;
        } else if (recursionLevel > 0) {
            type = AssetState.Type.CHILD;
        } else {
            type = AssetState.Type.OWNED;
        }
        var state = new AssetState(obj, typeInfo, futures, actualNames, type, depends, AssetState.State.LOADING);
        if (recursionLevel == 0) {
            deque.addLast(state);
        }

        return state;
    }

    static final class InnerAssetBinding {
        final String fieldName;
        final TypeInfo typeInfo;
        final VarHandle handle;

        InnerAssetBinding(String fieldName, TypeInfo typeInfo, VarHandle handle) {
            this.fieldName = fieldName;
            this.typeInfo = typeInfo;
            this.handle = handle;
        }

        Object get(Object object) {
            return (Object) handle.get(object);
        }
    }

    static final class TypeInfo {
        final AssetBindings[] bindings;
        final InnerAssetBinding[] inners;
        final boolean allOwned;

        TypeInfo(AssetBindings[] bindings, InnerAssetBinding[] inners, boolean allOwned) {
            this.bindings = bindings;
            this.inners = inners;
            this.allOwned = allOwned;
        }
    }

    static final class AssetBindings {
        final Class<?> type;
        final String fieldName;
        final String name;
        final MethodHandle nameProvider;
        final VarHandle field;
        final AssetsManager.LoadType loadType;
        final boolean owning;

        AssetBindings(Class<?> type, String fieldName, String name, MethodHandle nameProvider,
                      VarHandle field, AssetsManager.LoadType loadType,
                      boolean owning) {
            this.type = type;
            this.fieldName = fieldName;
            this.name = name;
            this.nameProvider = nameProvider;
            this.field = field;
            this.loadType = loadType;
            this.owning = owning;
        }

        String getName(Object instance) {
            if (!name.isEmpty()) {
                return name;
            }
            try {
                return (String) nameProvider.invoke(instance);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        void set(Object gameObject, Object assetObj) {
            try {
                field.set(gameObject, assetObj);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        Object get(Object object) {
            return (Object) field.get(object);
        }
    }
}
