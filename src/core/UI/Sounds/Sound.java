package core.UI.Sounds;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class Sound {
    private static final int effectVolume = Integer.parseInt(Config.getFromConfig("EffectsVolume"));
    private static final int musicVolume = Integer.parseInt(Config.getFromConfig("SoundsVolume"));
    private static int volume;
    private static boolean suppVolumeLevel = true;
    public static ConcurrentHashMap<String, Boolean> sounds = new ConcurrentHashMap<>();

    //only wav, dev 0.0.0.2
    public static void SoundPlay(String path, String type) {
        if (sounds.get(path) == null || !sounds.get(path)) {
            if (!suppVolumeLevel) {
                Logger.log("this device not supported volume level");
            }

            new Thread(() -> {
                if (type.equals("effect")) volume = effectVolume;
                if (type.equals("sound")) volume = musicVolume;

                try {
                    sounds.put(path, true);

                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(path));
                    AudioFormat format = inputStream.getFormat();

                    // преобразовать формат в PCM
                    if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                        inputStream = AudioSystem.getAudioInputStream(format, inputStream);
                    }
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                    suppVolumeLevel = sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN);

                    try {
                        FloatControl gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(20f * (float) Math.log10(volume));
                    } catch (Exception e) {
                        Logger.log(e.toString());
                    }

                    sourceDataLine.open(format);
                    sourceDataLine.open(format);
                    sourceDataLine.start();

                    byte[] bytesBuffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                        sourceDataLine.write(bytesBuffer, 0, bytesRead);
                    }
                    sourceDataLine.drain();
                    sourceDataLine.stop();
                    sourceDataLine.close();

                } catch (Exception e) {
                    Logger.log("Error during sound playback: " + e + ", file: " + path);
                } finally {
                    sounds.put(path, false);
                }
            }).start();
        }
    }
}