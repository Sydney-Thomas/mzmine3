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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureInformationType extends
    DataType<ObjectProperty<SimpleFeatureInformation>> implements NullColumnType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_information";
  }

  @Override
  @NotNull
  public String getHeaderString() {
    return "Feature information";
  }

  @Override
  @NotNull
  public String getFormattedString(@NotNull ObjectProperty<SimpleFeatureInformation> property) {
    return property.getValue() != null ? property.getValue().getAllProperties().entrySet().stream()
        .map(Object::toString).collect(Collectors.joining(";")) : "";
  }

  @Override
  public ObjectProperty<SimpleFeatureInformation> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof SimpleFeatureInformation info)) {
      return;
    }

    info.saveToXML(writer);
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    while (
        !(reader.isStartElement() && reader.getLocalName().equals(FeatureInformation.XML_ELEMENT))
            && reader.hasNext()) {
      reader.next();
    }

    if (reader.isStartElement() && reader.getLocalName()
        .equals(SimpleFeatureInformation.XML_ELEMENT)) {
      return SimpleFeatureInformation.loadFromXML(reader);
    }
    return null;
  }
}
