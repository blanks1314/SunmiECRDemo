package com.pos.hardware.connection.demo.help;
import android.util.Log;

/**
 * @author: Dadong
 * @date: 2024/11/21
 */
public class Logger {

    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int NONE = 6;

    private static int printLevel = VERBOSE;

    public static void v(String tag, String msg) {
        log(VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        log(DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        log(INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        log(WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        log(ERROR, tag, msg);
    }

    private static void log(int type, String tag, String message) {
        int index = 4;
        StackTraceElement[] stackTraceArrays = Thread.currentThread().getStackTrace();
        StackTraceElement stackTrace = stackTraceArrays[index];
        String fileName = stackTrace.getFileName();
        String className = stackTrace.getClassName();
        int lineNumber = stackTrace.getLineNumber();
        String methodName = stackTrace.getMethodName();

        StringBuilder builder = new StringBuilder();
        builder.append(className)
                .append(".")
                .append(methodName)
                .append("()")
                .append(" (")
                .append(fileName)
                .append(":")
                .append(lineNumber)
                .append(") ");

        String prefixString = builder.toString();
        int size = 3800;
        String printString = message;
        int length = printString.length();

        if (length > size) {
            while (printString.length() > size) {
                String substring = printString.substring(0, size);
                printString = printString.substring(size);
                print(type, tag, substring, prefixString);
            }
        }
        print(type, tag, printString, prefixString);
    }

    private static void print(int type, String tag, String message, String prefix) {
        String log = prefix + message;
        switch (type) {
            case INFO:
                if (printLevel <= INFO) Log.i(tag, log);
                break;
            case WARN:
                if (printLevel <= WARN) Log.w(tag, log);
                break;
            case DEBUG:
                if (printLevel <= DEBUG) Log.d(tag, log);
                break;
            case ERROR:
                if (printLevel <= ERROR) Log.e(tag, log);
                break;
            case VERBOSE:
                if (printLevel <= VERBOSE) Log.v(tag, log);
                break;
        }
    }
}
