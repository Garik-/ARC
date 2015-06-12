package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;


public class HttpServer extends NanoHTTPD {

    private Context mContext;
    private Control mControl;
    private String lastUpdateTime = null;
    private VideoStream mVideoStream;

    @Override
    public void stop() {
        mVideoStream.stop();
        super.stop();
    }


    private HashMap<String, Integer> mCommands;

    final private Pattern commandPattern;
    final private Pattern ipPattern;

    private static SimpleDateFormat GMT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        GMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public void setVideoStream(VideoStream stream) {
        mVideoStream = stream;
    }

    public HttpServer(final int port) {
        super(port);

        commandPattern = Pattern.compile("^\\/command\\/([a-z]*)$");
        ipPattern = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);
        mCommands = new HashMap<String, Integer>();
    }

    public void setContext(Context context) {
        mContext = context;
        mControl = new Control(context);
    }

    public void addCommand(String name, int resId) {
        mCommands.put(name, resId);
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

    private Response indexResponse(IHTTPSession session) {

        if (lastUpdateTime == null) {
            try {
                PackageInfo localPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                lastUpdateTime = Milliseconds2GMT(localPackageInfo.lastUpdateTime);
            } catch (PackageManager.NameNotFoundException e) {
                ARCLog.e(e.getMessage());
            }
        }

        if (!isUpdate(session.getHeaders(), lastUpdateTime)) {
            return newFixedLengthResponse(Response.Status.NOT_MODIFIED, NanoHTTPD.MIME_PLAINTEXT, "Not Modified");
        }

        Resources r = mContext.getResources();
        InputStream is = r.openRawResource(R.raw.index);
        Response response = newChunkedResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, is);

        if (lastUpdateTime != null) {
            response.addHeader("Cache-control", "private");
            response.addHeader("Last-Modified", lastUpdateTime);
        }

        return response;
    }

    private Response executeCommand(String command) {

        Integer resId = mCommands.get(command);
        if (null == resId) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Bad Request");
        }

        // загрузка данных файла в память осуществляется в том же потоке что и сессия клиента...
        // возможно стоит запускать отдельный тред... хз...
        mControl.command(resId.intValue());
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, null);
    }

    @Override
    public void start() throws IOException {

        if (null != mVideoStream) {
            //mVideoStream.startPreview();
            mVideoStream.start();
        }

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

        if (!(Method.GET.equals(method) || Method.POST.equals(method))) {
            return newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED, NanoHTTPD.MIME_PLAINTEXT, "Not Implemented");
        }

        if (Method.GET.equals(method) && uri.equals("/")) {
            return indexResponse(session);
        }

        if (Method.GET.equals(method) && uri.equals("/stream")) {
            videoStream(session); // возможно надо в отдельном потоке
            return null;
        }

        if (Method.POST.equals(method)) {

            Matcher m = commandPattern.matcher(uri);
            if (m.matches()) {
                return executeCommand(m.group(1));
            }
        }

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
