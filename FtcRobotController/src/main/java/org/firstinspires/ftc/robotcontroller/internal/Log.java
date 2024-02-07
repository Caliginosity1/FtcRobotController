package org.firstinspires.ftc.robotcontroller.internal;

import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
    private static final String BASE_FOLDER_NAME = "FTCLogs"; // Adjust the folder name as needed
    private FileWriter fileWriter;
    private StringBuilder stringBuilder = new StringBuilder();
    private final boolean logTime;
    private final long startTime;

    public Log(String filename, boolean logTime) {
        this.logTime = logTime;
        this.startTime = System.nanoTime();
        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + BASE_FOLDER_NAME;
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean wasSuccessful = directory.mkdirs();
            if (!wasSuccessful) {
                System.out.println("Failed to create directory for logs.");
            }
        }
        try {
            File logFile = new File(directoryPath, filename + ".csv");
            this.fileWriter = new FileWriter(logFile, false); // False to overwrite existing files
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addData(String key, double value) {
        addData(key + ": " + value);
    }

    public void addData(String data) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(", ");
        }
        stringBuilder.append(data);
    }

    // Overloaded methods for supporting logging of all basic types directly
    public void addData(String key, Object value) { addData(key + ": " + value); }
    public void addData(String key, boolean value) { addData(key + ": " + value); }
    public void addData(String key, byte value) { addData(key + ": " + value); }
    public void addData(String key, char value) { addData(key + ": " + value); }
    public void addData(String key, short value) { addData(key + ": " + value); }
    public void addData(String key, int value) { addData(key + ": " + value); }
    public void addData(String key, long value) { addData(key + ": " + value); }
    public void addData(String key, float value) { addData(key + ": " + value); }

    public void update() {
        try {
            if (logTime) {
                long currentTime = System.nanoTime();
                double elapsedTimeInSeconds = (currentTime - startTime) / 1E9;
                stringBuilder.insert(0, "Elapsed Time: " + elapsedTimeInSeconds + ", ");
            }
            fileWriter.write(stringBuilder.toString() + "\n");
            fileWriter.flush();
            stringBuilder.setLength(0); // Clear the StringBuilder for new data
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
