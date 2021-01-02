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

package io.github.mzmine.datamodel.features.types;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mzmine.datamodel.IsotopePattern;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class IsotopePatternType extends DataType<ObjectProperty<IsotopePattern>> {

  @Override
  @Nonnull
  public String getHeaderString() {
    return "Isotopes";
  }

  @Override
  @Nonnull
  public String getFormattedString(@Nonnull ObjectProperty<IsotopePattern> property) {
    return property.getValue() != null ? String.valueOf(property.getValue().getNumberOfDataPoints()) : "";
  }

  @Nonnull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if(value==null || !(value instanceof IsotopePattern))
      return "";
    return String.valueOf(((IsotopePattern)value).getNumberOfDataPoints());
  }

  @Override
  public ObjectProperty<IsotopePattern> createProperty() {
    return new SimpleObjectProperty<>();
  }

}
