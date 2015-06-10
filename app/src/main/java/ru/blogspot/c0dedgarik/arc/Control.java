package ru.blogspot.c0dedgarik.arc;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class Control {


    private Context mContext;
    private AudioTrack mAudioTrack;
    private LruCache<Integer, byte[]> mSamples;
    private int mLastCommand;


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

            execute(command);
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

        audioTrack.write(sample.toByteArray(), 0, sample.size());
        audioTrack.setLoopPoints(0, sample.getCountFrames(), -1);

        return audioTrack;
    }

    private void execute(int command) {
        try {
            WAV sample = new WAV(getTrack(command));
            AudioTrack audioTrack = createAudioTrack(sample);

            if (null == audioTrack) {
                ARCLog.e("null audioTrack");
                return; // !!!
            }

            mAudioTrack = audioTrack;
            mLastCommand = command;

            mAudioTrack.play();

        } catch (IOException e) {
            ARCLog.e(e.getMessage());
        }
    }

    /*
    private class LoadTask extends AsyncTask<Integer, Void, AudioTrack> {

        private int command;

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

        protected AudioTrack doInBackground(Integer... params) {

            command = params[0];

            try {

                WAV sample = new WAV(getTrack(command));

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

                audioTrack.write(sample.toByteArray(), 0, sample.size());
                audioTrack.setLoopPoints(0, sample.getCountFrames(), -1);

                return audioTrack;

            } catch (IOException e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(AudioTrack audioTrack) {
            if (null == audioTrack) {
                ARCLog.e("null audioTrack");
                return; // !!!
            }

            mAudioTrack = audioTrack;
            mLastCommand = command;

            mAudioTrack.play();
        }



    }*/

    final private class WAV { // http://audiocoding.ru/article/2008/05/22/wav-file-structure.html
        private byte[] pcmdata;
        private int sampleRate = 0;

        public WAV(byte[] buf) {
            pcmdata = buf;
        }

        public int getSampleRate() {
            if (0 == sampleRate) {
                byte[] arr = {pcmdata[27], pcmdata[26], pcmdata[25], pcmdata[24]};
                ByteBuffer wrapped = ByteBuffer.wrap(arr);
                sampleRate = wrapped.getInt();
            }
            return sampleRate;
        }

        public short getBitsPerSample() {
            return (short) ((pcmdata[35] << 8) + (pcmdata[34] & 0xff));
        }

        public short getNumChannels() {
            return (short) ((pcmdata[23] << 8) + (pcmdata[22] & 0xff));
        }

        public short getBlockAlign() {
            return (short) ((pcmdata[33] << 8) + (pcmdata[32] & 0xff));
        }

        public int getCountFrames() {
            return (pcmdata.length - 44) / getBlockAlign(); // countFrames = subchunk2Size / blockAlign;
        }

        public byte[] toByteArray() {
            return pcmdata;
        }

        public int size() {
            return pcmdata.length;
        }
    }
}
