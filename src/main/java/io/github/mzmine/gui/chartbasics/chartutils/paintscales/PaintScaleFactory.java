/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General private License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * private License for more details.
 * 
 * You should have received a copy of the GNU General private License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.gui.chartbasics.chartutils.paintscales;

import java.awt.Color;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScaleFactory {

  public PaintScale createColorsForPaintScale(PaintScale paintScale) {
    Color[] colors = calculateColorsForPaintScale(paintScale.getPaintScaleColorStyle(),
        paintScale.getPaintScaleBoundStyle());
    double delta = (paintScale.getUpperBound() - paintScale.getLowerBound()) / (colors.length - 1);
    double value = paintScale.getLowerBound();
    for (Color color : colors) {
      paintScale.add(value, color);
      value = value + delta;
    }
    return paintScale;
  }

  private Color[] calculateColorsForPaintScale(PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle) {
    switch (paintScaleColorStyle) {
      case CYAN:
        return getCyanScale(paintScaleBoundStyle);
      case GREEN:
        return getGreenScale(paintScaleBoundStyle);
      case RAINBOW:
        return getRainbowScale();
      case GRREN_RED:
        return getGreenYellowRedScale(paintScaleBoundStyle);
      case RED:
        return getRedScale(paintScaleBoundStyle);
      case YELLOW:
        return getYellowScale(paintScaleBoundStyle);
      default:
        return getRainbowScale();
    }

  }

  /*
   * returns an array with cyan colors
   */
  public Color[] getCyanScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      cyanScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < cyanScale.length - adjustedLowerBound; i++) {
      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);
    }
    for (int i = cyanScale.length - adjustedUpperBound; i < cyanScale.length; i++) {
      cyanScale[i] = new Color(244, 66, 223);
    }

    return cyanScale;
  }

  /*
   * returns an array with green colors
   */
  public Color[] getGreenScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      greenScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < greenScale.length - adjustedLowerBound; i++) {
      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);
    }
    for (int i = greenScale.length - adjustedUpperBound; i < greenScale.length; i++) {
      greenScale[i] = new Color(244, 66, 223);
    }

    return greenScale;
  }

  /*
   * returns an array with rainbow colors
   */
  private Color[] getRainbowScale() {
    Color[] rainbow = new Color[1500];
    int r = 0;
    int g = 0;
    int b = 0;
    for (int i = 0; i < 255; i++) {
      b = i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 255; i < 510; i++) {
      g = i - 255;
      b = 255;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 510; i < 735; i++) {
      g = 255;
      b = 735 - i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 735; i < 990; i++) {
      r = i - 735;
      g = 255;
      b = 0;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 990; i < 1245; i++) {
      r = 255;
      g = 1245 - i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 1245; i < 1500; i++) {
      r = 255;
      g = 0;
      b = i - 1245;
      rainbow[i] = new Color(r, g, b);
    }

    return rainbow;
  }


  /*
   * returns an array with rainbow colors
   */
  private Color[] getGreenYellowRedScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 100;
    Color[] scale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      scale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < scale.length - adjustedLowerBound; i++) {
      scale[i] = new Color((255 * i / 100), ((255 * (100 - i)) / 100), 0);
    }
    for (int i = scale.length - adjustedUpperBound; i < scale.length; i++) {
      scale[i] = new Color(244, 66, 223);
    }

    return scale;
  }

  /*
   * returns an array with red colors
   */
  public Color[] getRedScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      redScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < redScale.length - adjustedLowerBound; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }
    for (int i = redScale.length - adjustedUpperBound; i < redScale.length; i++) {
      redScale[i] = new Color(244, 66, 223);
    }

    return redScale;
  }

  /*
   * returns an array with yellow colors
   */
  public Color[] getYellowScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      yellowScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < yellowScale.length - adjustedLowerBound; i++) {
      yellowScale[i] = new Color(255, 255, yellowScale.length - i - 1);
    }
    for (int i = yellowScale.length - adjustedUpperBound; i < yellowScale.length; i++) {
      yellowScale[i] = new Color(244, 66, 223);
    }

    return yellowScale;
  }

  private int adjustLowerBound(PaintScaleBoundStyle paintScaleBoundStyle) {
    switch (paintScaleBoundStyle) {
      case LOWER_AND_UPPER_BOUND:
        return 5;
      case LOWER_BOUND:
        return 5;
      case NONE:
        return 0;
      case UPPER_BOUND:
        return 0;
      default:
        return 0;
    }
  }

  private int adjustUpperBound(PaintScaleBoundStyle paintScaleBoundStyle) {
    switch (paintScaleBoundStyle) {
      case LOWER_AND_UPPER_BOUND:
        return 5;
      case LOWER_BOUND:
        return 0;
      case NONE:
        return 0;
      case UPPER_BOUND:
        return 5;
      default:
        return 0;
    }
  }

  public Color[] scaleAlphaForPaintScale(Color[] colors) {
    Color[] colorsWithScaledAlpha = new Color[colors.length];
    for (int i = 0; i < colors.length; i++) {
      int alpha;
      alpha = scaleAlphaValueLinear(1, colors.length, i);
      colorsWithScaledAlpha[i] =
          new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), alpha);
    }
    return colorsWithScaledAlpha;
  }

  private int scaleAlphaValueLinear(int min, int max, int value) {
    if (min == max) {
      return 1;
    } else {
      int maxScaled = 255;
      int minScaled = 1;
      double a = (maxScaled - minScaled) / ((double) max - min);
      double b = maxScaled - a * max;
      return (int) (a * value + b);
    }
  }

}
