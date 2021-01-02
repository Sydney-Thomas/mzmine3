/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.Feature;
import org.jfree.data.xy.AbstractXYDataset;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;

public class ChromatogramTICDataSet extends AbstractXYDataset {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private Feature chromatogram;
  private RawDataFile dataFile;
  private int scanNumbers[];

  public ChromatogramTICDataSet(Feature chromatogram) {
    this.chromatogram = chromatogram;
    this.dataFile = chromatogram.getRawDataFile();
    this.scanNumbers = chromatogram.getScanNumbers().stream().mapToInt(i -> i).toArray();
  }

  public Comparable<?> getSeriesKey(int series) {
    return chromatogram.toString();
  }

  public int getItemCount(int series) {
    return scanNumbers.length;
  }

  public Number getX(int series, int index) {
    return dataFile.getScan(scanNumbers[index]).getRetentionTime();
  }

  public Number getY(int series, int index) {
    DataPoint mzPeak = chromatogram.getDataPoint(scanNumbers[index]);
    if (mzPeak == null)
      return 0;
    return mzPeak.getIntensity();
  }

  public int getSeriesCount() {
    return 1;
  }

}
