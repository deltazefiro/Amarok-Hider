package deltazero.amarok.utils;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ShellUtil {
    public static String[] exec(String[] cmds) {
        try {
            Log.d("ShellExec", Arrays.toString(cmds));
            Process p = Runtime.getRuntime().exec(cmds);
            String stdoutString = convertInputStreamToString(p.getInputStream());
            String stderrString = convertInputStreamToString(p.getErrorStream());
            if (stdoutString.length() > 0)
                Log.d("ShellOut", stdoutString);
            if (stderrString.length() > 0)
                Log.i("ShellErr", stderrString);
            return new String[]{stdoutString, stderrString};
        } catch (IOException e) {
            Log.w("ShellErr", e.toString());
            return null;
        }
    }

    public static String[] exec(String cmd){
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            String stdoutString = convertInputStreamToString(p.getInputStream());
            String stderrString = convertInputStreamToString(p.getErrorStream());
            if (stdoutString.length() > 0)
                Log.d("ShellOut", stdoutString);
            if (stderrString.length() > 0)
                Log.i("ShellErr", stderrString);
            return new String[]{stdoutString, stderrString};
        } catch (IOException e) {
            Log.w("ShellErr", e.toString());
            return null;
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) {
        String newLine = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        try {
            for (String line; (line = reader.readLine()) != null; ) {
                if (result.length() > 0) {
                    result.append(newLine);
                }
                result.append(line);
            }
        } catch (IOException e) {
            return "";
        }
        return result.toString();

    }
}