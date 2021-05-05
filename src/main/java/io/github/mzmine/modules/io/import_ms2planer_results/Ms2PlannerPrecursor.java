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

package io.github.mzmine.modules.io.import_ms2planer_results;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
//columns={0: 'Mass [m/z]',1: 'mz_isolation',2: 'duration',3: 'rt_start',4: 'rt_end', 5: 'intensity', 6: 'rt_apex',7: 'charge'})
public record Ms2PlannerPrecursor(int path, double mz, double mz_isolation, double duration, double rt_start,
                                  double rt_end, double intensity, double rt_apex, int charge) {

}
