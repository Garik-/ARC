package ru.blogspot.c0dedgarik.arc;


import android.content.Context;

import java.io.IOException;

import fi.iki.elonen.NanoWebSocketServer;

public class WebSocketServer extends NanoWebSocketServer implements ARCApplication.DI {

    private Context mContext;
    private Control mControl;

    @Override
    public void setContext(Context context) {
        mContext = context;
        mControl = ((ARCApplication) context.getApplicationContext()).getControl();
    }

    public WebSocketServer(int port) {
        super(port);
    }

    public void createControl(Context context) {
        mControl = new Control(context);
    }

    @Override
    protected void onClose(WebSocket socket, WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {

        ARCLog.d("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
                + (reason != null && !reason.isEmpty() ? ": " + reason : ""));

    }

    @Override
    protected void onException(WebSocket socket, IOException e) {
        ARCLog.e(e.getMessage());
    }

    /*@Override
    protected void onFrameReceived(WebSocketFrame frame) {
        ARCLog.d("%s",frame.toString());
    }*/

    @Override
    protected void onMessage(WebSocket socket, WebSocketFrame messageFrame) {


        if (null != mControl) {
            ARCLog.d("%s", messageFrame.toString());
            mControl.command(messageFrame.getTextPayload());
        }
        /*try {
            //messageFrame.setUnmasked();
            //socket.sendFrame(messageFrame);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }


    @Override
    protected void onPong(WebSocket socket, WebSocketFrame pongFrame) {
        pongFrame.setUnmasked();
        ARCLog.d("P " + pongFrame);

    }

    @Override
    public void onSendFrame(WebSocketFrame frame) {
        ARCLog.d("S " + frame);
    }
}
