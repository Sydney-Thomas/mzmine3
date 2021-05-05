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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

import io.github.mzmine.modules.io.import_ms2planer_results.Ms2PlanerImportTask;
import io.github.mzmine.modules.io.import_ms2planer_results.Ms2PlannerPrecursor;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class Ms2PlannerTest {
  private static Logger logger = Logger.getLogger(Ms2PlannerTest.class.getName());

  @Test
  public void testCsvImport() {
    File file = new File(Ms2PlannerTest.class.getClassLoader()
        .getResource("ms2planner/ms2planner_results.csv").getFile());
    // read the csv into list of precursors
    List<Ms2PlannerPrecursor> pre = Ms2PlanerImportTask.importCSV(file);
    // print to console
    pre.forEach(precursor -> logger.info(precursor.toString()));

    Assertions.assertFalse(pre.isEmpty());
  }
}
