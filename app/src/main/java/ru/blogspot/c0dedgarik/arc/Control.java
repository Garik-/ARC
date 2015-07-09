package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class Control implements Runnable {


    private Context mContext;
    private AudioTrack mAudioTrack;
    private LruCache<Integer, byte[]> mSamples;
    private int mLastCommand;
    private int mCommand;
    private Handler mHandler = new Handler();
    private HashMap<String, Integer> mCommands = new HashMap<String, Integer>();

    public Control(Context context) {
        mContext = context;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mSamples = new LruCache<Integer, byte[]>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, byte[] buf) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return buf.length / 1024;
            }
        };

    }

    public void clearChache() {
        mSamples.evictAll();
    }

    //public void addCommand(String name, int resId, int countFrames) {
    //    mCommands.put(name, resId);
    //    mCountFrames.put(resId, countFrames);
    //}

    public void addCommand(String name, int count) {
        mCommands.put(name, count);
    }

    public void command(final String name) {


        Integer cmd = mCommands.get(name);
        if (null != cmd) {
            command(cmd.intValue());
        }
    }

    public void stop() {
        if (null != mAudioTrack) {
            mAudioTrack.stop();
        }
    }

    public void command(final int command) {

        if (null != mAudioTrack) {

            if (mLastCommand != command) {
                mAudioTrack.stop(); // flush data!
            }

            switch (mAudioTrack.getPlayState()) {
                case AudioTrack.PLAYSTATE_PLAYING:
                    mAudioTrack.pause();
                    break;
                case AudioTrack.PLAYSTATE_PAUSED:
                    mAudioTrack.play();
                    break;
            }

        }

        if (null == mAudioTrack || mLastCommand != command) {
            //LoadTask lt = new LoadTask();
            //lt.execute(command);

            mCommand = command;
            mHandler.post(this); // in thread
            // ^ если делать в потоке, то при коротких нажатиях на клаву команды не успевают загрузится
            // и начинается гонка потоков в итоге команда уже не должна звучать а она продолжает

        }
    }

    private byte[] getTrack(final int resId) throws IOException {

        byte[] buf = mSamples.get(resId);

        if (null == buf) {

            Resources r = mContext.getResources();
            InputStream is = r.openRawResource(resId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                baos.write(i);
                i = is.read();
            }
            is.close();

            buf = baos.toByteArray();

            mSamples.put(resId, buf);
            ARCLog.d("Cache add %d", resId);
        } else {
            ARCLog.d("Cache get %d", resId);
        }

        return buf;
    }

    private AudioTrack createAudioTrack(final WAV sample) {
        int audioFormat, channel;
        switch (sample.getBitsPerSample()) {
            case 16:
                audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case 8:
                audioFormat = AudioFormat.ENCODING_PCM_8BIT;
                break;
            default:
                ARCLog.e("audioFormat, bitsPerSample %d", sample.getBitsPerSample());
                return null;  // exception
        }

        switch (sample.getNumChannels()) {
            case 1:
                channel = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channel = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                ARCLog.e("channel, numChannels %d", sample.getNumChannels());
                return null;
        }

        final int minBufferSize = AudioTrack.getMinBufferSize(sample.getSampleRate(), channel, audioFormat);

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sample.getSampleRate(), channel, audioFormat, minBufferSize < sample.size() ? sample.size() : minBufferSize,
                AudioTrack.MODE_STATIC); // ! важно

        audioTrack.write(sample.toByteArray(), 0, minBufferSize < sample.size() ? sample.size() : minBufferSize);
        audioTrack.setLoopPoints(0, sample.getCountFrames(), -1);

        return audioTrack;
    }

    @Override
    public void run() {
        try {
            //WAV sample = new WAV(getTrack(mCommand));
            WAV sample = new WAVGen(mCommand);

            AudioTrack audioTrack = createAudioTrack(sample);

            if (null == audioTrack) {
                ARCLog.e("null audioTrack");
                return; // !!!
            }

            mAudioTrack = audioTrack;
            mLastCommand = mCommand;

            mAudioTrack.play();

        } catch (IOException e) {
            ARCLog.e(e.getMessage());
        }
    }


    private interface WAV {
        public int getSampleRate();

        public short getBitsPerSample();

        public short getNumChannels();

        public short getBlockAlign();

        public int getCountFrames();

        public byte[] toByteArray();

        public int size();
    }

    final private class WAVGen implements WAV {

        private static final int W2 = 76;
        private static final int W1 = 26;
        private static final short HIGH = 32767;
        private static final short LOW = -32767;


        private ByteArrayOutputStream buffer;
        private byte[] pcmdata;

        public WAVGen(final int command) throws IOException {

            buffer = new ByteArrayOutputStream();
            pcmdata = mSamples.get(command);

            if (pcmdata == null) {

                genMeandr(W2, W1, 4);
                genMeandr(W1, W1, command);

                pcmdata = buffer.toByteArray();
                mSamples.put(command, pcmdata);

                ARCLog.d("Cache add %d", command);
            } else {
                ARCLog.d("Cache get %d", command);
            }

        }

        private void write(final short data) {
            buffer.write((byte) (data & 0xff));
            buffer.write((byte) ((data >> 8) & 0xff));
        }

        private void genMeandr(int lowLenght, int highLength, int count) {

            for (int j = 0; j < count; j++) {

                for (int i = 0; i < lowLenght; i++) {
                    write(LOW);
                }

                for (int i = 0; i < highLength; i++) {
                    write(HIGH);
                }
            }
        }


        @Override
        public int getSampleRate() {
            return 44100;
        }

        @Override
        public short getBitsPerSample() {
            return 16;
        }

        @Override
        public short getNumChannels() {
            return 1;
        }

        @Override
        public short getBlockAlign() {
            return (short) (getNumChannels() * (getBitsPerSample() / 8));
        }

        @Override
        public int getCountFrames() {
            // return (buffer.size() - mCountFrames.get(mCommand)) / getBlockAlign();
            return (pcmdata.length / getBlockAlign());
        }

        @Override
        public int size() {
            return pcmdata.length;
        }

        @Override
        public byte[] toByteArray() {
            return pcmdata;
        }
    }

    final private class WAVFile implements WAV { // http://audiocoding.ru/article/2008/05/22/wav-file-structure.html
        private byte[] pcmdata;
        private int sampleRate = 0;

        public WAVFile(byte[] buf) {
            pcmdata = buf;
        }

        @Override
        public int getSampleRate() {
            if (0 == sampleRate) {
                byte[] arr = {pcmdata[27], pcmdata[26], pcmdata[25], pcmdata[24]};
                ByteBuffer wrapped = ByteBuffer.wrap(arr);
                sampleRate = wrapped.getInt();
            }
            return sampleRate;
        }

        @Override
        public short getBitsPerSample() {
            return (short) ((pcmdata[35] << 8) + (pcmdata[34] & 0xff));
        }

        @Override
        public short getNumChannels() {
            return (short) ((pcmdata[23] << 8) + (pcmdata[22] & 0xff));
        }

        @Override
        public short getBlockAlign() {
            return (short) ((pcmdata[33] << 8) + (pcmdata[32] & 0xff));
        }

        @Override
        public int getCountFrames() {

            int count = pcmdata.length / getBlockAlign();
            return count;  // countFrames = fileSize / blockAlign;
        }

        @Override
        public byte[] toByteArray() {
            return pcmdata;
        }

        @Override
        public int size() {
            return pcmdata.length;
        }
    }
}
