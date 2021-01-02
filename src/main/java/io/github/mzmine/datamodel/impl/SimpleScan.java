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

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan extends AbstractStorableSpectrum implements Scan {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;

  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private MassSpectrumType spectrumType;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;
  private ObservableList<MassList> massLists = FXCollections.observableArrayList();

  /**
   * Clone constructor
   */

  public SimpleScan(@Nonnull RawDataFile dataFile, Scan sc) {

    this(dataFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc.getPrecursorMZ(),
        sc.getPrecursorCharge(), null, null, sc.getSpectrumType(), sc.getPolarity(),
        sc.getScanDefinition(), sc.getScanningMZRange());
  }


  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, double mzValues[], double intensityValues[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    super(dataFile.getMemoryMapStorage());

    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.precursorMZ = precursorMZ;
    this.spectrumType = spectrumType;
    this.precursorCharge = precursorCharge;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;
    if (mzValues != null && intensityValues != null)
      setDataPoints(mzValues, intensityValues);

  }


  /**
   * @see io.github.mzmine.datamodel.Scan#getScanNumber()
   */
  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  /**
   * @param scanNumber The scanNumber to set.
   */
  public void setScanNumber(int scanNumber) {
    this.scanNumber = scanNumber;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getMSLevel()
   */
  @Override
  public int getMSLevel() {
    return msLevel;
  }

  /**
   * @param msLevel The msLevel to set.
   */
  public void setMSLevel(int msLevel) {
    this.msLevel = msLevel;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getPrecursorMZ()
   */
  @Override
  public double getPrecursorMZ() {
    return precursorMZ;
  }

  /**
   * @param precursorMZ The precursorMZ to set.
   */
  public void setPrecursorMZ(double precursorMZ) {
    this.precursorMZ = precursorMZ;
  }

  /**
   * @return Returns the precursorCharge.
   */
  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }

  /**
   * @param precursorCharge The precursorCharge to set.
   */
  public void setPrecursorCharge(int precursorCharge) {
    this.precursorCharge = precursorCharge;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#
   */
  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  /**
   * @param retentionTime The retentionTime to set.
   */
  public void setRetentionTime(float retentionTime) {
    this.retentionTime = retentionTime;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getSpectrumType()
   */
  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  @Override
  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  @Override
  public String toString() {
    return ScanUtils.scanToString(this, false);
  }

  @Override
  public synchronized void addMassList(final @Nonnull MassList massList) {

    // Remove all mass lists with same name, if there are any
    MassList currentMassLists[] = massLists.toArray(new MassList[0]);
    for (MassList ml : currentMassLists) {
      if (ml.getName().equals(massList.getName())) {
        removeMassList(ml);
      }
    }

    // Add the new mass list
    massLists.add(massList);

  }

  @Override
  public synchronized void removeMassList(final @Nonnull MassList massList) {
    massLists.remove(massList);
  }

  @Override
  @Nonnull
  public MassList[] getMassLists() {
    return massLists.toArray(new MassList[0]);
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    for (MassList ml : massLists) {
      if (ml.getName().equals(name)) {
        return ml;
      }
    }
    return null;
  }


  @Override
  @Nonnull
  public RawDataFile getDataFile() {
    return dataFile;
  }


  @Override
  @Nonnull
  public PolarityType getPolarity() {
    if (polarity == null) {
      polarity = PolarityType.UNKNOWN;
    }
    return polarity;
  }

  @Override
  public String getScanDefinition() {
    if (scanDefinition == null) {
      scanDefinition = "";
    }
    return scanDefinition;
  }

  @Override
  @Nonnull
  public Range<Double> getScanningMZRange() {
    if (scanMZRange == null)
      scanMZRange = getDataPointMZRange();
    return scanMZRange;
  }

}

