package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ARCApplication extends android.app.Application {
    private static ARCApplication sApplication;

    private HttpServer mHttpServer;
    private VideoStream mVideoStream;
    private CustomToast mToast;
    private Control mControl;


    public interface DI {
        public void setContext(Context context);
    }

    @Override
    public void onCreate() {


        mVideoStream = new VideoStream();

        mControl = new Control(this);

        // значения циклов повторения подобраны эксперементально... надо бы вынести настроки в настройки
       /* mControl.addCommand("up", R.raw.test_8, 44);
        mControl.addCommand("down", R.raw.down, 10);
        mControl.addCommand("left", R.raw.left, 0);
        mControl.addCommand("right", R.raw.right, 48);
        mControl.addCommand("downleft", R.raw.downleft, 20); // 18 */

        mControl.addCommand("up", 10);
        mControl.addCommand("down", 40);
        mControl.addCommand("left", 58);
        mControl.addCommand("right", 64);
        mControl.addCommand("downleft", 52);
        mControl.addCommand("downright", 46);
        mControl.addCommand("upright", 34);
        mControl.addCommand("upleft", 28);

        mHttpServer = new HttpServer(8080);
        mHttpServer.setContext(this);

        mToast = new CustomToast();


        super.onCreate();
    }

    public void toast(String text, int duration) {
        mToast.show(text, duration);
    }

    private class CustomToast {
        private Toast mToast;
        private TextView mTextView;

        public CustomToast() {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.toast_layout, null);

            mTextView = (TextView) layout.findViewById(R.id.text);

            mToast = new Toast(getApplicationContext());
            mToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            mToast.setView(layout);
        }

        public void show(String text, int duration) {
            mTextView.setText(text);
            mToast.setDuration(duration);
            mToast.show();
        }
    }

    public static ARCApplication getInstance() {
        return sApplication;
    }
    public HttpServer getHttpServer() {
        return mHttpServer;
    }

    public Control getControl() {
        return mControl;
    }

    public VideoStream getVideoStream() {
        return mVideoStream;
    }
}
