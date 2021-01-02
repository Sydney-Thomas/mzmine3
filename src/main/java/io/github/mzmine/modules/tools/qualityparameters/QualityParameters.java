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

package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.features.Feature;
import java.util.List;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import javafx.beans.property.Property;

/**
 * Calculates quality parameters for each peak in a feature list: - Full width at half maximum
 * (FWHM) - Tailing Factor - Asymmetry factor
 */
public class QualityParameters {

  public static void calculateAndSetModularQualityParameters(ModularFeatureList flist) {
    // add quality columns to flist - feature columns
    flist.addFeatureType(new FwhmType(), new AsymmetryFactorType(), new TailingFactorType());

    flist.modularStreamFeatures().forEach(peak -> {
      Property<Float> height = peak.getHeightProperty();
      Property<Float> rt = peak.getRTProperty();

      List<Integer> scanNumbers = peak.getScanNumbersProperty();
      RawDataFile dataFile = peak.getRawDataFile();
      List<DataPoint> dps = peak.getDataPointsProperty();
      if (height.getValue() == null || rt.getValue() == null || dataFile == null
          || scanNumbers.isEmpty() || dps.isEmpty())
        return;

      Range<Float> rtRange = peak.get(RTRangeType.class).getValue();
      if (rtRange == null)
        rtRange = Range.singleton(rt.getValue());

      height = peak.getHeightProperty();
      rt = peak.getRTProperty();

      // FWHM
      double rtValues[] =
          peakFindRTs(height.getValue() / 2.0, rt.getValue(), scanNumbers, dps, dataFile, rtRange);
      Double fwhm = rtValues[1] - rtValues[0];
      if (fwhm <= 0 || Double.isNaN(fwhm) || Double.isInfinite(fwhm)) {
        fwhm = null;
      }
      if (fwhm != null)
        peak.set(FwhmType.class, (fwhm.floatValue()));

      // Tailing Factor - TF
      double rtValues2[] =
          peakFindRTs(height.getValue() * 0.05, rt.getValue(), scanNumbers, dps, dataFile, rtRange);
      Double tf = (rtValues2[1] - rtValues2[0]) / (2 * (rt.getValue() - rtValues2[0]));
      if (tf <= 0 || Double.isNaN(tf) || Double.isInfinite(tf)) {
        tf = null;
      }
      if (tf != null)
        peak.set(TailingFactorType.class, (tf.floatValue()));

      // Asymmetry factor - AF
      double rtValues3[] =
          peakFindRTs(height.getValue() * 0.1, rt.getValue(), scanNumbers, dps, dataFile, rtRange);
      Double af = (rtValues3[1] - rt.getValue()) / (rt.getValue() - rtValues3[0]);
      if (af <= 0 || Double.isNaN(af) || Double.isInfinite(af)) {
        af = null;
      }
      if (af != null)
        peak.set(AsymmetryFactorType.class, (af.floatValue()));
    });
  }

  public static float calculateFWHM(Feature feature) {
    if(feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Integer> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    List<DataPoint> dps = feature.getDataPoints();
    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || dps.isEmpty())
      throw new IllegalArgumentException("Modular feature values are not initialized.");

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null)
      rtRange = Range.singleton(rt);

    height = feature.getHeight();
    rt = feature.getRT();

    // FWHM
    double[] rtValues =
        peakFindRTs(height / 2.0, rt, scanNumbers, dps, dataFile, rtRange);
    double fwhm = rtValues[1] - rtValues[0];
    if (fwhm <= 0 || Double.isInfinite(fwhm)) {
      return Float.NaN;
    }

