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

package io.github.mzmine.modules.dataprocessing.norm_standardcompound;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.norm_linear.LinearNormalizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StandardCompoundNormalizerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ModularFeatureList originalFeatureList, normalizedFeatureList;

  private int processedRows, totalRows;

  private String suffix;
  private StandardUsageType normalizationType;
  private FeatureMeasurementType featureMeasurementType;
  private double MZvsRTBalance;
  private boolean removeOriginal;
  private FeatureListRow[] standardRows;
  private ParameterSet parameters;

  public StandardCompoundNormalizerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters,  @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.originalFeatureList = (ModularFeatureList) featureList;

    suffix = parameters.getParameter(LinearNormalizerParameters.suffix).getValue();
    normalizationType =
        parameters.getParameter(StandardCompoundNormalizerParameters.standardUsageType).getValue();
    featureMeasurementType = parameters
        .getParameter(StandardCompoundNormalizerParameters.featureMeasurementType).getValue();
    MZvsRTBalance =
        parameters.getParameter(StandardCompoundNormalizerParameters.MZvsRTBalance).getValue();
    removeOriginal =
        parameters.getParameter(StandardCompoundNormalizerParameters.autoRemove).getValue();
    standardRows = parameters.getParameter(StandardCompoundNormalizerParameters.standardCompounds)
        .getMatchingRows(featureList);

  }

  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return (double) processedRows / (double) totalRows;
  }

  public String getTaskDescription() {
    return "Standard compound normalization of " + originalFeatureList;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting standard compound normalization of " + originalFeatureList + " using "
        + normalizationType + " (total " + standardRows.length + " standard features)");

    // Check if we have standards
    if (standardRows.length == 0) {
      setErrorMessage("No internal standard features selected");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Initialize new alignment result for the normalized result
    normalizedFeatureList = originalFeatureList.createCopy(originalFeatureList + " " + suffix,
        getMemoryMapStorage(), false);

    // Copy raw data files from original alignment result to new alignment
    // result
    totalRows = normalizedFeatureList.getNumberOfRows();

    // Loop through all rows
    rowIteration: for (FeatureListRow row : normalizedFeatureList.getRows()) {

      // Cancel ?
      if (isCanceled()) {
        return;
      }

      // Do not add the standard rows to the new peaklist
      for (int i = 0; i < standardRows.length; i++) {
        if (row == standardRows[i]) {
          processedRows++;
          continue rowIteration;
        }
      }

      // Get m/z and RT of the current row
      double mz = row.getAverageMZ();
      double rt = row.getAverageRT();

      // Loop through all raw data files
      for (RawDataFile file : normalizedFeatureList.getRawDataFiles()) {

        double normalizationFactors[] = null;
        double normalizationFactorWeights[] = null;

        if (normalizationType == StandardUsageType.Nearest) {

          // Search for nearest standard
          FeatureListRow nearestStandardRow = null;
          double nearestStandardRowDistance = Double.MAX_VALUE;

          for (int standardRowIndex =
              0; standardRowIndex < standardRows.length; standardRowIndex++) {
            FeatureListRow standardRow = standardRows[standardRowIndex];

            double stdMZ = standardRow.getAverageMZ();
            double stdRT = standardRow.getAverageRT();
            double distance = MZvsRTBalance * Math.abs(mz - stdMZ) + Math.abs(rt - stdRT);
            if (distance <= nearestStandardRowDistance) {
              nearestStandardRow = standardRow;
              nearestStandardRowDistance = distance;
            }

          }

          assert nearestStandardRow != null;

          // Calc and store a single normalization factor
          normalizationFactors = new double[1];
          normalizationFactorWeights = new double[1];
          Feature standardFeature = nearestStandardRow.getFeature(file);
          if (standardFeature == null) {
            // What to do if standard feature is not available?
            normalizationFactors[0] = 1.0;
          } else {
            if (featureMeasurementType == FeatureMeasurementType.HEIGHT) {
              normalizationFactors[0] = standardFeature.getHeight();
            } else {
              normalizationFactors[0] = standardFeature.getArea();
            }
          }
          logger.finest("Normalizing row #" + row.getID() + " using standard feature " + standardFeature
              + ", factor " + normalizationFactors[0]);
          normalizationFactorWeights[0] = 1.0f;

        }

        if (normalizationType == StandardUsageType.Weighted) {

          // Add all standards as factors, and use distance as weight
          normalizationFactors = new double[standardRows.length];
          normalizationFactorWeights = new double[standardRows.length];

          for (int standardRowIndex =
              0; standardRowIndex < standardRows.length; standardRowIndex++) {
            FeatureListRow standardRow = standardRows[standardRowIndex];

            double stdMZ = standardRow.getAverageMZ();
            double stdRT = standardRow.getAverageRT();
            double distance = MZvsRTBalance * Math.abs(mz - stdMZ) + Math.abs(rt - stdRT);

            Feature standardFeature = standardRow.getFeature(file);
            if (standardFeature == null) {
              // What to do if standard feature is not available?
              normalizationFactors[standardRowIndex] = 1.0;
              normalizationFactorWeights[standardRowIndex] = 0.0;
            } else {
              if (featureMeasurementType == FeatureMeasurementType.HEIGHT) {
                normalizationFactors[standardRowIndex] = standardFeature.getHeight();
              } else {
                normalizationFactors[standardRowIndex] = standardFeature.getArea();
              }
              normalizationFactorWeights[standardRowIndex] = 1 / distance;
            }
          }

        }

        assert normalizationFactors != null;
        assert normalizationFactorWeights != null;

        // Calculate a single normalization factor as weighted average
        // of all factors
        double weightedSum = 0.0f;
        double sumOfWeights = 0.0f;
        for (int factorIndex = 0; factorIndex < normalizationFactors.length; factorIndex++) {
          weightedSum +=
              normalizationFactors[factorIndex] * normalizationFactorWeights[factorIndex];
          sumOfWeights += normalizationFactorWeights[factorIndex];
        }
        double normalizationFactor = weightedSum / sumOfWeights;

        // For simple scaling of the normalized values
        normalizationFactor = normalizationFactor / 100.0f;

        logger.finest("Normalizing row #" + row.getID() + "[" + file + "] using factor "
            + normalizationFactor);

        // How to handle zero normalization factor?
        if (normalizationFactor == 0.0)
          normalizationFactor = Double.MIN_VALUE;

        // Normalize feature
        Feature originalFeature = row.getFeature(file);
        if (originalFeature != null) {
          float normalizedHeight = (float) (originalFeature.getHeight() / normalizationFactor);
          float normalizedArea = (float) (originalFeature.getArea() / normalizationFactor);
          originalFeature.setHeight(normalizedHeight);
          originalFeature.setArea(normalizedArea);
        }

      }

      processedRows++;
    }

    // Add new feature list to the project
    project.addFeatureList(normalizedFeatureList);

    // Add task description to feature list
    normalizedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Standard compound normalization",
            StandardCompoundNormalizerModule.class, parameters, getModuleCallDate()));

    // Remove the original feature list if requested
    if (removeOriginal)
      project.removeFeatureList(originalFeatureList);

    logger.info("Finished standard compound normalizer");
    setStatus(TaskStatus.FINISHED);

  }

}
