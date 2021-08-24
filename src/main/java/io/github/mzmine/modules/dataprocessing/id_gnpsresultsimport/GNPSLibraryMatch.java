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

package io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport;

import java.text.MessageFormat;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identity of GNPS library matching results imported from a graphml network
 */
public class GNPSLibraryMatch {

  private final String identity;
  private final HashMap<String, Object> results;
  private final int nodeID;

  public GNPSLibraryMatch(HashMap<String, Object> results, String compound, int nodeID) {
    this.results = results;
    String adduct = getResultOr(ATT.ADDUCT, "").toString();
    String score = getResultOr(ATT.LIBRARY_MATCH_SCORE, "NO_SCORE").toString();
    identity = MessageFormat.format("{0}; {1}; cos={2}", compound, adduct, score);
    this.nodeID = nodeID;
  }

  @NotNull
  public HashMap<String, Object> getResults() {
    return results;
  }

  /**
   * Retrieve results for a specific attribute
   *
   * @param att the attribute to retrieve a field
   * @return the result for this attribute or null
   */
  @Nullable
  public Object getResult(ATT att) {
    return results.get(att.getKey());
  }

  /**
   * Get result or return default
   *
   * @param att          the attribute to retrieve a field
   * @param defaultValue the value to replace null
   * @return if result is null return default
   */
  @NotNull
  public Object getResultOr(ATT att, @NotNull Object defaultValue) {
    Object result = getResult(att);
    return result == null ? defaultValue : result;
  }

  public double getMatchScore() {
    String s = getResultOr(ATT.LIBRARY_MATCH_SCORE, "0").toString();
    return Double.parseDouble(s);
  }

  public int getSharedSignals() {
    String s = getResultOr(ATT.SHARED_SIGNALS, "0").toString();
    return Integer.parseInt(s);
  }

  @Override
  public String toString() {
    return identity;
  }

  public int getNodeID() {
    return nodeID;
  }

  /**
   * Different node attributes from GNPS FBMN or IIMN
   */
  public enum ATT {
    CLUSTER_INDEX("cluster index", Integer.class), // GNPS cluster -
    // similarity
    COMPOUND_NAME("Compound_Name", String.class), // library match
    ADDUCT("Adduct", String.class), // from GNPS library match
    MASS_DIFF("MassDiff", Double.class), // absolute diff from gnps
    LIBRARY_MATCH_SCORE("MQScore", Double.class), // cosine score to library
    // spec
    SHARED_SIGNALS("SharedPeaks", String.class), // shared signals library
    // <-> query
    INCHI("INCHI", String.class), SMILES("Smiles", String.class), // structures
    IONSOURCE("Ion_Source", String.class), IONMODE("IonMode",
        String.class), INSTRUMENT("Instrument", String.class), // instrument
    GNPS_LIBRARY_URL("GNPSLibraryURL", String.class), // link to library
    // entry
    GNPS_NETWORK_URL("GNPSLinkout_Network", String.class), // link to
    // network
    GNPS_CLUSTER_URL("GNPSLinkout_Cluster", String.class), // link to
    // cluster
    COMPOUND_SOURCE("Compound_Source", String.class), // isolated, ...
    DATA_COLLECTOR("Data_Collector", String.class), //
    PI("PI", String.class), //

    // node specific keys
    COMPONENT_INDEX("componentindex", Integer.class), //
    PRECURSOR_MASS("precursor mass", Double.class), // precursor
    NEUTRAL_M_MASS("neutral M mass", Double.class), // neutral mass by ion identity networking
    LIBRARY_CLASS("Library_Class", String.class), //
    SPECTRUM_ID("SpectrumID", String.class); // spectrum id

    private final String key;
    private final Class c;

    ATT(String key, Class c) {
      this.c = c;
      this.key = key;
    }

    public Class getValueClass() {
      return c;
    }

    public String getKey() {
      return key;
    }
  }
}
