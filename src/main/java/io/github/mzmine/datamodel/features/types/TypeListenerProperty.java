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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.ListRowBinding;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Holds no value but makes fireChangedEvent public. Used by {@link LinkedDataType} and {@link
 * ListRowBinding} to listen to multiple feature DataTypes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TypeListenerProperty extends SimpleBooleanProperty {

  // Listen to multiple feature types for changes
  private List<DataType> featureTypes;

  public void fireChangedEvent() {
    super.fireValueChangedEvent();
  }
}
