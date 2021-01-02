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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.features.Feature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class ChromatogramVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "TIC/XIC visualizer";
  private static final String MODULE_DESCRIPTION = "TIC/XIC visualizer."; // TODO

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    final RawDataFile[] dataFiles = parameters.getParameter(TICVisualizerParameters.DATA_FILES)
        .getValue().getMatchingRawDataFiles();
    final Range<Double> mzRange =
        parameters.getParameter(TICVisualizerParameters.MZ_RANGE).getValue();
    final ScanSelection scanSelection =
        parameters.getParameter(TICVisualizerParameters.scanSelection).getValue();
    final TICPlotType plotType =
        parameters.getParameter(TICVisualizerParameters.PLOT_TYPE).getValue();
    final List<Feature> selectionPeaks =
        parameters.getParameter(TICVisualizerParameters.PEAKS).getValue();

    // Add the window to the desktop only if we actually have any raw
    // data to show.
    boolean weHaveData = false;
    for (RawDataFile dataFile : dataFiles) {
      Scan selectedScans[] = scanSelection.getMatchingScans(dataFile);
      if (selectedScans.length > 0)
        weHaveData = true;
    }

    if (weHaveData) {
      TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
          mzRange, selectionPeaks, ((TICVisualizerParameters) parameters).getPeakLabelMap());
      MZmineCore.getDesktop().addTab(window);

    } else {

      MZmineCore.getDesktop().displayErrorMessage("No scans found");
    }

    return ExitCode.OK;
  }

  public static void setupNewTICVisualizer(final RawDataFile dataFile) {

    setupNewTICVisualizer(new RawDataFile[] {dataFile});
  }

  public static void setupNewTICVisualizer(final RawDataFile[] dataFiles) {
    setupNewTICVisualizer(MZmineCore.getProjectManager().getCurrentProject().getDataFiles(),
        dataFiles, new Feature[0], new Feature[0], null, null, null);
  }

  public static void setupNewTICVisualizer(final RawDataFile[] allFiles,
      final RawDataFile[] selectedFiles, final Feature[] allPeaks, final Feature[] selectedPeaks,
      final Map<Feature, String> peakLabels, ScanSelection scanSelection,
      final Range<Double> mzRange) {

    assert allFiles != null;

    final ChromatogramVisualizerModule myInstance =
        MZmineCore.getModuleInstance(ChromatogramVisualizerModule.class);
    final TICVisualizerParameters myParameters = (TICVisualizerParameters) MZmineCore
        .getConfiguration().getModuleParameters(ChromatogramVisualizerModule.class);
    myParameters.getParameter(TICVisualizerParameters.PLOT_TYPE).setValue(TICPlotType.BASEPEAK);

    if (scanSelection != null) {
      myParameters.getParameter(TICVisualizerParameters.scanSelection).setValue(scanSelection);
    }

    if (mzRange != null) {
      myParameters.getParameter(TICVisualizerParameters.MZ_RANGE).setValue(mzRange);
    }

    if (myParameters.showSetupDialog(true, allFiles, selectedFiles, allPeaks,
        selectedPeaks) == ExitCode.OK) {

      final TICVisualizerParameters p = (TICVisualizerParameters) myParameters.cloneParameterSet();

      if (peakLabels != null) {
        p.setPeakLabelMap(peakLabels);
      }

      myInstance.runModule(MZmineCore.getProjectManager().getCurrentProject(), p,
          new ArrayList<Task>());
    }

  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final Feature[] selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
        mzRange, Arrays.asList(selectionPeaks), peakLabels);
    MZmineCore.getDesktop().addTab(window);
  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final List<Feature> selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
        mzRange, selectionPeaks, peakLabels);
    MZmineCore.getDesktop().addTab(window);
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return TICVisualizerParameters.class;
  }
}
