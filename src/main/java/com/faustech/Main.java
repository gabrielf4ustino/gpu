package com.faustech;

import com.faustech.gpu.GPU;
import com.faustech.gpu.VideoFrameToVertexArray;
import com.faustech.memory.FrameBuffer;
import lombok.extern.java.Log;

@Log
public class Main {

  private static final int WIDTH = 1080;

  private static final int HEIGHT = 720;

  private static final int FRAME_BUFFER_SIZE = WIDTH * HEIGHT * 4; // 4 bytes per pixel

  public static void main(String[] args) {

    final FrameBuffer frameBuffer = new FrameBuffer(FRAME_BUFFER_SIZE);

    if (args.length < 1) {
      throw new IllegalArgumentException("Video file path not provided.");
    }

    final VideoFrameToVertexArray videoFrameToVertexArray =
        new VideoFrameToVertexArray(args[0], WIDTH, HEIGHT, frameBuffer);
    GPU gpu = new GPU(WIDTH, HEIGHT, frameBuffer);

    videoFrameToVertexArray.start();
    gpu.start();

    while (gpu.isAlive()) {
      if (gpu.getState() == Thread.State.TERMINATED) {
        gpu.interrupt();
        videoFrameToVertexArray.interrupt();
      }
    }
  }
}
