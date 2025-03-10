/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.tools.isotopepatternpreview;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.validation.constraints.NotNull;
import org.jfree.data.xy.XYDataset;

public class IsotopePatternPreviewTask extends AbstractTask {

  private static final double _2SQRT_2LN2 = 2 * Math.sqrt(2 * Math.log(2));

  SimpleIsotopePattern pattern;
  IsotopePatternPreviewDialog dialog;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private String message;
  private boolean parametersChanged;
  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType polarity;
  private String formula;
  private boolean displayResult;

  public IsotopePatternPreviewTask() {
    super(null, Instant.now()); // date irrelevant, not in batch mode
    message = "Wating for parameters";
    parametersChanged = false;
    formula = "";
    minIntensity = 0.d;
    mergeWidth = 0.01d;
    charge = 0;
    pattern = null;
  }

  public IsotopePatternPreviewTask(String formula, double minIntensity, double mergeWidth,
      int charge, PolarityType polarity, IsotopePatternPreviewDialog dialog) {
    super(null, Instant.now());
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    this.dialog = dialog;
    setStatus(TaskStatus.WAITING);
    parametersChanged = true;
    pattern = null;
    displayResult = true;
    message = "Calculating isotope pattern " + formula + ".";
  }

  public void initialise(String formula, double minIntensity, double mergeWidth, int charge,
      PolarityType polarity) {
    message = "Wating for parameters";
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    parametersChanged = true;
    pattern = null;
    displayResult = true;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    assert mergeWidth > 0d;

    message = "Calculating isotope pattern " + formula + ".";
    pattern = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(formula,
        minIntensity, mergeWidth, charge, polarity, true);
    if (pattern == null) {
      logger.warning("Isotope pattern could not be calculated.");
      setStatus(TaskStatus.FINISHED);
      return;
    }
    logger.finest("Pattern " + pattern.getDescription() + " calculated.");

    if (displayResult) {
      updateWindow();
      startNextThread();
    }
    setStatus(TaskStatus.FINISHED);
  }

  public void updateWindow() {
    final XYDataset fit = gaussianIsotopePatternFit(pattern, pattern.getBasePeakMz() / mergeWidth);
    Platform.runLater(() -> {
      dialog.updateChart(pattern, fit);
      dialog.updateTable(pattern);
    });
  }

  public void startNextThread() {
    Platform.runLater(() -> dialog.startNextThread());
  }

  public void setDisplayResult(boolean val) {
    this.displayResult = val;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  /**
   * f(x) = a * e^( -(x-b)/(2(c^2)) )
   *
   * @param pattern
   * @param resolution
   */
  public XYDataset gaussianIsotopePatternFit(@NotNull final IsotopePattern pattern,
      final double resolution) {

    final Double basePeakMz = pattern.getBasePeakMz();
    if (basePeakMz == null) {
      return null;
    }

    final double fwhm = basePeakMz / resolution;
    final double stepSize = fwhm / 10d;
    final double mzOffset = 1d;

    final Range<Double> mzRange = pattern.getDataPointMZRange();
    final int numPoints = (int) (
        ((mzRange.upperEndpoint() + mzOffset) - (mzRange.lowerEndpoint() - mzOffset)) / stepSize);

    TDoubleArrayList tempMzs = new TDoubleArrayList();

    for (int i = 0; i < numPoints; i++) {
      double mz = mzRange.lowerEndpoint() - mzOffset + i * stepSize;
      for (int j = 0; j < pattern.getNumberOfDataPoints(); j++) {
        // no need to calc otherwise
        if (Math.abs(mz - pattern.getMzValue(j)) < (5 * fwhm)) {
          tempMzs.add(mz);
        }
      }
    }

    final double[] mzs = tempMzs.toArray();
    final double c = fwhm / _2SQRT_2LN2;
    final double[] intensities = new double[mzs.length];
    Arrays.fill(intensities, 0d);

    double highest = 1d;
    for (int i = 0; i < mzs.length; i++) {
      final double gridMz = mzs[i];

      for (int j = 0; j < pattern.getNumberOfDataPoints(); j++) {
        final double isotopeMz = pattern.getMzValue(j);
        final double isotopeIntensity = pattern.getIntensityValue(j);

        final double gauss = getGaussianValue(gridMz, isotopeMz, c);
        intensities[i] += isotopeIntensity * gauss;
      }

      highest = Math.max(highest, intensities[i]);
    }

    final Double basePeakIntensity = Objects.requireNonNullElse(pattern.getBasePeakIntensity(), 1d);
    final DataPoint[] dataPoints = new DataPoint[mzs.length];
    for (int i = 0; i < dataPoints.length; i++) {
      intensities[i] = intensities[i] / highest * basePeakIntensity;
      dataPoints[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }
    return new ColoredXYDataset(
        new AnyXYProvider(MZmineCore.getConfiguration().getDefaultColorPalette().getAWT(1),
            "Gaussian fit; Resolution = " + (int) (resolution), dataPoints.length,
            i -> dataPoints[i].getMZ(), i -> dataPoints[i].getIntensity()));
  }

  /**
   * f(x) = 1 * e^( -(x-b)/(2(c^2)) )
   *
   * @param x the x value.
   * @param b the center of the curve.
   * @param c the sigma of the curve.
   * @return The f(x) value for x.
   */
  public double getGaussianValue(final double x, final double b, final double c) {
    return Math.exp(-Math.pow((x - b), 2) / (2 * (Math.pow(c, 2))));
  }
}
