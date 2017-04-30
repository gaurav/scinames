/*
 *
 *  Validator
 *  Copyright (C) 2017 Gaurav Vaidya
 *
 *  This file is part of SciNames.
 *
 *  SciNames is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SciNames is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SciNames.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ggvaidya.scinames.validation;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Project;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validate individual changes.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeValidator implements Validator {
	@Override
	public Stream<ValidationError> validate(Project p) {
		return Stream.concat(
			getIncorrectAdditionsAndDeletions(p),
			Stream.concat(
				getIncorrectLumpsAndSplits(p),
				Stream.concat(
					checkFromWasPreviouslyRecognized(p),
					changesOfNonRecognizedTypes(p)
				)
			)
		);
	}
	
	private Stream<ValidationError> getIncorrectAdditionsAndDeletions(Project p) {
		return p.getChanges()
			.filter(ch -> {
				if(ch.getType().equals(Change.ADDITION)) {
					return !(ch.getFrom().isEmpty() && !ch.getTo().isEmpty());
				} else if(ch.getType().equals(Change.DELETION)) {
					return !(!ch.getFrom().isEmpty() && ch.getTo().isEmpty());
				} else
					return false;
			})
			.map(ch -> new ValidationError<Change>(this, p, "Incorrect addition or deletion", ch));
	}
	
	private Stream<ValidationError> getIncorrectLumpsAndSplits(Project p) {
		return p.getChanges()
			.filter(ch -> {
				if(ch.getType().equals(Change.LUMP)) {
					return !(ch.getFrom().size() > ch.getTo().size());
				} else if(ch.getType().equals(Change.SPLIT)) {
					return !(ch.getFrom().size() < ch.getTo().size());
				} else
					return false;
			})
			.map(ch -> new ValidationError<Change>(this, p, "Incorrect lump or split", ch));
	}
	
	private Stream<ValidationError> checkFromWasPreviouslyRecognized(Project p) {
		return p.getChanges()
			.filter(ch -> {
				Dataset prev = ch.getDataset().getPreviousDataset();
				
				if(prev == null)
					return false;
				
				Set<Name> prevNames = p.getRecognizedNames(prev);
				
				return !ch.getFromStream().allMatch(n -> prevNames.contains(n));
			})
			.map(ch -> new ValidationError<Change>(this, p, "'From' not previously recognized", ch));
	}
	
	private Stream<ValidationError> changesOfNonRecognizedTypes(Project p) {
		return p.getChanges()
			.filter(ch -> !Change.RECOGNIZED_TYPES.contains(ch.getType()))
			.map(ch -> new ValidationError<Change>(this, p, "Change type '" + ch.getType().toString() + "' not recognized", ch));	
	}

	@Override
	public String getName() {
		return "Change validator";
	}
}
