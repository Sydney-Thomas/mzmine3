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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBCompound;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.InetUtils;

public class MetaCycGateway implements DBGateway {

  private static final String metaCycSearchAddress =
      "https://websvc.biocyc.org/META/monoisotopicwt?wts=";
  private static final String metaCycObjectAddress = "https://websvc.biocyc.org/getxml?META:";
  private static final String metaCycEntryAddress =
      "https://websvc.biocyc.org/compound?orgid=META&id=";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    final double ppmTolerance = mzTolerance.getPpmToleranceForMass(mass);

    final String queryAddress = metaCycSearchAddress + mass + "&tol=" + ppmTolerance;

    final URL queryURL = new URL(queryAddress);

    final Logger logger = Logger.getLogger(this.getClass().getName());

    // Submit the query
    logger.finest("Retrieving " + queryAddress);
    final String queryResult = InetUtils.retrieveData(queryURL);

    final List<String> results = new ArrayList<String>();
    BufferedReader lineReader = new BufferedReader(new StringReader(queryResult));
    String line;
    while ((line = lineReader.readLine()) != null) {
      String split[] = line.split("\\t");
      if (split.length < 5)
        continue;
      String id = split[4];
      results.add(id);
      if (results.size() == numOfResults)
        break;
    }

    return results.toArray(new String[0]);

  }

  /**
   * This method retrieves the details about PlantCyc compound
   * 
   */
  public DBCompound getCompound(String ID, ParameterSet parameters) throws IOException {

    final String dataURL = metaCycObjectAddress + ID;

    try {

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();
      Document parsedResult = builder.parse(dataURL);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      XPathExpression expr = xpath.compile("//ptools-xml/Compound/common-name");
      NodeList nameElementNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      Element nameElement = (Element) nameElementNL.item(0);

      if (nameElement == null)
        throw new IOException("Could not parse compound name");

      String compoundName = nameElement.getTextContent();

      expr = xpath.compile("//ptools-xml/Compound/cml/molecule/formula");
      NodeList formulaElementNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      Element formulaElement = (Element) formulaElementNL.item(0);
      String compoundFormula = formulaElement.getAttribute("concise");
      compoundFormula = compoundFormula.replaceAll(" ", "");

      final URL entryURL = new URL(metaCycEntryAddress + ID);

      // Unfortunately MetaCyc does not contain structures in MOL format
      URL structure2DURL = null;
      URL structure3DURL = null;

      if (compoundName == null) {
        throw (new IOException("Invalid compound ID " + ID));
      }

      DBCompound newCompound = new DBCompound(OnlineDatabases.METACYC, ID, compoundName,
          compoundFormula, entryURL, structure2DURL, structure3DURL);

      return newCompound;

    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(e);
    }
  }
}
