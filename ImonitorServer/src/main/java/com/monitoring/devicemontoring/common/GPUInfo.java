package com.monitoring.devicemontoring.common;

import java.io.Serializable;

/**
 * Data transfer object representing a Graphics Processing Unit (GPU).
 *
 * <p>This class stores basic hardware information about a detected GPU, such as its name, vendor,
 * and total Video RAM (VRAM).
 *
 * @author Devicelytics Team
 */
public class GPUInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The model name of the GPU (e.g., "NVIDIA GeForce RTX 3080"). */
  public String name;

  /** The manufacturer/vendor of the GPU (e.g., "NVIDIA", "AMD"). */
  public String vendor;

  /** The total Video RAM in bytes. May be 0 if the information is unavailable. */
  public long vram;

  /** Default constructor for serialization. */
  public GPUInfo() {}

  /**
   * Constructs a new GPUInfo object with specified hardware details.
   *
   * @param name the model name of the GPU
   * @param vendor the manufacturer of the GPU
   * @param vram the total Video RAM in bytes
   */
  public GPUInfo(String name, String vendor, long vram) {
    this.name = name;
    this.vendor = vendor;
    this.vram = vram;
  }

  /**
   * Gets the GPU model name.
   *
   * @return the name string
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the GPU vendor name.
   *
   * @return the vendor string
   */
  public String getVendor() {
    return vendor;
  }

  /**
   * Gets the total Video RAM.
   *
   * @return total VRAM in bytes
   */
  public long getVram() {
    return vram;
  }
}
