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

package io.github.mzmine.datamodel.data;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakInformation;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import java.util.List;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface representing feature list row
 */
public interface FeatureListRow {

  /**
   * Return raw data with peaks on this row
   */
  public List<RawDataFile> getRawDataFiles();

  /**
   * Returns ID of this row
   */
  public int getID();

  /**
   * Returns number of peaks assigned to this row
   */
  public int getNumberOfPeaks();

  /**
   * Return peaks assigned to this row
   */
  public ObservableList<Feature> getFeatures();

  /**
   * Returns peak for given raw data file
   */
  public Feature getPeak(RawDataFile rawData);

  /**
   * Add a peak
   */
  public void addPeak(RawDataFile rawData, Feature peak);

  /**
   * D Remove a peak
   */
  public void removePeak(RawDataFile file);

  /**
   * Has a peak?
   */
  public boolean hasFeature(Feature peak);

  /**
   * Has a peak?
   */
  public boolean hasFeature(RawDataFile rawData);

  /**
   * Returns average M/Z for peaks on this row
   */
  public double getAverageMZ();

  /**
   * Returns average RT for peaks on this row
   */
  public float getAverageRT();

  /**
   * Returns average height for peaks on this row
   */
  public double getAverageHeight();

  /**
   * Returns the charge for peak on this row. If more charges are found 0 is returned
   */
  public int getRowCharge();

  /**
   * Returns average area for peaks on this row
   */
  public double getAverageArea();

  /**
   * Returns comment for this row
   */
  public String getComment();

  /**
   * Sets comment for this row
   */
  public void setComment(String comment);

  /**
   * Sets average mz for this row
   */
  public void setAverageMZ(double averageMZ);

  /**
   * Sets average rt for this row
   */
  public void setAverageRT(float averageRT);

  /**
   * Add a new identity candidate (result of identification method)
   *
   * @param identity New peak identity
   * @param preffered boolean value to define this identity as preferred identity
   */
  public void addPeakIdentity(PeakIdentity identity, boolean preffered);

  /**
   * Remove identity candidate
   *
   * @param identity Peak identity
   */
  public void removePeakIdentity(PeakIdentity identity);

  /**
   * Returns all candidates for this peak's identity
   *
   * @return Identity candidates
   */
  public PeakIdentity[] getPeakIdentities();

  /**
   * Returns preferred peak identity among candidates
   *
   * @return Preferred identity
   */
  public PeakIdentity getPreferredPeakIdentity();

  /**
   * Sets a preferred peak identity among candidates
   *
   * @param identity Preferred identity
   */
  public void setPreferredPeakIdentity(PeakIdentity identity);

  /**
   * Adds a new PeakInformation object.
   *
   * PeakInformation is used to keep extra information about peaks in the form of a map
   * <propertyName, propertyValue>
   *
   * @param peakInformation object
   */

  public void setPeakInformation(PeakInformation peakInformation);

  /**
   * Returns PeakInformation
   *
   * @return
   */

  public PeakInformation getPeakInformation();

  /**
   * Returns maximum raw data point intensity among all peaks in this row
   *
   * @return Maximum intensity
   */
  public double getMaxDataPointIntensity();

  /**
   * Returns the most intense peak in this row
   */
  public Feature getBestPeak();

  /**
   * Returns the most intense fragmentation scan in this row
   */
  public Scan getBestFragmentation();

  /**
   * Returns all fragmentation scans of this row
   */
  @Nonnull
  public Scan[] getAllMS2Fragmentations();

  /**
   * Returns the most intense isotope pattern in this row. If there are no isotope patterns present
   * in the row, returns null.
   */
  public IsotopePattern getBestIsotopePattern();

  // DorresteinLaB edit
  /**
   * reset the rowID
   */
  public void setID(int id);

  @Nullable
  FeatureList getFeatureList();

  void setFeatureList(@Nonnull FeatureList flist);

  // End DorresteinLab edit
}
