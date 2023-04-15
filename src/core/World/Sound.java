package core.World;

import javax.sound.sampled.*;
import java.io.File;

public class Sound extends Thread {
    private static String path;
    private static boolean stop = true;

    public static void SoundPlay(String path) {
        Sound.path = path;
        Sound.stop = false;
    }

    public void run() {
        while (true) {
            if (!stop) {
                try {
                    // загрузить аудио-файл
                    File file = new File(path);
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
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
