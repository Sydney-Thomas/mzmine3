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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;


public class SimpleImagingScan extends SimpleScan implements ImagingScan {

  private Coordinates coordinates;

  public SimpleImagingScan(RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, double mzValues[], double intensityValues[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, Coordinates coordinates) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, mzValues,
        intensityValues, spectrumType, polarity, scanDefinition, scanMZRange);
    this.setCoordinates(coordinates);
  }

  /**
   *
   * @return the xyz coordinates. null if no coordinates were specified
   */
  @Override
  public Coordinates getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(Coordinates coordinates) {
    this.coordinates = coordinates;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleImagingScan other = (SimpleImagingScan) obj;
    if (coordinates == null) {
      if (other.coordinates != null)
        return false;
    } else if (!coordinates.equals(other.coordinates))
      return false;
    return true;
  }

}
