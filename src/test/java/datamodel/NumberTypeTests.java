/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  License for more details.
 *
 *  You should have received a copy of the GNU General License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.types.numbers.AreaRangeType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.MsMsScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.ParentChromatogramIDType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.SizeType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import org.junit.jupiter.api.Test;

class NumberTypeTests {

  @Test
  void testAreaRange() {
    AreaRangeType areaRangeType = new AreaRangeType();
    Range<Float> areaRange = Range.closed(1300.23f, 1500.3455f);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(areaRangeType, areaRange);
  }

  @Test
  void testAreaType() {
    AreaType areaType = new AreaType();
    Float area = 1.423E4f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(areaType, area);
  }

  @Test
  void testAsymmetryFactorType() {
    AsymmetryFactorType type = new AsymmetryFactorType();
    Float value = 1.423E4f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testCCSType() {
    CCSType type = new CCSType();
    Float value = 352.3f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testChargeType() {
    ChargeType type = new ChargeType();
    int value = 2;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testCombinedScoreType() {
    CombinedScoreType type = new CombinedScoreType();
    Float value = 0.934f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testFWHMType() {
    FwhmType type = new FwhmType();
    Float value = 2.54f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testHeightType() {
    HeightType type = new HeightType();
    Float value = 3.1232145132E4f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testIdType() {
    IDType type = new IDType();
    Integer value = 4;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testIntensityRangeType() {
    IntensityRangeType intensityRangeType = new IntensityRangeType();
    Range<Float> value = Range.closed(1300.23f, 1500.3455f);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(intensityRangeType, value);
  }

  @Test
  void testIsotopePatternScoreType() {
    IsotopePatternScoreType type = new IsotopePatternScoreType();
    Float value = 0.3456f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMatchingSignalsType() {
    MatchingSignalsType type = new MatchingSignalsType();
    Integer value = 3;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMobilityRangeType() {
    MobilityRangeType type = new MobilityRangeType();
    Range<Float> value = Range.closed(0.354f, 0.450f);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMobilityType() {
    MobilityType type = new MobilityType();
    Float value = 0.3456f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMsMsScoreType() {
    MsMsScoreType type = new MsMsScoreType();
    Float value = 0.3456f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMzAbsoluteDifferenceType() {
    MzAbsoluteDifferenceType type = new MzAbsoluteDifferenceType();
    Double value = 0.0003456d;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMzPpmDifferenceType() {
    MzPpmDifferenceType type = new MzPpmDifferenceType();
    Float value = 5.35321f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMzRangeType() {
    MZRangeType type = new MZRangeType();
    Range<Double> value = Range.closed(354.354d, 354.450d);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testMZType() {
    MZType type = new MZType();
    Double value = 235.35321d;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testNeutralMassType() {
    NeutralMassType type = new NeutralMassType();
    Double value = 5.35321d;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testParentChromatogramIdType() {
    ParentChromatogramIDType type = new ParentChromatogramIDType();
    Integer value = 5;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testPrecursorMzType() {
    PrecursorMZType type = new PrecursorMZType();
    Double value = 235.213321d;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testRTRangeType() {
    RTRangeType type = new RTRangeType();
    Range<Float> value = Range.closed(5.35321f, 54.68f);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testRTType() {
    RTType type = new RTType();
    Float value = 5.35321f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testSizeType() {
    SizeType type = new SizeType();
    Integer value = 13;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void testTailingFactorType() {
    TailingFactorType type = new TailingFactorType();
    Float value = 5.35321f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }
}
