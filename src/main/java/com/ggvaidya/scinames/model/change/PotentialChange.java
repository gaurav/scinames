/*
 * Copyright (C) 2017 Gaurav Vaidya <gaurav@ggvaidya.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ggvaidya.scinames.model.change;

import com.ggvaidya.scinames.model.Name;

import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;

/**
 * A Potential Change is a change, but hasn't been applied to a particular dataset yet.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class PotentialChange extends Change {
	public PotentialChange(Dataset ds, ChangeType type, Stream<Name> fromStream, Stream<Name> toStream) {
		super(ds, type, fromStream, toStream, false);
	}
	
	public void submit() {
		registerToDataset();
		getDataset().explicitChangesProperty().add(this);
	}
	
	public void cancel() {
		// Luckily, we don't have to do anything!
	}
}
