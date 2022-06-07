package com.weeswares.iok.health.helpers;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Logger {
    public static File extractLogToFileAndWeb(Context context) {
        //set a file
        Date datum = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
        String fullName = df.format(datum) + "appLog.log";
        File file = new File(Environment.getExternalStorageDirectory(), fullName);

        //clears a file
        if (file.exists()) {
            file.delete();
        }


        //write log to file
        try {

            FileWriter out = new FileWriter(file);
            out.write(dumpProcessLog(context).toString());
            out.close();

            //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }


        //clear the log
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }

        return file;
    }

    public static String dumpProcessLog(Context context) {
        try {
            int pid = android.os.Process.myPid();
            String command = "logcat -d -v threadtime *:*";
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String currentLine = null;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(String.valueOf(pid))) {
                    result.append(currentLine);
                    result.append("\n");
                }
            }
            return result.toString();
        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return "";
    }

    public static List<String> getProcessLog(Context context) {
        try {
            int pid = android.os.Process.myPid();
            String command = "logcat -d -v threadtime *:*";
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> result = new ArrayList<>();
            String currentLine = null;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(String.valueOf(pid))) {
                    result.add(currentLine);
                }
            }
            return result;
        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return new ArrayList<>();
    }
}
