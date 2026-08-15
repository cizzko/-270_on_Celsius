package core.audio;

import core.Application;
import core.Global;
import core.assets.AssetsManager;
import core.util.Config;
import core.util.Disposable;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class AudioManager implements Disposable {

    @Override
    public void close() {
        sources.keySet().forEach(AL10::alDeleteSources);
        sources.clear();
        currentSources = 0;

        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }

    public enum PlayType {
        REPEAT,
        ONCE
    }

    private enum PlayState {
        PAUSE,
        PLAYING,
        STOP
    }
    private static long device;
    private static long context;

    private static final Map<Integer, Integer> sources = new HashMap<>();
    private static int availableSources;
    private static int currentSources = 0;

    private AudioManager() {}

    public static void init() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            Application.log.error("Unable to open audio device's");
        }
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        int maxHardwareSources = getMaxSources();
        if (maxHardwareSources <= 0) maxHardwareSources = 32;

        availableSources = Config.getInt("soundSources", maxHardwareSources - 5);
    }

    private static int getMaxSources() {
        IntBuffer monoSources = BufferUtils.createIntBuffer(1);
        IntBuffer stereoSources = BufferUtils.createIntBuffer(1);

        ALC10.alcGetIntegerv(device, ALC11.ALC_MONO_SOURCES, monoSources);
        ALC10.alcGetIntegerv(device, ALC11.ALC_STEREO_SOURCES, stereoSources);

        return monoSources.get(0) + stereoSources.get(0);
    }

    public static void play(String filename, AudioType audioType, PlayType type, int priority) {
        if (audioType.getVolume() <= 0) return;
        try {
            AudioBuffer buffer = Global.assets.load(AudioBuffer.class, filename, AssetsManager.LoadType.SYNC).get();
            playLoaded(buffer, audioType, type, priority);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void playAsync(String filename, AudioType audioType, PlayType type, int priority) {
        if (audioType.getVolume() <= 0) return;
        Future<AudioBuffer> future = Global.assets.load(AudioBuffer.class, filename, AssetsManager.LoadType.ASYNC);
        Global.scheduler.post(() -> {
            try {
                AudioBuffer buffer = future.get();
                playLoaded(buffer, audioType, type, priority);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    private static void playLoaded(AudioBuffer buffer, AudioType audioType, PlayType type, int priority) {
        int sourceId = getAvailableSource(priority);
        if (sourceId <= 0) return;

        AL10.alSourcei(sourceId, AL10.AL_BUFFER, buffer.bufferId());
        AL10.alSourcef(sourceId, AL10.AL_GAIN, Math.min(1, audioType.getVolume() / 100f));
        int looping = (type == PlayType.REPEAT) ? AL10.AL_TRUE : AL10.AL_FALSE;
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping);
        AL10.alSourcePlay(sourceId);
    }

    private static PlayState getSourceState(int sourceId) {
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        return switch (state) {
            case AL10.AL_PLAYING -> PlayState.PLAYING;
            case AL10.AL_PAUSED -> PlayState.PAUSE;
            default -> PlayState.STOP;
        };
    }

    private static int getAvailableSource(int priority) {
        cleanStoppedSources();

        if (currentSources < availableSources) {
            currentSources++;
            int id = AL10.alGenSources();
            sources.put(id, priority);
            return id;
        }

        int freedSourceId = freeDowned(priority);
        if (freedSourceId != -1) {
            sources.put(freedSourceId, priority);
            return freedSourceId;
        }

        return 0;
    }

    private static int freeDowned(int currentPriority) {
        int targetSourceId = -1;
        int lowestPriority = 0;

        for (Map.Entry<Integer, Integer> entry : sources.entrySet()) {
            int sourceId = entry.getKey();
            int priority = entry.getValue();

            if (priority < lowestPriority) {
                lowestPriority = priority;
                targetSourceId = sourceId;
            }
        }

        if (targetSourceId != -1 && lowestPriority <= currentPriority) {
            AL10.alSourceStop(targetSourceId);
            AL10.alSourcei(targetSourceId, AL10.AL_BUFFER, 0);
            return targetSourceId;
        }

        return -1;
    }

    private static void cleanStoppedSources() {
        sources.keySet().removeIf(sourceId -> {
            if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED) {
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
                AL10.alDeleteSources(sourceId);
                currentSources--;
                return true;
            }
            return false;
        });
    }

    public static void freeSource(int sourceId) {
        if (!sources.containsKey(sourceId)) return;

        AL10.alSourceStop(sourceId);
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
        AL10.alDeleteSources(sourceId);

        sources.remove(sourceId);
        currentSources--;
    }
}