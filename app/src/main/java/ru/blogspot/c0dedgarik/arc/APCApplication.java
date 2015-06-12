package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class APCApplication extends android.app.Application {
    private static APCApplication sApplication;

    private HttpServer mHttpServer;
    private VideoStream mVideoStream;
    private CustomToast mToast;

    @Override
    public void onCreate() {


        mHttpServer = new HttpServer(8080);
        mHttpServer.setContext(this);

        mHttpServer.addCommand("up", R.raw.up);
        mHttpServer.addCommand("down", R.raw.down);
        mHttpServer.addCommand("left", R.raw.left);
        mHttpServer.addCommand("right", R.raw.right);
        mHttpServer.addCommand("downleft", R.raw.downleft);

        mToast = new CustomToast();

        mVideoStream = new VideoStream();
        mHttpServer.setVideoStream(mVideoStream);

        super.onCreate();
    }

    public void toast(String text, int duration) {
        mToast.show(text, duration);
    }

    public VideoStream getVideoStream() {
        return mVideoStream;
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

    public static APCApplication getInstance() {
        return sApplication;
    }


    public HttpServer getHttpServer() {
        return mHttpServer;
    }
}
