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
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.Project;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
		List<ValidationError> errors = new LinkedList<>();
		
		errors.addAll(getIncorrectAdditionsAndDeletions(p).collect(Collectors.toList()));
		errors.addAll(getIncorrectLumpsAndSplits(p).collect(Collectors.toList()));
		errors.addAll(checkFromWasPreviouslyRecognized(p).collect(Collectors.toList()));
		errors.addAll(changesOfNonRecognizedTypes(p).collect(Collectors.toList()));
		errors.addAll(checkForDuplicateNameClustersOnSameSide(p).collect(Collectors.toList()));
		errors.addAll(getEncodingErrors(p).collect(Collectors.toList()));
		
		return errors.stream();
	}
	
	private Stream<ValidationError> getEncodingErrors(Project p) {
		CharsetDecoder utfDecoder = Charset.forName("UTF8").newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		
		CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
		
		return p.getChanges().flatMap(ch -> {
			String str = ch.toString();
			List<ValidationError> errors = new LinkedList<>();
			
			try {
				utfDecoder.decode(ByteBuffer.wrap(str.getBytes(Charset.forName("UTF8"))));
			} catch(CharacterCodingException ex) {
				errors.add(new ValidationError<Change>(this, p, "Change '" + ch + "' contains invalid UTF-8 characters", ch));
			}
			
			// Specifically, names should be convertible to ASCII.
			if(!asciiEncoder.canEncode(str)) {
				errors.add(new ValidationError<Change>(this, p, "Change '" + ch + "' cannot be rendered in ASCII", ch));
			}
			
			return errors.stream();
		});
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
	
	private Stream<ValidationError> checkForDuplicateNameClustersOnSameSide(Project p) {
		return p.getChanges()
			.flatMap(ch -> Stream.concat(
				checkNameClusters(p, ch, "from", ch.getFrom()), 
				checkNameClusters(p, ch, "to", ch.getTo())
			));
	}
	
	/**
	 * Helper function. Checks to see if any of these sets of names (intended to be either all the
	 * getFrom()s or getTo()s from a list of changes) contains the same name cluster more than once.
	 * 
	 * @param sets
	 * @return
	 */
	private Stream<ValidationError> checkNameClusters(Project p, Change ch, String nameSetName, Set<Name> names) {
		Map<NameCluster, Name> clustersSeen = new HashMap<>();
		
		return names.stream().flatMap(name -> {
			Optional<NameCluster> optCluster = p.getNameClusterManager().getCluster(name);
			if(!optCluster.isPresent()) 
				return Stream.of(
					new ValidationError<Change>(this, p, "Change " + ch + " contains name '" + name + "' missing a name cluster", ch)
				);
			else {
				NameCluster cluster = optCluster.get();
				Stream<ValidationError> errors = Stream.empty();
				
				if(clustersSeen.containsKey(cluster)) {
					errors = Stream.concat(errors, Stream.of(
						new ValidationError<Change>(this, p, 
							"Name cluster repeats twice in change " + ch + " in " + nameSetName + 
							": first as " + clustersSeen.get(cluster) + ", then as " + name, 
							ch)
					));
				}
				
				clustersSeen.put(cluster, name);
				return errors;
			}
		});
	}

	@Override
	public String getName() {
		return "Change validator";
	}
}
