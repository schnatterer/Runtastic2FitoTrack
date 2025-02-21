/*
 * Copyright (c) 2020 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.export;

import java.util.List;

import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;

public class IntervalSetContainer {

    private IntervalSet set;
    private List<Interval> intervals;

    public IntervalSetContainer() {
    }

    public IntervalSetContainer(IntervalSet set, List<Interval> intervals) {
        this.set = set;
        this.intervals = intervals;
    }

    public IntervalSet getSet() {
        return set;
    }

    public void setSet(IntervalSet set) {
        this.set = set;
    }

    public List<Interval> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<Interval> intervals) {
        this.intervals = intervals;
    }
}
