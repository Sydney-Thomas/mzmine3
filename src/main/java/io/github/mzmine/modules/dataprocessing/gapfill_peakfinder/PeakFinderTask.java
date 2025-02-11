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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

class PeakFinderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ModularFeatureList peakList, processedPeakList;
  private String suffix;
  private double intTolerance;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean rtCorrection;
  private ParameterSet parameters;
  private int totalScans;
  private AtomicInteger processedScans;
  private boolean MASTERLIST = true, removeOriginal;
  private int masterSample = 0;
  private boolean useParallelStream = false;

  PeakFinderTask(MZmineProject project, FeatureList peakList, ParameterSet parameters, MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.peakList = (ModularFeatureList) peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(PeakFinderParameters.suffix).getValue();
    intTolerance = parameters.getParameter(PeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(PeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(PeakFinderParameters.RTTolerance).getValue();
    rtCorrection = parameters.getParameter(PeakFinderParameters.RTCorrection).getValue();
    removeOriginal = parameters.getParameter(PeakFinderParameters.autoRemove).getValue();
    useParallelStream = parameters.getParameter(PeakFinderParameters.useParallel).getValue();
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running gap filler on " + peakList);

    // Calculate total number of scans in all files
    for (RawDataFile dataFile : peakList.getRawDataFiles()) {
      totalScans += dataFile.getNumOfScans(1);
    }
    processedScans = new AtomicInteger();

    // Create new feature list
    processedPeakList = peakList.createCopy(peakList + " " + suffix, getMemoryMapStorage(), false);

    if (rtCorrection) {
      totalScans *= 2;
      // Fill the gaps of a random sample using all the other samples and
      // take it as master list
      // to fill the gaps of the other samples
      masterSample = (int) Math.floor(Math.random() * processedPeakList.getNumberOfRawDataFiles());
      fillList(MASTERLIST);

      // Process all raw data files
      fillList(!MASTERLIST);

    } else {

      // Process all raw data files
      IntStream rawStream = IntStream.range(0, processedPeakList.getNumberOfRawDataFiles());
      if (useParallelStream)
        rawStream = rawStream.parallel();

      rawStream.forEach(i -> {
        // Canceled?
        if (isCanceled()) {
          // inside stream - only skips this element
          return;
        }
        RawDataFile dataFile = processedPeakList.getRawDataFile(i);

        List<Gap> gaps = new ArrayList<Gap>();

        // Fill each row of this raw data file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < processedPeakList.getNumberOfRows(); row++) {
          // Canceled?
          if (isCanceled()) {
            // inside stream - only skips this element
            return;
          }

          FeatureListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = newRow.getFeature(dataFile);

          if (sourcePeak == null) {
            // Create a new gap
            Range<Double> mzRange = mzTolerance.getToleranceRange(newRow.getAverageMZ());
            Range<Float> rtRange = rtTolerance.getToleranceRange(newRow.getAverageRT());

            Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance);
            gaps.add(newGap);
          }
        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans.addAndGet(dataFile.getNumOfScans());
          return;
        }

        // Get all scans of this data file
        dataFile.getScanNumbers(1).forEach(scan -> {
          if(!isCanceled()) {
            // Feed this scan to all gaps
            for (Gap gap : gaps) {
              gap.offerNextScan(scan);
            }

            processedScans.incrementAndGet();
          }
        });

        // Finalize gaps
        for (Gap gap : gaps) {
          gap.noMoreOffers();
        }
      });
    }
    // terminate - stream only skips all elements
    if (isCanceled())
      return;

    // Append processed feature list to the project
    project.addFeatureList(processedPeakList);

    // Add task description to peakList
    processedPeakList
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Gap filling ",
            PeakFinderModule.class, parameters, getModuleCallDate()));

    // Remove the original peaklist if requested
    if (removeOriginal)
      project.removeFeatureList(peakList);

    logger.info("Finished gap-filling on " + peakList);
    setStatus(TaskStatus.FINISHED);

  }

  public void fillList(boolean masterList) {
    for (int i = 0; i < processedPeakList.getNumberOfRawDataFiles(); i++) {
      if (i != masterSample) {

        RawDataFile datafile1;
        RawDataFile datafile2;

        if (masterList) {
          datafile1 = processedPeakList.getRawDataFile(masterSample);
          datafile2 = processedPeakList.getRawDataFile(i);
        } else {
          datafile1 = processedPeakList.getRawDataFile(i);
          datafile2 = processedPeakList.getRawDataFile(masterSample);
        }
        RegressionInfo info = new RegressionInfo();

        for (FeatureListRow row : processedPeakList.getRows()) {
          Feature peaki = row.getFeature(datafile1);
          Feature peake = row.getFeature(datafile2);
          if (peaki != null && peake != null) {
            info.addData(peake.getRT(), peaki.getRT());
          }
        }

        info.setFunction();

        // Canceled?
        if (isCanceled()) {
          return;
        }

        Vector<Gap> gaps = new Vector<Gap>();

        // Fill each row of this raw data file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < processedPeakList.getNumberOfRows(); row++) {
          FeatureListRow sourceRow = peakList.getRow(row);
          FeatureListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = sourceRow.getFeature(datafile1);

          if (sourcePeak == null || sourcePeak.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
            // Create a new gap

            double mz = sourceRow.getAverageMZ();
            double rt2 = -1;
            if (!masterList) {
              if (peakList.getRow(row).getFeature(datafile2) != null) {
                rt2 = peakList.getRow(row).getFeature(datafile2).getRT();
              }
            } else {
              if (peakList.getRow(row).getFeature(datafile2) != null) {
                rt2 = peakList.getRow(row).getFeature(datafile2).getRT();
              }
            }

            if (rt2 > -1) {
              float rt = (float) info.predict(rt2);

              if (rt != -1) {
                Range<Double> mzRange = mzTolerance.getToleranceRange(mz);
                Range<Float> rtRange = rtTolerance.getToleranceRange(rt);

                Gap newGap = new Gap(newRow, datafile1, mzRange, rtRange, intTolerance);
                gaps.add(newGap);
              }
            }
          }
        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans.addAndGet(datafile1.getNumOfScans());
          continue;
        }

        // Get all scans of this data file
        datafile1.getScanNumbers(1).forEach(scan -> {
          if(!isCanceled()) {
            // Feed this scan to all gaps
            for (Gap gap : gaps) {
              gap.offerNextScan(scan);
            }
            processedScans.incrementAndGet();
          }
        });

        // Canceled?
        if (isCanceled()) {
          return;
        }

        // Finalize gaps
        for (Gap gap : gaps) {
          gap.noMoreOffers();
        }
      }
    }
  }

  public double getFinishedPercentage() {
    if (totalScans == 0 || processedScans == null) {
      return 0;
    }
    return (double) processedScans.get() / (double) totalScans;
  }

  public String getTaskDescription() {
    return "Gap filling " + peakList;
  }

}
