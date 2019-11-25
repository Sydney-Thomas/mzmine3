package io.github.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;

public class GroupedFeature implements Feature {
  private List<Feature> features = new ArrayList<>();

  private HashMap<String, Object> data;

  public GroupedFeature(List<Feature> features) {
    super();
    this.features = features;
  }

  public GroupedFeature(Feature... features) {
    super();
    for (Feature f : features)
      this.features.add(f);
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public void addFeature(Feature f) {
    features.add(f);
  }

  @Override
  @Nonnull
  public FeatureStatus getFeatureStatus() {
    return features.stream().;
  }

  @Override
  public double getMZ() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getRT() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getHeight() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getArea() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  @Nonnull
  public RawDataFile getDataFile() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public int[] getScanNumbers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getRepresentativeScanNumber() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  @Nullable
  public DataPoint getDataPoint(int scanNumber) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getRawDataPointsRTRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getRawDataPointsMZRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getRawDataPointsIntensityRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getMostIntenseFragmentScanNumber() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int[] getAllMS2FragmentScanNumbers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setFragmentScanNumber(int fragmentScanNumber) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAllMS2FragmentScanNumbers(int[] allMS2FragmentScanNumbers) {
    // TODO Auto-generated method stub

  }

  @Override
  @Nullable
  public IsotopePattern getIsotopePattern() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    // TODO Auto-generated method stub

  }

  @Override
  public int getCharge() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setCharge(int charge) {
    // TODO Auto-generated method stub

  }

  @Override
  public Double getFWHM() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double getTailingFactor() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double getAsymmetryFactor() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setFWHM(Double fwhm) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTailingFactor(Double tf) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAsymmetryFactor(Double af) {
    // TODO Auto-generated method stub

  }

  @Override
  public void outputChromToFile() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setPeakInformation(SimplePeakInformation peakInfoIn) {
    // TODO Auto-generated method stub

  }

  @Override
  public SimplePeakInformation getPeakInformation() {
    // TODO Auto-generated method stub
    return null;
  }

}
