package core.audio;

import core.Application;
import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;
import core.assets.AssetsManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioHandler extends AssetHandler<AudioBuffer, AudioHandler.Params, AudioHandler.State> {
    private static final ExecutorService AUDIO_EXECUTOR = Executors.newCachedThreadPool(_ -> new Thread());

    public AudioHandler() {
        super(AudioBuffer.class, "content/audio");
    }

    @Override
    public void release(AssetReleaser releaser, AudioBuffer asset) {
        AL10.alDeleteBuffers(asset.bufferId());
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Params params, State state) {
        boolean sync = (res.loadType() == AssetsManager.LoadType.SYNC);
        if (sync) {
            decode(res, name, state);
        } else {
            var future = AUDIO_EXECUTOR.submit(() -> {
                decode(res, name, state);
                return null;
            });
            res.fork(() -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
    }

    @Override
    public AudioBuffer loadSync(AssetResolver res, String name, Params params, State state) throws Exception {
        if (state.pcmData == null || state.format == null) {
            Application.log.error("Audio data not prepared for: " + name);
        }

        int alFormat = getOpenALFormat(state.format.getChannels(), state.format.getSampleSizeInBits());
        int bufferId = AL10.alGenBuffers();
        AL10.alBufferData(bufferId, alFormat, state.pcmData, state.sampleRate);
        state.pcmData = null;

        return new AudioBuffer(bufferId, name);
    }

    @Override
    protected Params createParams() {
        return new Params();
    }

    @Override
    protected State createState() {
        return new State();
    }

    private void decode(AssetResolver res, String name, State state) {
        try (InputStream is = res.openStreamInDir(name, ".wav");
             InputStream bis = new BufferedInputStream(is);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bis)) {

            AudioFormat format = ais.getFormat();
            byte[] rawBytes = ais.readAllBytes();

            state.pcmData = BufferUtils.createByteBuffer(rawBytes.length);
            state.pcmData.put(rawBytes).flip();
            state.format = format;
            state.sampleRate = (int) format.getSampleRate();
        } catch (Exception e) {
            Application.log.error("Failed to decode audio: " + name, e);
        }
    }

    private int getOpenALFormat(int channels, int sampleSizeInBits) {
        if (channels == 1) {
            if (sampleSizeInBits == 8) return AL10.AL_FORMAT_MONO8;
            if (sampleSizeInBits == 16) return AL10.AL_FORMAT_MONO16;
        } else if (channels == 2) {
            if (sampleSizeInBits == 8) return AL10.AL_FORMAT_STEREO8;
            if (sampleSizeInBits == 16) return AL10.AL_FORMAT_STEREO16;
        }
        throw new IllegalArgumentException("Unsupported audio format");
    }

    public static class Params { }

    public static class State {
        public ByteBuffer pcmData;
        public AudioFormat format;
        public int sampleRate;
    }
}