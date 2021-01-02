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

package io.github.mzmine.parameters.parametertypes;

import java.util.Arrays;
// import org.apache.commons.lang3.ArrayUtils;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class ListDoubleComponent extends GridPane {
  private TextField inputField;
  private Label textField;

  public ListDoubleComponent() {
    inputField = new TextField();
    inputField.setPrefColumnCount(16);

    /*
     * inputField.getDocument().addDocumentListener(new DocumentListener() {
     * 
     * @Override public void changedUpdate(DocumentEvent e) { update(); }
     * 
     * @Override public void removeUpdate(DocumentEvent e) { update(); }
     * 
     * @Override public void insertUpdate(DocumentEvent e) { update(); } });
     */

    textField = new Label();
    // textField.setColumns(8);

    add(inputField, 0, 0);
    add(textField, 0, 1);
  }

  public List<Double> getValue() {
    try {
      String values = textField.getText().replaceAll("\\s", "");
      String[] strValues = values.split(",");
      double[] doubleValues = new double[strValues.length];
      for (int i = 0; i < strValues.length; i++) {
        try {
          doubleValues[i] = Double.parseDouble(strValues[i]);
        } catch (NumberFormatException nfe) {
          // The string does not contain a parsable integer.
        }
      }
      Double[] doubleArray = ArrayUtils.toObject(doubleValues);
      List<Double> ranges = Arrays.asList(doubleArray);
      return ranges;
    } catch (Exception e) {
      return null;
    }
  }

  public void setValue(List<Double> ranges) {
    String[] strValues = new String[ranges.size()];

    for (int i = 0; i < ranges.size(); i++) {
      strValues[i] = Double.toString(ranges.get(i));
    }
    String text = String.join(",", strValues);

    // textField.setForeground(Color.black);
    textField.setText(text);
    inputField.setText(text);
  }

  public void setToolTipText(String toolTip) {
    textField.setTooltip(new Tooltip(toolTip));
    inputField.setTooltip(new Tooltip(toolTip));
  }

  private void update() {
    try {
      String values = inputField.getText().replaceAll("\\s", "");
      String[] strValues = values.split(",");

      String text = String.join(",", strValues);

      // textField.setFont(new );.setForeground(Color.black);
      textField.setText(text);
    } catch (IllegalArgumentException e) {
      // textField.setForeground(Color.red);
      textField.setText(e.getMessage());
    }
  }
}
