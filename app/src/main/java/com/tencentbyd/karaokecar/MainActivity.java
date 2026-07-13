ackage com.tencentbyd.karaokecar;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        result = new TextView(this);
        result.setText("AudioVisualizer Test\nReady");
        result.setTextSize(20);
        result.setPadding(0, 0, 0, 30);
        root.addView(result);

        Button start = new Button(this);
        start.setText("START AUDIO OUTPUT");
        start.setOnClickListener(v -> startAudioOutput());
        root.addView(start);

        Button youtube = new Button(this);
        youtube.setText("OPEN YOUTUBE PREMIUM");
        youtube.setOnClickListener(v -> openYouTube());
        root.addView(youtube);

        setContentView(root);
    }

    private void startAudioOutput() {
        try {
            AudioManager manager =
                    (AudioManager) getSystemService(AUDIO_SERVICE);

            Method method =
                    AudioManager.class.getMethod("startAudioOutput");

            Object response = method.invoke(manager);

            result.setText(
                    "SUCCESS\nstartAudioOutput called\nResult: " + response
            );

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            result.setText(
                    "INVOCATION FAILED\n"
                            + cause.getClass().getName()
                            + "\n"
                            + cause.getMessage()
            );

            android.util.Log.e(
                    "AudioVisualizerTest",
                    "startAudioOutput invocation failed",
                    cause
            );

        } catch (Throwable error) {
            result.setText(
                    "FAILED\n"
                            + error.getClass().getName()
                            + "\n"
                            + error.getMessage()
            );

            android.util.Log.e(
                    "AudioVisualizerTest",
                    "startAudioOutput failed",
                    error
            );
        }
    }

    private void openYouTube() {
        Intent launch =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                "com.android.youtube.premium"
                        );

        if (launch == null) {
            result.setText("YouTube Premium not found");
            return;
        }

        startActivity(launch);
    }
}
