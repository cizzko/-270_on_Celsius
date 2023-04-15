package core.World;

import core.Physics;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class Sound extends Thread {
    private static String path;
    private static boolean stop = true;
    private static ConcurrentHashMap<String, File> sounds = new ConcurrentHashMap<>();

    public static void SoundPlay(String path) {
        Sound.path = path;
        Sound.stop = false;

        if (sounds.isEmpty()) new Thread(new Sound()).start();
        sounds.putIfAbsent(path, new File(path));
    }

    public void run() {
        while (true) {
            if (!stop) {
                try {
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(sounds.get(path));
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

                    // создать source data line и воспроизвести звук
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                    sourceDataLine.open();
                    sourceDataLine.start();
                    byte[] bytesBuffer = new byte[4096];
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                        sourceDataLine.write(bytesBuffer, 0, bytesRead);
                    }
                    sourceDataLine.drain();
                    sourceDataLine.stop();
                    sourceDataLine.close();
                    stop = true;

                } catch (Exception e) {
                    System.err.println("Error during sound playback: " + e);
                }
            }
            Sound.yield();
        }
    }
}
