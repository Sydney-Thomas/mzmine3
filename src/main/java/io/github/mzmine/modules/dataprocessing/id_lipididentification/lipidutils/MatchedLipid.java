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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import org.apache.commons.lang.StringUtils;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.SpeciesLevelAnnotation;
import io.github.mzmine.util.ParsingUtils;

public class MatchedLipid {

  public static final String XML_ELEMENT = "matchedlipid";
  private static final String XML_LIPID_ANNOTATION_ELEMENT = "lipidannotation";
  private static final String XML_ACCURATE_MZ = "accuratemz";
  private static final String XML_IONIZATION_TYPE = "ionizationtype";
  private static final String XML_MATCHED_FRAGMENTS = "matchedfragments";
  private static final String XML_MSMS_SCORE = "msmsscore";
  private static final String XML_COMMENT = "comment";

  private ILipidAnnotation lipidAnnotation;
  private Double accurateMz;
  private IonizationType ionizationType;
  private Set<LipidFragment> matchedFragments;
  private Double msMsScore;
  private String comment;

  public MatchedLipid(ILipidAnnotation lipidAnnotation, Double accurateMz,
      IonizationType ionizationType, Set<LipidFragment> matchedFragments, Double msMsScore) {
    this.lipidAnnotation = lipidAnnotation;
    this.accurateMz = accurateMz;
    this.ionizationType = ionizationType;
    this.matchedFragments = matchedFragments;
    this.msMsScore = msMsScore;
  }

  public ILipidAnnotation getLipidAnnotation() {
    return lipidAnnotation;
  }

  public void setLipidAnnotation(ILipidAnnotation lipidAnnotation) {
    this.lipidAnnotation = lipidAnnotation;
  }

  public Double getAccurateMz() {
    return accurateMz;
  }

  public void setAccurateMz(Double accurateMz) {
    this.accurateMz = accurateMz;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public void setIonizationType(IonizationType ionizationType) {
    this.ionizationType = ionizationType;
  }

  public Set<LipidFragment> getMatchedFragments() {
    return matchedFragments;
  }

  public void setMatchedFragments(Set<LipidFragment> matchedFragments) {
    this.matchedFragments = matchedFragments;
  }

  public Double getMsMsScore() {
    return msMsScore;
  }

  public void setMsMsScore(Double msMsScore) {
    this.msMsScore = msMsScore;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return lipidAnnotation.getAnnotation();
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    lipidAnnotation.saveToXML(writer);
    writer.writeStartElement(XML_ACCURATE_MZ);
    writer.writeCharacters(accurateMz.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_IONIZATION_TYPE);
    writer.writeCharacters(ionizationType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_MATCHED_FRAGMENTS);
    if (matchedFragments != null) {
      for (LipidFragment lipidFragment : matchedFragments) {
        lipidFragment.saveToXML(writer);
      }
    } else {
      writer.writeCharacters(StringUtils.EMPTY);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_MSMS_SCORE);
    writer.writeCharacters(msMsScore.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_COMMENT);
    if (comment != null) {
      writer.writeCharacters(comment);
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();

    writer.writeEndElement();

  }

  public static MatchedLipid loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load matched lipid from the current element. Wrong name.");
    }

    ILipidAnnotation lipidAnnotation = null;
    Double accurateMz = null;
    IonizationType ionizationType = null;
    Set<LipidFragment> lipidFragments = null;
    Double msMsScore = null;
    String comment = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_ANNOTATION_ELEMENT:
          if (reader.getAttributeValue(null, XML_LIPID_ANNOTATION_ELEMENT)
              .equals(LipidAnnotationLevel.SPECIES_LEVEL.name())) {
            lipidAnnotation = SpeciesLevelAnnotation.loadFromXML(reader);
          } else if (reader.getAttributeValue(null, XML_LIPID_ANNOTATION_ELEMENT)
              .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.name())) {
            lipidAnnotation = MolecularSpeciesLevelAnnotation.loadFromXML(reader);
          }
          break;
        case XML_ACCURATE_MZ:
          accurateMz = Double.parseDouble(reader.getElementText());
          break;
        case XML_IONIZATION_TYPE:
          ionizationType = ParsingUtils.ionizationNameToIonizationType(reader.getElementText());
          break;
        case XML_MATCHED_FRAGMENTS:
          lipidFragments = loadLipidFragmentsFromXML(reader, possibleFiles);
          break;
        case XML_MSMS_SCORE:
          msMsScore = Double.parseDouble(reader.getElementText());
          break;
        case XML_COMMENT:
          comment = reader.getElementText();
          break;
        default:
          break;
      }
    }

    MatchedLipid matchedLipid =
        new MatchedLipid(lipidAnnotation, accurateMz, ionizationType, lipidFragments, msMsScore);
    if (comment != null) {
      matchedLipid.setComment(comment);
    }
    return matchedLipid;
  }

  private static Set<LipidFragment> loadLipidFragmentsFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_MATCHED_FRAGMENTS))) {
      throw new IllegalStateException(
          "Cannot load matched lipid fragments from the current element. Wrong name.");
    }

    Set<LipidFragment> lipidFragments = new HashSet<>();
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_MATCHED_FRAGMENTS))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      lipidFragments.add(LipidFragment.loadFromXML(reader, possibleFiles));

    }
    return lipidFragments;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((accurateMz == null) ? 0 : accurateMz.hashCode());
    result = prime * result + ((comment == null) ? 0 : comment.hashCode());
    result = prime * result + ((ionizationType == null) ? 0 : ionizationType.hashCode());
    result = prime * result + ((lipidAnnotation.getAnnotation() == null) ? 0
        : lipidAnnotation.getAnnotation().hashCode());
    result = prime * result + ((matchedFragments == null) ? 0 : matchedFragments.hashCode());
    result = prime * result + ((msMsScore == null) ? 0 : msMsScore.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MatchedLipid other = (MatchedLipid) obj;
    if (accurateMz == null) {
      if (other.accurateMz != null)
        return false;
    } else if (!accurateMz.equals(other.accurateMz))
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (ionizationType != other.ionizationType)
      return false;
    if (lipidAnnotation.getAnnotation() == null) {
      if (other.lipidAnnotation.getAnnotation() != null)
        return false;
    } else if (!lipidAnnotation.getAnnotation().equals(other.lipidAnnotation.getAnnotation()))
      return false;
    if (matchedFragments == null) {
      if (other.matchedFragments != null)
        return false;
    } else if (!matchedFragments.equals(other.matchedFragments))
      return false;
    if (msMsScore == null) {
      if (other.msMsScore != null)
        return false;
    } else if (!msMsScore.equals(other.msMsScore))
      return false;
    return true;
  }


}
