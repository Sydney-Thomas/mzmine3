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

package io.github.mzmine.modules.dataprocessing.id_adductsearch;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class AdductSearchModule implements MZmineProcessingModule {

  private static final String NAME = "Adduct search";

  private static final String DESCRIPTION =
      "This method searches for adduct peaks that appear at the same retention time as other peaks and have a defined mass difference.";

  @Override
  public @NotNull String getName() {

    return NAME;
  }

  @Override
  public @NotNull String getDescription() {

    return DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {

    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {

    return AdductSearchParameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull final ParameterSet parameters,
      @NotNull final Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    for (final FeatureList peakList : parameters.getParameter(AdductSearchParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists()) {
      tasks.add(new AdductSearchTask(parameters, peakList, moduleCallDate));
    }

    return ExitCode.OK;
  }
}