    return (float) fwhm;
  }

  public static float calculateTailingFactor(Feature feature) {
    if(feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Integer> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    List<DataPoint> dps = feature.getDataPoints();
    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || dps.isEmpty())
      throw new IllegalArgumentException("Modular feature values are not initialized.");

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null)
      rtRange = Range.singleton(rt);

    height = feature.getHeight();
    rt = feature.getRT();

    // Tailing Factor - TF
    double[] rtValues =
        peakFindRTs(height * 0.05, rt, scanNumbers, dps, dataFile, rtRange);
    double tf = (rtValues[1] - rtValues[0]) / (2 * (rt - rtValues[0]));
    if (tf <= 0 || Double.isInfinite(tf)) {
      return Float.NaN;
    }

    return (float) tf;
  }

  public static float calculateAsymmetryFactor(Feature feature) {
    if(feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Integer> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    List<DataPoint> dps = feature.getDataPoints();
    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || dps.isEmpty())
      throw new IllegalArgumentException("Modular feature values are not initialized.");

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null)
      rtRange = Range.singleton(rt);

    height = feature.getHeight();
    rt = feature.getRT();


    // Asymmetry factor - AF
    double[] rtValues =
        peakFindRTs(height * 0.1, rt, scanNumbers, dps, dataFile, rtRange);
    double af = (rtValues[1] - rt) / (rt - rtValues[0]);
    if (af <= 0 || Double.isInfinite(af)) {
      af = Double.NaN;
    }

    return (float) af;
  }

  private static double[] peakFindRTs(double intensity, float featureRT, List<Integer> scanNumbers,
      List<DataPoint> dps, RawDataFile dataFile, Range<Float> rtRange) {
    return peakFindRTs(intensity, featureRT,
        scanNumbers.stream().mapToInt(i -> i.intValue()).toArray(), dps.toArray(DataPoint[]::new), //
        dataFile, //
        Range.closed(rtRange.lowerEndpoint().doubleValue(), rtRange.upperEndpoint().doubleValue()));
  }

  private static double[] peakFindRTs(double intensity, double featureRT, int[] scanNumbers,
      DataPoint[] dps, RawDataFile dataFile, Range<Double> rtRange) {

    assert scanNumbers != null;
    assert dps != null;
    assert dataFile != null;
    assert rtRange != null;

    double x1 = 0, x2 = 0, x3 = 0, x4 = 0, y1 = 0, y2 = 0, y3 = 0, y4 = 0;
    double lastDiff1 = intensity;
    double lastDiff2 = intensity;
    double currentDiff;
    double currentRT;

    // Find the data points closet to input intensity on both side of the
    // peak apex
    DataPoint lastDP = dps[0];
    double lastRT = dataFile.getScan(scanNumbers[0]).getRetentionTime();
    DataPoint dp = dps[1];
    double rt = dataFile.getScan(scanNumbers[1]).getRetentionTime();
    for (int i = 1; i < scanNumbers.length - 1; i++) {
      DataPoint nextDP = dps[i + 1];
      double nextRT = dataFile.getScan(scanNumbers[i + 1]).getRetentionTime();

      if (dp != null) {
        currentDiff = Math.abs(intensity - dp.getIntensity());
        currentRT = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        if (currentDiff < lastDiff1 && currentDiff > 0 && currentRT <= featureRT
            && nextDP != null) {
          x1 = rt;
          y1 = dp.getIntensity();
          x2 = nextRT;
          y2 = nextDP.getIntensity();
          lastDiff1 = currentDiff;
        } else if (currentDiff < lastDiff2 && currentDiff > 0 && currentRT >= featureRT
            && lastDP != null) {
          x3 = lastRT;
          y3 = lastDP.getIntensity();
          x4 = rt;
          y4 = dp.getIntensity();
          lastDiff2 = currentDiff;
        }
      }
      // set to next
      lastDP = dp;
      lastRT = rt;
      dp = nextDP;
      rt = nextRT;
    }

    // Calculate RT value for input intensity based on linear regression
    double slope, intercept, rt1, rt2;
    if (y1 > 0) {
      slope = (y2 - y1) / (x2 - x1);
      intercept = y1 - (slope * x1);
      rt1 = (intensity - intercept) / slope;
    } else if (x2 > 0) { // Straight drop of peak to 0 intensity
      rt1 = x2;
    } else {
      rt1 = rtRange.lowerEndpoint();
    }
    if (y4 > 0) {
      slope = (y4 - y3) / (x4 - x3);
      intercept = y3 - (slope * x3);
      rt2 = (intensity - intercept) / slope;
    } else if (x3 > 0) { // Straight drop of peak to 0 intensity
      rt2 = x3;
    } else {
      rt2 = rtRange.upperEndpoint();
    }

    return new double[] {rt1, rt2};
  }

}
