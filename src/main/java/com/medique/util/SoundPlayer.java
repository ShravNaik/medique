package com.medique.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class SoundPlayer {

    /**
     * Plays a pleasant dual-tone chime sound programmatically without requiring external files.
     */
    public static void playReminderChime() {
        new Thread(() -> {
            try {
                // Generate 44.1 kHz, 16-bit mono PCM chime
                float sampleRate = 44100f;
                int durationMs = 800;
                int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                byte[] audioData = new byte[numSamples * 2];

                // Frequencies for a pleasant medical reminder chime (E5: ~659 Hz, B5: ~987 Hz)
                double freq1 = 659.25;
                double freq2 = 987.77;

                for (int i = 0; i < numSamples; i++) {
                    double time = i / (double) sampleRate;
                    // Decay envelope
                    double envelope = Math.exp(-3.5 * time);
                    double sampleVal = (0.6 * Math.sin(2 * Math.PI * freq1 * time)
                                      + 0.4 * Math.sin(2 * Math.PI * freq2 * time)) * envelope;

                    short val = (short) (sampleVal * 32767);
                    // Little-endian
                    audioData[2 * i] = (byte) (val & 0xFF);
                    audioData[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
                }

                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(Clip.class, format);

                if (AudioSystem.isLineSupported(info)) {
                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(format, audioData, 0, audioData.length);
                    clip.start();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            } catch (Exception e) {
                try {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        }, "Medique-SoundPlayer-Thread").start();
    }
}
