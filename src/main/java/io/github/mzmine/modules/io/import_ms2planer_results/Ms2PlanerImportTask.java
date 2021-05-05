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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.Ms2PlannerPrecursorType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Export of a feature cluster (ADAP) to mgf. Used in GC-GNPS
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class Ms2PlanerImportTask extends AbstractTask {
  private static Logger logger = Logger.getLogger(Ms2PlanerImportTask.class.getName());

  private static final String SPLIT_CHAR = " ";
  private final int totalRows = 0;
  private final MZmineProject project;
  private int finishedRows = 0;

  private ModularFeatureList flist;
  private final File file;

  public Ms2PlanerImportTask(MZmineProject project, ParameterSet parameters) {
    this(project, parameters, null);
  }
  public Ms2PlanerImportTask(MZmineProject project,
      ParameterSet parameters, ModularFeatureList flist) {
    super(flist==null? null : flist.getMemoryMapStorage());
    this.project = project;
    this.flist = flist;

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
    logger.info("Number of precursors: "+results.size());
    if(flist == null) {
      logger.info("Creating new feature list");
      RawDataFileImpl raw = null;
      try {
        raw = new RawDataFileImpl("TMP", null);
        project.addFile(raw);
        flist = new ModularFeatureList(file.getName(), null, raw);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // add all to list
    flist.addRowType(new Ms2PlannerPrecursorType());

    int id = 1;
    for(Ms2PlannerPrecursor precursor : results) {
      ModularFeatureListRow row =  new ModularFeatureListRow(flist);
      row.setID(id);
      row.set(Ms2PlannerPrecursorType.class, precursor);
      flist.addRow(row);
      id++;
    }

    project.addFeatureList(flist);

    // TODO VISUALIZATION in the 2d raw data viewer

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
      return List.of();
    }
    return results;
  }

}
