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
import org.jfree.chart.renderer.LookupPaintScale;
import com.google.common.collect.Range;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScale extends LookupPaintScale {

  private PaintScaleColorStyle paintScaleColorStyle;
  private PaintScaleBoundStyle paintScaleBoundStyle;

  public PaintScale(Range<Double> scaleRange) {
    super(scaleRange.lowerEndpoint(), scaleRange.upperEndpoint(), Color.BLACK);
  }

  public PaintScale(PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle, Range<Double> scaleRange) {
    super(scaleRange.lowerEndpoint(), scaleRange.upperEndpoint(), Color.BLACK);
    this.paintScaleColorStyle = paintScaleColorStyle;
    this.paintScaleBoundStyle = paintScaleBoundStyle;
  }

  public PaintScaleColorStyle getPaintScaleColorStyle() {
    return paintScaleColorStyle;
  }

  public void setPaintScaleColorStyle(PaintScaleColorStyle paintScaleColorStyle) {
    this.paintScaleColorStyle = paintScaleColorStyle;
  }

  public PaintScaleBoundStyle getPaintScaleBoundStyle() {
    return paintScaleBoundStyle;
  }

  public void setPaintScaleBoundStyle(PaintScaleBoundStyle paintScaleBoundStyle) {
    this.paintScaleBoundStyle = paintScaleBoundStyle;
  }

  @Override
  public String toString() {
    return paintScaleColorStyle.toString();
  }

}
