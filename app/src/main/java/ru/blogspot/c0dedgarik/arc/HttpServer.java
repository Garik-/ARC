package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;


public class HttpServer extends NanoHTTPD {

    private static final int WEBSOCKET_TIMEOUT = 30000; // timeout = 30 sec !!
    private static final int FAVICON_QUALITY = 100;

    private Context mContext;
    private Resources mResources;
    private SharedPreferences mSharedPreferences;

    private String lastUpdateTime = null;
    private VideoStream mVideoStream;
    private WebSocketServer mWebSocketServer;

    @Override
    public void stop() {
        mVideoStream.stop();
        mWebSocketServer.stop();
        super.stop();
    }


    //final private Pattern commandPattern;
    final private Pattern ipPattern;

    private static SimpleDateFormat GMT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        GMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    public HttpServer(final Context context, final int port) {
        super(port);

        //commandPattern = Pattern.compile("^\\/command\\/([a-z]*)$");
        ipPattern = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);

        setContext(context);
    }

    public void setContext(Context context) {

        mResources = context.getResources();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mWebSocketServer = new WebSocketServer(context, mResources.getInteger(R.integer.socket_port));

        mVideoStream = ((ARCApplication) context.getApplicationContext()).getVideoStream();

        mContext = context;
    }


    private static long GTM2Milliseconds(final String date) {
        long mills = 0;
        try {
            mills = GMT.parse(date).getTime();
        } catch (ParseException e) {
            ARCLog.e(e.getMessage());
        }

        return mills;
    }

    private static String Milliseconds2GMT(final long mills) {
        return GMT.format(mills);
    }

    /**
     * @param headers
     * @param modifiedTime
     * @return true если нужно обновить false оставить без изменений
     */
    private boolean isUpdate(Map<String, String> headers, String modifiedTime) {
        boolean update = true;
        String modifiedSince = headers.get("if-modified-since");

        if (null != modifiedSince && null != modifiedTime) {
            update = !modifiedSince.equals(modifiedTime);
        }

        return update;
    }

    private boolean isUpdate(Map<String, String> headers, long modifiedTime) {
        boolean update = true;
        String modifiedSince = headers.get("if-modified-since");

        if (null != modifiedSince) {
            update = (GTM2Milliseconds(modifiedSince) < modifiedTime);
        }

        return update;
    }

    private Response checkCache(IHTTPSession session) {
        Response response = null;

        if (lastUpdateTime == null) {
            try {
                PackageInfo localPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                lastUpdateTime = Milliseconds2GMT(localPackageInfo.lastUpdateTime);
            } catch (PackageManager.NameNotFoundException e) {
                ARCLog.e(e.getMessage());
            }
        }

        if (!isUpdate(session.getHeaders(), lastUpdateTime)) {
            response = newFixedLengthResponse(Response.Status.NOT_MODIFIED, NanoHTTPD.MIME_PLAINTEXT, "Not Modified");
        }

        return response;
    }

    private void addCache(Response response) {
        if (lastUpdateTime != null) {
            response.addHeader("Cache-control", "private");
            response.addHeader("Last-Modified", lastUpdateTime);
        }
    }

    private Response indexResponse(IHTTPSession session) {

        Response response = checkCache(session);
        if (null == response) {

            InputStream is = mResources.openRawResource(R.raw.index);
            response = newChunkedResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, is);
            addCache(response);
        }

        return response;
    }

    private Response faviconResponse(IHTTPSession session) {
        Response response = checkCache(session);
        if (null == response) {
            try {
                Drawable icon = mContext.getPackageManager().getApplicationIcon(mContext.getPackageName());
                BitmapDrawable bitDw = ((BitmapDrawable) icon);
                Bitmap bitmap = bitDw.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, FAVICON_QUALITY, stream);
                byte[] imageInByte = stream.toByteArray();
                ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);

                response = newChunkedResponse(Response.Status.OK, "image/png", bis);
                response.addHeader("Connection", "close");
                addCache(response);

            } catch (PackageManager.NameNotFoundException e) {
                ARCLog.e(e.getMessage());

                response = newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
            }
        }

        return response;
    }

    private int getPrefString(final String name, int def) {
        int defaultValue;

        try {
            defaultValue = mResources.getInteger(def);
        } catch (Resources.NotFoundException e) {
            defaultValue = def;
        }

        String value = mSharedPreferences.getString(name, String.valueOf(defaultValue));
        return Integer.parseInt(value);
    }

    /*private Response executeCommand(String command) {

        Integer resId = mCommands.get(command);
        if (null == resId) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Bad Request");
        }

        // загрузка данных файла в память осуществляется в том же потоке что и сессия клиента...
        // возможно стоит запускать отдельный тред... хз...
        mControl.command(resId.intValue());
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, null);
    }*/

    @Override
    public void start() throws IOException {


        setPort(getPrefString("port", R.integer.port));
        mWebSocketServer.setPort(getPrefString("websocket_port", R.integer.socket_port));

        if (null != mVideoStream) {


            mVideoStream.setVideoQuality(
                    new VideoQuality(
                            mSharedPreferences.getString("video_resolution", "640x480"),
                            mSharedPreferences.getInt("video_quality", mResources.getInteger(R.integer.quality)),
                            90//sp.getInt("")
                            //Integer.parseInt(sp.getString("video_orientation","90")

                    )
            );
            //mVideoStream.startPreview();
            mVideoStream.start();
        }

        mWebSocketServer.start(WEBSOCKET_TIMEOUT);

        super.start();
    }

    private Response videoStream(IHTTPSession session) {
        try {
            mVideoStream.setSocketOutput(session.getOutputStream());
            return null;
        } catch (Exception e) {
            ARCLog.e(e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Server Error");
        }
    }


    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();

        ARCLog.d(method + " '" + uri + "' ");

        if (!(Method.GET.equals(method))) {
            return newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED, NanoHTTPD.MIME_PLAINTEXT, "Not Implemented");
        }

        if (uri.equals("/")) {
            return indexResponse(session);
        }

        if (uri.equals("/ws")) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            Response r = newFixedLengthResponse(
                    Response.Status.OK,
                    NanoHTTPD.MIME_PLAINTEXT,
                    sp.getString("websocket_port", String.valueOf(mResources.getInteger(R.integer.socket_port)))
            );

            r.addHeader("Connection", "close"); // !!! убираем pending
            return r;
        }


        if (uri.equals("/stream")) {
            return videoStream(session);
        }

        if (uri.equals("/favicon.ico")) {
            return faviconResponse(session);
        }

        /*if (Method.POST.equals(method)) {

            Matcher m = commandPattern.matcher(uri);
            if (m.matches()) {
                return executeCommand(m.group(1));
            }
        }*/

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    public boolean isIpv4Address(String ipAddress) {
        Matcher m1 = ipPattern.matcher(ipAddress);
        return m1.matches();
    }

    /**
     * Returns the IP address of the first configured interface of the device
     *
     * @param removeIPv6 If true, IPv6 addresses are ignored
     * @return the IP address of the first configured interface or null
     */
    public String getLocalIpAddress(boolean removeIPv6) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.isSiteLocalAddress() &&
                            !inetAddress.isAnyLocalAddress() &&
                            (!removeIPv6 || isIpv4Address(inetAddress.getHostAddress().toString()))) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ignore) {
        }
        return null;
    }
}
