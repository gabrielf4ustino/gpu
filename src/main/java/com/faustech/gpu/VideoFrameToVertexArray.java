package com.faustech.gpu;

import com.faustech.memory.FrameBuffer;
import com.faustech.memory.MemoryException;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/** This thread processes a video file, converting frames to vertex arrays for rendering. */
@Log // Lombok annotation for logging
@RequiredArgsConstructor // Lombok generates a constructor for all final fields
public class VideoFrameToVertexArray extends Thread {

  private final String videoFilePath; // Path to the video file

  private final Java2DFrameConverter converter =
      new Java2DFrameConverter(); // Converter for frames to images

  private final int width; // Width of the target rendering

  private final int height; // Height of the target rendering

  private final FrameBuffer frameBuffer; // Frame buffer to write the converted frames

  /**
   * Resizes a BufferedImage to the specified dimensions.
   *
   * @param originalImage The original BufferedImage.
   * @param targetWidth The desired width.
   * @param targetHeight The desired height.
   * @return A new resized BufferedImage.
   */
  private static BufferedImage resizeImage(
      BufferedImage originalImage, int targetWidth, int targetHeight) {

    BufferedImage resizedImage =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = resizedImage.createGraphics();
    g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    g2d.dispose();
    return resizedImage;
  }

  /** Entry point for the thread; begins the video processing. */
  @Override
  public void run() {

    this.processVideo();
  }

  /** Processes each frame of the video, converting and writing to the frame buffer. */
  private void processVideo() {

    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFilePath)) {
      grabber.start();
      Frame frame;

      while ((frame = grabber.grabImage()) != null) {
        long time = System.currentTimeMillis();

        processFrameAndWriteInBuffer(frame);

        time = System.currentTimeMillis() - time;
        long sleepTime =
            Math.max(0, 1000 / 60 - time); // Calculate time to delay to maintain frame rate
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          log.severe(e.getMessage());
          return;
        }
      }
      grabber.stop();
      this.processVideo(); // Restart video processing to loop continuously
    } catch (Exception e) {
      throw new RuntimeException(String.format("Error processing video: %s", e.getMessage()));
    }
  }

  /**
   * Processes a single frame, resizing and mapping it into the frame buffer.
   *
   * @param frame The frame to be processed.
   * @throws MemoryException If there's an issue writing to the frame buffer.
   */
  private void processFrameAndWriteInBuffer(Frame frame) throws MemoryException {

    BufferedImage originalImage = converter.getBufferedImage(frame);
    BufferedImage resizedImage = resizeImage(originalImage, width, height);
    int address = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        float normX = (x / (float) width) * 2 - 1;
        float normY = ((height - y) / (float) height) * 2 - 1;

        int color = resizedImage.getRGB(x, y);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float u = x / (float) width;
        float v = y / (float) height;

        frameBuffer.writeToPixelBufferFromFloats(address * 4, new float[] {r, g, b, 1});

        frameBuffer.writeToVertexBufferFromFloats(
            (y * width + x) * 32, new float[] {normX, normY, r, g, b, 1, u, v});

        address += 4;
      }
    }
    frameBuffer.swap();
  }
}
