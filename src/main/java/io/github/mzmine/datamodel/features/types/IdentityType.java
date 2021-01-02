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

import io.github.mzmine.datamodel.features.types.modifiers.AddElementDialog;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import javafx.util.StringConverter;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;

public class IdentityType extends ListDataType<FeatureIdentity> implements AnnotationType,
    EditableColumnType, AddElementDialog, StringParser<FeatureIdentity> {

  private static StringConverter<FeatureIdentity> converter = new StringConverter<FeatureIdentity>() {
    @Override
    public String toString(FeatureIdentity object) {
      return object.toString();
    }

    @Override
    public FeatureIdentity fromString(String name) {
      return new SimpleFeatureIdentity(name);
    }
  };

  @Override
  public String getHeaderString() {
    return "Identity";
  }

  @Override
  public FeatureIdentity fromString(String s) {
    return converter.fromString(s);
  }

  @Override
  public StringConverter<FeatureIdentity> getStringConverter() {
    return converter;
  }
}
