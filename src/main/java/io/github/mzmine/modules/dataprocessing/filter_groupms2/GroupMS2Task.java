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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import javafx.collections.FXCollections;

/**
 * Filters out feature list rows.
 */
public class GroupMS2Task extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(GroupMS2Task.class.getName());
  // Feature lists.
  private final MZmineProject project;
  // Processed rows counter
  private int processedRows, totalRows;
  // Parameters.
  private final ParameterSet parameters;
  private FeatureList list;
  private RTTolerance rtTol;
  private MZTolerance mzTol;
  private boolean limitRTByFeature;

  /**
   * Create the task.
   *
   * @param list feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet) {

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    rtTol = parameters.getParameter(GroupMS2Parameters.rtTol).getValue();
    mzTol = parameters.getParameter(GroupMS2Parameters.mzTol).getValue();
    limitRTByFeature = parameters.getParameter(GroupMS2Parameters.limitRTByFeature).getValue();
    this.list = list;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Adding all MS2 scans to their features in list " + list.getName();
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);

      totalRows = list.getNumberOfRows();
      // for all features
      for (FeatureListRow row : list.getRows()) {
        for (Feature f : row.getFeatures()) {
          if (getStatus() == TaskStatus.ERROR)
            return;
          if (isCanceled())
            return;

          RawDataFile raw = f.getRawDataFile();
          IntArrayList scans = new IntArrayList();
          int best = f.getRepresentativeScanNumber();
          float frt = f.getRT();
          double fmz = f.getMZ();
          Range<Float> rtRange = f.getRawDataPointsRTRange();
          int i = best;
          // left
          while (i > 1) {
            i--;
            Scan scan = raw.getScan(i);
            if (scan != null) {
              if ((!limitRTByFeature || rtRange.contains(scan.getRetentionTime()))
                  && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())) {
                if (scan.getPrecursorMZ() != 0
                    && mzTol.checkWithinTolerance(fmz, scan.getPrecursorMZ()))
                  scans.add(i);
              } else {
                // end of loop - out of tolerance
                break;
              }
            }
          }
          int[] scanNumbers = raw.getScanNumbers();
          // right
          while (i < scanNumbers[scanNumbers.length - 1]) {
            i++;
            Scan scan = raw.getScan(i);
            // scanID does not have to be contiguous
            if (scan != null) {
              if ((!limitRTByFeature || rtRange.contains(scan.getRetentionTime()))
                  && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())) {
                if (scan.getPrecursorMZ() != 0
                    && mzTol.checkWithinTolerance(fmz, scan.getPrecursorMZ()))
                  scans.add(i);
              } else {
                // end of loop - out of tolerance
                break;
              }
            }
          }
          // set list to feature
          f.setAllMS2FragmentScanNumbers(FXCollections.observableArrayList(scans));
        }
        processedRows++;
      }

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished adding all MS2 scans to their features in " + list.getName());

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Error while adding all MS2 scans to their feautres", t);
    }

  }

}
