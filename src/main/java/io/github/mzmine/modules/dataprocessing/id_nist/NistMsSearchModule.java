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

package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * NIST MS Search module.
 */
public class NistMsSearchModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "NIST MS Search";
  private static final String MODULE_DESCRIPTION =
      "This method searches spectra against the NIST library.";

  @Override
  public @NotNull String getName() {

    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {

    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {

    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return NistMsSearchParameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    for (final FeatureList peakList : parameters.getParameter(NistMsSearchParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists()) {

      tasks.add(new NistMsSearchTask(peakList, parameters, moduleCallDate));
    }

    return ExitCode.OK;
  }

  /**
   * Search for a peak-list row's mass spectrum.
   *
   * @param peakList the peak-list.
   * @param row the peak-list row.
   */
  public static void singleRowSearch(final FeatureList peakList, final FeatureListRow row) {

    final ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(NistMsSearchModule.class);
    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(new NistMsSearchTask(row, peakList, parameters, Instant.now()));
    }
  }
}
