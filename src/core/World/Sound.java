package core.World;

import core.Logging.config;
import core.Logging.logger;
import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class Sound {
    private static int effectVolume = Integer.parseInt(config.jetFromConfig("EffectsVolume")), musicVolume = Integer.parseInt(config.jetFromConfig("MusicVolume")), volume;
    private static boolean suppVolumeLevel = true;
    private static ConcurrentHashMap<String, Boolean> sounds = new ConcurrentHashMap<>();

    public static void SoundPlay(String path, String type) {
        if (sounds.get(path) != null && sounds.get(path)) {
            return;
        }
        if (!suppVolumeLevel) {
            logger.log("this device not supported volume level");
        }

        new Thread(() -> {
            if (type.equals("effect")) volume = effectVolume;
            if (type.equals("music")) volume = musicVolume;

            try {
                sounds.put(path, true);

                AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(path));
                AudioFormat format = inputStream.getFormat();

                // преобразовать формат в PCM
                if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            format.getSampleRate(),
                            format.getSampleSizeInBits() * 2,
                            format.getChannels(),
                            format.getFrameSize() * 2,
                            format.getFrameRate(),
                            true);
                    inputStream = AudioSystem.getAudioInputStream(format, inputStream);
                }

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                suppVolumeLevel = sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN);

                FloatControl gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(20f * (float) Math.log10(volume));

                sourceDataLine.open(format);
                sourceDataLine.open(format);
                sourceDataLine.start();

                byte[] bytesBuffer = new byte[4096];
                int bytesRead = -1;

                while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                    sourceDataLine.write(bytesBuffer, 0, bytesRead);
                }
                sourceDataLine.drain();
                sourceDataLine.stop();
                sourceDataLine.close();

            } catch (Exception e) {
                logger.log("Error during sound playback: " + e);
            } finally {
                sounds.put(path, false);
            }
        }).start();
    }
}