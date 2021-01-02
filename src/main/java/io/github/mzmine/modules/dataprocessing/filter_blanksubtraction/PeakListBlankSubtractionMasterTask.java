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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import io.github.mzmine.datamodel.features.*;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PeakListBlankSubtractionMasterTask extends AbstractTask {

  private NumberFormat mzFormat, rtFormat;

  private static Logger logger = Logger.getLogger("PeakListBlankSubstractionTask");

  private MZmineProject project;
  private PeakListBlankSubtractionParameters parameters;

  private int minBlankDetections;

  private RawDataFilesSelection blankSelection;
  private RawDataFile[] blankRaws;
  private FeatureList alignedFeatureList;

  private List<AbstractTask> subTasks;

  public PeakListBlankSubtractionMasterTask(MZmineProject project,
      PeakListBlankSubtractionParameters parameters) {

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    this.project = project;
    this.parameters = parameters;
    this.blankSelection =
        parameters.getParameter(PeakListBlankSubtractionParameters.blankRawDataFiles).getValue();
    this.blankRaws = blankSelection.getMatchingRawDataFiles();
    this.alignedFeatureList =
        parameters.getParameter(PeakListBlankSubtractionParameters.alignedPeakList).getValue()
            .getMatchingFeatureLists()[0];
    this.minBlankDetections =
        parameters.getParameter(PeakListBlankSubtractionParameters.minBlanks).getValue();

    subTasks = new ArrayList<>();

    setStatus(TaskStatus.WAITING);

    logger.setLevel(Level.FINEST);
  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction master task";
  }

  @Override
  public double getFinishedPercentage() {
    double perc = 0;
    for (AbstractTask t : subTasks) {
      perc += t.getFinishedPercentage();
    }
    return perc / subTasks.size();
  }

  @Override
  public void run() {

    if (!checkBlankSelection(alignedFeatureList, blankRaws)) {
      setErrorMessage("Peak list " + alignedFeatureList.getName()
          + " does no contain all selected blank raw data files.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    // PeakListRow[] rowsInBlanks =
    // getFeatureRowsContainedBlanks(alignedFeatureList, blankRaws,
    // minBlankDetections);

    FeatureListRow[] rows =
        FeatureUtils.copyFeatureRows(alignedFeatureList.getRows().toArray(ModularFeatureListRow[]::new));
    rows = FeatureUtils.sortRowsMzAsc(rows);

    for (RawDataFile raw : alignedFeatureList.getRawDataFiles()) {
      // only create a task for every file that is not a blank
      if (Arrays.asList(blankRaws).contains(raw))
        continue;

      // these tasks will access the passed array and remove the features
      // that appear in their raw
      // data file and the blanks from these rows
      AbstractTask task = new PeakListBlankSubtractionSingleTask(parameters, raw, rows);
      MZmineCore.getTaskController().addTask(task);
      subTasks.add(task);

      if (getStatus() == TaskStatus.CANCELED)
        return;
    }

    // wait for tasks to finish
    boolean allTasksFinished = false;
    while (!allTasksFinished) {
      allTasksFinished = true;
      for (AbstractTask task : subTasks) {
        if (task.getStatus() != TaskStatus.FINISHED)
          allTasksFinished = false;
      }

      try {
        TimeUnit.MILLISECONDS.sleep(5);
      } catch (InterruptedException e) {
        e.printStackTrace();
        setErrorMessage(e.getMessage());
        setStatus(TaskStatus.ERROR);
        return;
      }

      if (getStatus() == TaskStatus.CANCELED)
        return;
    }

    // remove rows that only contain blankRaws
    List<RawDataFile> blankRawsList = Arrays.asList(blankRaws);
    int onlyBlankRows = 0;
    for (int i = 0; i < rows.length; i++) {
      FeatureListRow row = rows[i];

      if (blankRawsList.containsAll(Arrays.asList(row.getRawDataFiles()))) {
        onlyBlankRows++;
        rows[i] = null;
      }

      if (getStatus() == TaskStatus.CANCELED)
        return;
    }

    logger.finest("Removed " + onlyBlankRows + " rows that only existed in blankfiles.");

    FeatureList result = new ModularFeatureList(alignedFeatureList.getName() + " sbtrctd",
        alignedFeatureList.getRawDataFiles());

    for (FeatureListRow row : rows) {
      if (row != null) {
        result.addRow(row);
      }
    }

    FeatureListUtils.copyPeakListAppliedMethods(alignedFeatureList, result);
    result.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(PeakListBlankSubtractionModule.MODULE_NAME, parameters));

    project.addFeatureList(result);

    setStatus(TaskStatus.FINISHED);
  }

  private boolean checkBlankSelection(FeatureList aligned, RawDataFile[] blankRaws) {

    RawDataFile[] flRaws = aligned.getRawDataFiles().toArray(RawDataFile[]::new);

    for (int i = 0; i < blankRaws.length; i++) {
      boolean contained = false;

      for (RawDataFile flRaw : flRaws) {
        if (blankRaws[i] == flRaw)
          contained = true;
      }

      if (contained == false) {
        logger.info("Peak list " + aligned.getName() + " does not contain raw data files "
            + blankRaws[i].getName());
        return false;
      }
    }

    logger.info("Peak list " + aligned.getName() + " contains all selected blank raw data files.");
    return true;
  }

}
