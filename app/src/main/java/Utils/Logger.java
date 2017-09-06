package Utils;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;

/**
 * Created by paqin on 06/09/2017.
 */

public class Logger {

    private static SDCardWriter writer = SDCardWriter.buildSDCardWriter("logs");
    private static android.os.Handler mHandler = new android.os.Handler(Looper.getMainLooper());
    private static StringBuilder buffer = new StringBuilder();

    private static String LOGGER_TAG = "log";

    private static String routeBuffer = new String();

    private static TextView mTextView;

    public static void showLog()
    {
        mTextView.setText(getBuffer());
        mTextView.setVisibility(View.VISIBLE);
        mTextView.scrollTo(0, mTextView.getHeight());

    }

    public static void clearLog() {
        clearBuffer();
        showLog();
    }
    public static void setRoute(String route)
    {
        routeBuffer = route;
    }
    public static void showRoute()
    {
        mTextView.setText(routeBuffer);
        mTextView.setVisibility(View.VISIBLE);
        mTextView.scrollTo(0, mTextView.getHeight());
    }
    public static void hide()
    {
        mTextView.setVisibility(View.GONE);
    }

    public static void setOutTextView(TextView textView)
    {
        mTextView = textView;
        textView.setVisibility(View.GONE);
    }

    public static void log(final String data) {
        log(LOGGER_TAG, data);
    }

    public static void saveLogs() {
        if(buffer != null && buffer.length() > 0) {
            writer.writeLogFile(buffer);
        }
    }
    public static void log(final String tag, final String data)
    {


        if (buffer.length() > 1024 * 1024 * 2)
        {
            saveLogs();
            buffer = new StringBuilder();
        }
        SimpleDateFormat sDateFormat    =   new    SimpleDateFormat("hh:mm:ss");
        final String    date    =    sDateFormat.format(new    java.util.Date());
        Log.d(tag, data);
        if(Looper.getMainLooper() == Looper.myLooper()) {
            buffer.append(date).append("  ").append(tag).append(" : ").append(data).append("\n\r");

            if(mTextView == null) {
                return;
            }
            mTextView.setText(getBuffer());
            mTextView.scrollTo(0,mTextView.getHeight());
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    buffer.append(date).append("  ").append(tag).append(" : ").append(data).append("\n\r");
                    if(mTextView == null) {
                        return;
                    }
                    mTextView.setText(getBuffer());
                    mTextView.scrollTo(0, mTextView.getHeight());
                }
            });
        }

    }

    private static void clearBuffer() {
        buffer = new StringBuilder();
    }

    public static String getBuffer()
    {
        return buffer.toString();
    }

    public static void clear()
    {
        buffer = new StringBuilder();
    }
}



