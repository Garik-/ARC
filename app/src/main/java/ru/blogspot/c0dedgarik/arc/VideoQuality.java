
package ru.blogspot.c0dedgarik.arc;

/**
 * A class that represents the quality of a video stream.
 * It contains the resolution, the framerate (in fps) and the bitrate (in bps) of the stream.
 */
public class VideoQuality {

    /**
     * Default video stream quality.
     */
    public final static VideoQuality DEFAULT_VIDEO_QUALITY = new VideoQuality(640, 480, 50, 90);

    /**
     * Represents a quality for a video stream.
     */
    public VideoQuality() {
    }


    public VideoQuality(int resX, int resY, int quality, int orientation) {
        this.quality = quality;
        this.resX = resX;
        this.resY = resY;
        this.orientation = orientation;
    }

    public VideoQuality(String resolution, int quality, int orientation) {
        this.quality = quality;
        this.orientation = orientation;
        parseResolution(resolution);
    }

    public void parseResolution(String resolution) {
        String[] tokens = resolution.split("x");
        this.resX = Integer.parseInt(tokens[0]);
        this.resY = Integer.parseInt(tokens[1]);
    }


    public int quality = 0;
    public int resX = 0;
    public int resY = 0;
    public int orientation = 90;

    public boolean equals(VideoQuality quality) {
        if (quality == null) return false;
        return (quality.resX == this.resX &
                quality.resY == this.resY &
                quality.quality == this.quality &
                quality.orientation == this.orientation);
    }

    public VideoQuality clone() {
        return new VideoQuality(resX, resY, quality, orientation);
    }

}
