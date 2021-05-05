/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package io.github.mzmine.modules.io.import_ms2planer_results;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Export of a feature cluster (ADAP) to mgf. Used in GC-GNPS
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class Ms2PlanerImportTask extends AbstractTask {

  private static final String SPLIT_CHAR = " ";
  private final int totalRows;
  private int finishedRows = 0;

  private final ModularFeatureList flist;
  private final File file;

  public Ms2PlanerImportTask(ParameterSet parameters, ModularFeatureList flist) {
    super(flist.getMemoryMapStorage());
    this.flist = flist;
    totalRows = flist.getNumberOfRows();

    file = parameters.getParameter(Ms2PlanerImportParameters.FILENAME).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows > 0 ? finishedRows / (double) totalRows : 0;
  }

  @Override
  public String getTaskDescription() {
    return "Importing MS2Planer results from " + file.getAbsolutePath();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<Ms2PlannerPrecursor> results = importCSV(file);



    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  public static List<Ms2PlannerPrecursor> importCSV(File file) {
    List<Ms2PlannerPrecursor> results = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      int path = 1;
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(SPLIT_CHAR);
        // skip first column as its the path number
        for (int i = 1; i < values.length; i += 8) {
          double mz = Double.parseDouble(values[i]);
          double mz_isolation = Double.parseDouble(values[i + 1]);
          double duration = Double.parseDouble(values[i + 2]);
          double rt_start = Double.parseDouble(values[i + 3]);
          double rt_end = Double.parseDouble(values[i + 4]);
          double intensity = Double.parseDouble(values[i + 5]);
          double rt_apex = Double.parseDouble(values[i + 6]);
          int charge = Math.round(Float.parseFloat(values[i + 7]));

          // create new precursor
          results.add(
              new Ms2PlannerPrecursor(path, mz, mz_isolation, duration, rt_start, rt_end, intensity,
                  rt_apex, charge));
        }
        path++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return results;
  }

}
