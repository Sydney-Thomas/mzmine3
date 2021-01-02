/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General License as published by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * License for more details.
 *
 * You should have received a copy of the GNU General License along with MZmine; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.util.MemoryMapStorage;
import javafx.beans.property.ObjectProperty;

public interface RawDataFile {

  @Nonnull
  RawDataFile clone() throws CloneNotSupportedException;

  /**
   * Returns the name of this data file (can be a descriptive name, not necessarily the original
   * file name)
   */
  @Nonnull
  String getName();

  /**
   * Change the name of this data file
   */
  void setName(@Nonnull String name);

  int getNumOfScans();

  int getNumOfScans(int msLevel);

  /**
   * Returns sorted array of all MS levels in this file
   */
  @Nonnull
  int[] getMSLevels();

  /**
   * Returns sorted array of all scan numbers in this file
   *
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  int[] getScanNumbers();

  /**
   * Returns sorted array of all scan numbers in given MS level
   *
   * @param msLevel MS level
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  int[] getScanNumbers(int msLevel);

  /**
   * Returns sorted array of all scan numbers in given MS level and retention time range
   *
   * @param msLevel MS level
   * @param rtRange Retention time range
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  int[] getScanNumbers(int msLevel, @Nonnull Range<Float> rtRange);

  /**
   * Scan could be null if scanID is not contained in the raw data file
   *
   * @param scan Desired scan number
   * @return Desired scan
   */
  @Nullable
  Scan getScan(int scan);

  /**
   * @param rt The rt
   * @param mslevel The ms level
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   *         the rt range of this file.
   */
  int getScanNumberAtRT(float rt, int mslevel);

  /**
   * @param rt The rt
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   *         the rt range of this file.
   */
  int getScanNumberAtRT(float rt);

  @Nonnull
  Range<Double> getDataMZRange();

  @Nonnull
  Range<Float> getDataRTRange();

  @Nonnull
  Range<Double> getDataMZRange(int msLevel);

  @Nonnull
  Range<Float> getDataRTRange(Integer msLevel);

  double getDataMaxBasePeakIntensity(int msLevel);

  double getDataMaxTotalIonCurrent(int msLevel);

  /**
   * Returns a list of the different scan polarity types found in the raw data file.
   *
   * @return Scan polarity types.
   */
  @Nonnull
  List<PolarityType> getDataPolarity();

  java.awt.Color getColorAWT();

  javafx.scene.paint.Color getColor();

  void setColor(javafx.scene.paint.Color color);

  ObjectProperty<javafx.scene.paint.Color> colorProperty();

  /**
   * Close the file in case it is removed from the project
   */
  void close();

  @Nonnull
  MemoryMapStorage getMemoryMapStorage();

  void addScan(Scan newScan) throws IOException;

  void setRTRange(int msLevel, Range<Float> rtRange);

  void setMZRange(int msLevel, Range<Double> mzRange);


}
