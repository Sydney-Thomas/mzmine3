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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatRangeType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_ms2planer_results.Ms2PlannerPrecursor;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;

public class Ms2PlannerPrecursorType extends DataType<ObjectProperty<Ms2PlannerPrecursor>> {

  public Ms2PlannerPrecursorType() {
  }

  @Override
  @Nonnull
  public String getHeaderString() {
    return "MS2 planner";
  }

  @Override
  public ObjectProperty<Ms2PlannerPrecursor> createProperty() {
    return new SimpleObjectProperty<Ms2PlannerPrecursor>();
  }


}
