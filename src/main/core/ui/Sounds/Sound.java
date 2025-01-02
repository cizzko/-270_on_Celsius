package core.ui.Sounds;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import javax.sound.sampled.*;
import java.io.File;
import java.util.HashSet;

public class Sound {
    private static final int effectVolume = Integer.parseInt(Config.getFromConfig("EffectsVolume"));
    private static final int musicVolume = Integer.parseInt(Config.getFromConfig("SoundsVolume"));
    private static int volume;
    private static boolean suppVolumeLevel = true, error = false;
    public static HashSet<String> sounds = new HashSet<>();

    public enum types {
        SOUND,
        EFFECT
    }

    // only wav, dev 0.0.0.2
    public static void playSound(String path, types type, boolean limitAmount) {
        if (path != null && (!sounds.contains(path) || !limitAmount)) {
            if (!suppVolumeLevel && !error) {
                error = true;
                Logger.log("this device not supported volume level");
            }

            new Thread(() -> {
                volume = type == types.EFFECT ? effectVolume : musicVolume;

                try {
                    sounds.add(path);

                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(path));
                    AudioFormat format = inputStream.getFormat();

                    // to PCM
                    if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                        inputStream = AudioSystem.getAudioInputStream(format, inputStream);
                    }
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                    suppVolumeLevel = sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN);

                    try {
                        FloatControl gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                        gainControl.setValue(20f * (float) Math.log10(volume));
                    } catch (Exception e) {
                        if (!error) {
                            error = true;
                            Logger.printException("Error at gain control", e);
                        }
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
                    Logger.printException("Error during sound playback, file: " + path, e);
                } finally {
                    sounds.remove(path);
                }
            }).start();
        }
    }
}
