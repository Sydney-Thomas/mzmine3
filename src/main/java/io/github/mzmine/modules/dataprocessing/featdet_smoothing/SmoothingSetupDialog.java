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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingTask.SmoothingDimension;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.util.FeatureUtils;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class SmoothingSetupDialog extends ParameterSetupDialogWithPreview {

  private final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  private final ColoredXYShapeRenderer smoothedRenderer;

  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  protected ComboBox<ModularFeatureList> flistBox;
  protected ComboBox<ModularFeature> fBox;
  protected ColoredXYShapeRenderer shapeRenderer = new ColoredXYShapeRenderer();
  protected SmoothingDimension previewDimension;

  public SmoothingSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    previewChart = new SimpleXYChart<>("Preview");
    previewChart.setRangeAxisLabel("Intensity");
    previewChart.setDomainAxisLabel(uf.format("Retention time", "min"));
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    previewChart.setMinHeight(400);
    smoothedRenderer = new ColoredXYShapeRenderer();

    previewDimension = SmoothingDimension.RETENTION_TIME;
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<ModularFeatureList> flists = (ObservableList<ModularFeatureList>)
        (ObservableList<? extends FeatureList>) MZmineCore.getProjectManager().getCurrentProject()
            .getFeatureLists();

    fBox = new ComboBox<>();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(
                FXCollections.observableArrayList(newValue
                    .getFeatures(newValue.getRawDataFile(0))));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(ModularFeature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public ModularFeature fromString(String string) {
        return null;
      }
    });

    fBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> onSelectedFeatureChanged(newValue)));

    ComboBox<SmoothingDimension> previewDimensionBox = new ComboBox<>(
        FXCollections.observableArrayList(SmoothingDimension.values()));
    previewDimensionBox.setValue(previewDimension);
    previewDimensionBox.valueProperty().addListener((obs, old, newval) -> {
      this.previewDimension = newval;
      if(previewDimension == SmoothingDimension.RETENTION_TIME) {
        previewChart.setDomainAxisLabel(uf.format("Retention time", "min"));
      } else {
        previewChart.setDomainAxisLabel("Mobility");
      }
      parametersChanged();
    });

    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    pnControls.add(new Label("Preview dimension"), 0, 2);
    pnControls.add(previewDimensionBox, 1, 2);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());

  }

  private void onSelectedFeatureChanged(final ModularFeature f) {
    previewChart.removeAllDatasets();
    if (f == null) {
      return;
    }

    IonTimeSeries<? extends Scan> featureSeries = f.getFeatureData();

    if (previewDimension == SmoothingDimension.RETENTION_TIME) {
      previewChart
          .addDataset(new ColoredXYDataset(new IonTimeSeriesToXYProvider(f.getFeatureData(),
              FeatureUtils.featureToString(f), f.getRawDataFile().colorProperty())));
    } else {
      if (featureSeries instanceof IonMobilogramTimeSeries) {
        previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(f)));
      }
    }

    final Color previewColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getPositiveColor();

    final boolean smoothRt = parameterSet.getParameter(SmoothingParameters.rtSmoothing).getValue();
    final int rtFilterWidth = parameterSet.getParameter(SmoothingParameters.rtSmoothing)
        .getEmbeddedParameter()
        .getValue();
    boolean smoothMobility = parameterSet.getParameter(SmoothingParameters.mobilitySmoothing)
        .getValue();
    if (!(f.getFeatureData() instanceof IonMobilogramTimeSeries)) {
      smoothMobility = false;
    }
    final int mobilityFilterWidth = parameterSet.getParameter(SmoothingParameters.mobilitySmoothing)
        .getEmbeddedParameter().getValue();
    final double[] rtWeights = SavitzkyGolayFilter.getNormalizedWeights(rtFilterWidth);
    final double[] mobilityWeights = SavitzkyGolayFilter.getNormalizedWeights(mobilityFilterWidth);

    final SGIntensitySmoothing smoothing = new SGIntensitySmoothing(ZeroHandlingType.KEEP,
        rtWeights);
    final IonTimeSeries<? extends Scan> smoothed = SmoothingTask
        .replaceOldIntensities(null, featureSeries, f, smoothing.smooth(featureSeries),
            ZeroHandlingType.KEEP, smoothMobility, mobilityWeights);

    if (previewDimension == SmoothingDimension.RETENTION_TIME) {
      previewChart.addDataset(
          new ColoredXYDataset(new IonTimeSeriesToXYProvider(smoothed, "smoothed",
              new SimpleObjectProperty<>(previewColor))), smoothedRenderer);
    } else {
      if (smoothed instanceof IonMobilogramTimeSeries) {
        previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(
            ((IonMobilogramTimeSeries) smoothed).getSummedMobilogram(),
            new SimpleObjectProperty<>(previewColor),
            "smoothed")), smoothedRenderer);
      }
    }
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updateParameterSetFromComponents();
    onSelectedFeatureChanged(fBox.getValue());
  }
}
