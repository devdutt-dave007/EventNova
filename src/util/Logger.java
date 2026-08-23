// ============================================
// util/Logger.java
// ============================================
package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Logger {

    private static final String LOG_FILE = "eventnova3_log.txt";

    public static void log(String message) {
        Runnable logTask = new Runnable() {
            @Override
            public void run() {
                writeToFile(message);
            }
        };
        new Thread(logTask).start();
    }

    private static synchronized void writeToFile(String message) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            pw.println("[" + LocalDateTime.now() + "] " + message);
        } catch (IOException e) {
            System.out.println("Log write failed: " + e.getMessage());
        }
    }
}