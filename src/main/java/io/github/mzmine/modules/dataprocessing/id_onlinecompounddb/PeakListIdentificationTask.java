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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.util.FeatureListRowSorter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import org.jetbrains.annotations.NotNull;

public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(PeakListIdentificationTask.class.getName());

  // Minimum abundance.
  private static final double MIN_ABUNDANCE = 0.001;

  // Counters.
  private int finishedItems;
  private int numItems;

  private final MZmineProcessingStep<OnlineDatabases> db;
  private final MZTolerance mzTolerance;
  private final int numOfResults;
  private final FeatureList peakList;
  private final boolean isotopeFilter;
  private final ParameterSet isotopeFilterParameters;
  private final IonizationType ionType;
  private DBGateway gateway;
  private FeatureListRow currentRow;
  private final ParameterSet parameters;

  /**
   * Create the identification task.
   *
   * @param parameters task parameters.
   * @param list feature list to operate on.
   */
  PeakListIdentificationTask(final ParameterSet parameters, final FeatureList list, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    peakList = list;
    numItems = 0;
    finishedItems = 0;
    gateway = null;
    currentRow = null;

    db = parameters.getParameter(SingleRowIdentificationParameters.DATABASE).getValue();
    mzTolerance =
        parameters.getParameter(SingleRowIdentificationParameters.MZ_TOLERANCE).getValue();
    numOfResults =
        parameters.getParameter(SingleRowIdentificationParameters.MAX_RESULTS).getValue();
    isotopeFilter =
        parameters.getParameter(SingleRowIdentificationParameters.ISOTOPE_FILTER).getValue();
    isotopeFilterParameters = parameters
        .getParameter(SingleRowIdentificationParameters.ISOTOPE_FILTER).getEmbeddedParameters();
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    this.parameters = parameters;
  }

  @Override
  public double getFinishedPercentage() {

    return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of peaks in " + peakList
        + (currentRow == null ? " using " + db
            : " (" + MZmineCore.getConfiguration().getMZFormat().format(currentRow.getAverageMZ())
                + " m/z) using " + db);
  }

  @Override
  public void run() {

    if (!isCanceled()) {
      try {

        setStatus(TaskStatus.PROCESSING);

        // Create database gateway.
        gateway = db.getModule().getGatewayClass().getDeclaredConstructor().newInstance();

        // Identify the feature list rows starting from the biggest
        // peaks.
        final FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);
        Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

          // Retrieve results for each row.
          retrieveIdentification(rows[finishedItems]);
        }

        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        final String msg = "Could not search " + db;
        logger.log(Level.WARNING, msg, t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
      }
    }

    peakList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(OnlineDBSearchModule.class, parameters, getModuleCallDate()));
  }

  /**
   * Search the database for the peak's identity.
   *
   * @param row the feature list row.
   * @throws IOException if there are i/o problems.
   */
  private void retrieveIdentification(final FeatureListRow row) throws IOException {

    currentRow = row;

    // Determine peak charge.
    final Feature bestPeak = row.getBestFeature();
    int charge = bestPeak.getCharge();
    if (charge <= 0) {
      charge = 1;
    }

    // Calculate mass value.

    final double massValue = row.getAverageMZ() * charge - ionType.getAddedMass();

    // Isotope pattern.
    final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

    // Process each one of the result ID's.
    final String[] findCompounds =
        gateway.findCompounds(massValue, mzTolerance, numOfResults, db.getParameterSet());

    for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

      final DBCompound compound = gateway.getCompound(findCompounds[i], db.getParameterSet());

      // In case we failed to retrieve data, skip this compound
      if (compound == null)
        continue;

      final String formula = compound.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA);

      // If required, check isotope score.
      if (isotopeFilter && rowIsotopePattern != null && formula != null) {

        // First modify the formula according to ionization.
        final String adjustedFormula = FormulaUtils.ionizeFormula(formula, ionType, charge);

        logger.finest("Calculating isotope pattern for compound formula " + formula
            + " adjusted to " + adjustedFormula);

        // Generate IsotopePattern for this compound
        final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
            .calculateIsotopePattern(adjustedFormula, MIN_ABUNDANCE, charge, ionType.getPolarity());

        // Check isotope pattern match
        boolean check = IsotopePatternScoreCalculator.checkMatch(rowIsotopePattern,
            compoundIsotopePattern, isotopeFilterParameters);

        if (!check)
          continue;
      }

      // Add the retrieved identity to the feature list row
      row.addFeatureIdentity(compound, false);

    }
  }
}
