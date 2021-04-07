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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.gui.py4j;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class PythonController {

  public MZmineProject getProject() {
    return MZmineCore.getProjectManager().getCurrentProject();
  }

  public List<FeatureList> getFeatureLists() {
    return getProject().getFeatureLists();
  }

  public int getNumFeatureLists() {
    return getFeatureLists().size();
  }

  /**
   * Access data points
   *
   * @param row feature list row
   * @param raw raw data file
   * @return list of data points
   */
  public FeatureData getFeatureData(FeatureListRow row, RawDataFile raw,
      boolean includeZeros) {
    FeatureDataType dataType =
        includeZeros ? FeatureDataType.INCLUDE_ZEROS : FeatureDataType.ONLY_DETECTED;
    FeatureDataAccess data = EfficientDataAccess.of(row, dataType, raw);

    if (data.hasNextFeature()) {
      data.nextFeature();

      int size = data.getNumberOfValues();
      double[] rts = new double[size];
      double[] intensities = new double[size];
      double[] mzs = new double[size];
      for (int i = 0; i < data.getNumberOfValues(); i++) {
        mzs[i] = data.getMZ(i);
        intensities[i] = data.getIntensity(i);
        rts[i] = data.getRetentionTime(i);
      }
      return new FeatureData(rts, intensities, mzs);
    }
    return null;
  }

  /**
   * All MS2 spectra
   *
   * @param row feature list row
   * @return List of all fragmentation spectra
   */
  public List<Scan> getMS2Spectra(FeatureListRow row) {
    return row.getAllMS2Fragmentations();
  }


  public int getNumRawDataFiles() {
    return getRawDataFiles().size();
  }

  public List<RawDataFile> getRawDataFiles() {
    return getProject().getRawDataFiles();
  }

  public RawDataFile getRawDataFile(String name) {
    return getRawDataFiles().stream().filter(raw -> name.equals(raw.getName())).findFirst()
        .orElse(null);
  }

}
