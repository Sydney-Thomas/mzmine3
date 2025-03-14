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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases;

import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Range;

import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBCompound;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.InetUtils;
import io.github.mzmine.util.RangeUtils;

public class MassBankJapanGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // Unfortunately, we need to list all the instrument types here
  private static final String instrumentTypes[] = {"APCI-ITFT", "APCI-ITTOF", "CE-ESI-TOF", "CI-B",
      "EI-B", "EI-EBEB", "ESI-ITFT", "ESI-ITTOF", "ESI-QTOF", "FAB-B", "FAB-EB", "FAB-EBEB", "FD-B",
      "FI-B", "GC-EI-QQ", "GC-EI-TOF", "LC-APCI-QTOF", "LC-APPI-QQ", "LC-ESI-IT", "LC-ESI-ITFT",
      "LC-ESI-ITTOF", "LC-ESI-Q", "LC-ESI-QFT", "LC-ESI-QIT", "LC-ESI-QQ", "LC-ESI-QTOF",
      "LC-ESI-TOF", "MALDI-QIT", "MALDI-TOF", "MALDI-TOFTOF"};

  private static final String massBankSearchAddress =
      "http://www.massbank.jp/jsp/Result.jsp?type=quick";
  private static final String massBankEntryAddress = "http://www.massbank.jp/jsp/FwdRecord.jsp?id=";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    StringBuilder queryAddress = new StringBuilder(massBankSearchAddress);

    for (String inst : instrumentTypes) {
      queryAddress.append("&inst=");
      queryAddress.append(inst);
    }

    queryAddress.append("&mz=");
    queryAddress.append(RangeUtils.rangeCenter(toleranceRange));
    queryAddress.append("&tol=");
    queryAddress.append(RangeUtils.rangeLength(toleranceRange) / 2.0);

    URL queryURL = new URL(queryAddress.toString());

    // Submit the query
    logger.finest("Querying URL " + queryURL);
    String queryResult = InetUtils.retrieveData(queryURL);

    Vector<String> results = new Vector<String>();

    // Find IDs in the HTML data
    Pattern pat = Pattern.compile("&nbsp;&nbsp;&nbsp;&nbsp;([A-Z0-9]{8})&nbsp;");
    Matcher matcher = pat.matcher(queryResult);
    while (matcher.find()) {
      String MID = matcher.group(1);
      results.add(MID);
      if (results.size() == numOfResults)
        break;
    }

    return results.toArray(new String[0]);

  }

  /**
   * This method retrieves the details about the compound
   * 
   */
  public DBCompound getCompound(String ID, ParameterSet parameters) throws IOException {

    URL entryURL = new URL(massBankEntryAddress + ID);

    // Retrieve data
    logger.finest("Querying URL " + entryURL);
    String massBankEntry = InetUtils.retrieveData(entryURL);

    String compoundName = null;
    String compoundFormula = null;
    URL structure2DURL = null;
    URL structure3DURL = null;
    URL databaseURL = entryURL;

    // Find compound name
    Pattern patName = Pattern.compile("RECORD_TITLE: (.*)");
    Matcher matcherName = patName.matcher(massBankEntry);
    if (matcherName.find()) {
      compoundName = matcherName.group(1);
    }

    // Find compound formula
    Pattern patFormula = Pattern.compile("CH\\$FORMULA: .*>(.+)</a>");
    Matcher matcherFormula = patFormula.matcher(massBankEntry);
    if (matcherFormula.find()) {
      compoundFormula = matcherFormula.group(1);
    }

    if (compoundName == null) {
      logger
          .warning("Could not parse compound name for compound " + ID + ", ignoring this compound");
      return null;
    }

    DBCompound newCompound = null; // new
                                   // DBCompound(OnlineDatabases.MASSBANKJapan,
                                   // ID, compoundName, compoundFormula,
                                   // databaseURL, structure2DURL,
                                   // structure3DURL);

    return newCompound;

  }
}
