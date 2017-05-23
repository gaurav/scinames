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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;

/**
 * Validate individual changes.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeValidator implements Validator {
	@SuppressWarnings("rawtypes")
	@Override
	public Stream<ValidationError> validate(Project p) {
		List<ValidationError> errors = new LinkedList<>();
		
		errors.addAll(getIncorrectAdditionsAndDeletions(p).collect(Collectors.toList()));
		errors.addAll(getLumpsAndSplitsWithUnexpectedFromTos(p).collect(Collectors.toList()));
		errors.addAll(getLumpsAndSplitsWithoutSharedConcepts(p).collect(Collectors.toList()));
		errors.addAll(checkFromWasPreviouslyRecognized(p).collect(Collectors.toList()));
		errors.addAll(changesOfNonRecognizedTypes(p).collect(Collectors.toList()));
		errors.addAll(checkForDuplicateNameClustersOnSameSide(p).collect(Collectors.toList()));
		errors.addAll(findDuplicateAdditionsOrDeletions(p).collect(Collectors.toList()));		
		errors.addAll(getEncodingErrors(p).collect(Collectors.toList()));
		
		return errors.stream();
	}
	
	private Stream<ValidationError<Change>> getEncodingErrors(Project p) {
		CharsetDecoder utfDecoder = Charset.forName("UTF8").newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		
		CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
		
		return p.getChanges().flatMap(ch -> {
			String str = ch.toString();
			List<ValidationError<Change>> errors = new LinkedList<>();
			
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
	
	private Stream<ValidationError<Change>> getIncorrectAdditionsAndDeletions(Project p) {
		return p.getChanges()
			.filter(ch -> {
				if(ch.getType().equals(ChangeType.ADDITION)) {
					return !(ch.getFrom().isEmpty() && !ch.getTo().isEmpty());
				} else if(ch.getType().equals(ChangeType.DELETION)) {
					return !(!ch.getFrom().isEmpty() && ch.getTo().isEmpty());
				} else
					return false;
			})
			.map(ch -> new ValidationError<Change>(this, p, "Incorrect addition or deletion", ch));
	}
	
	private Stream<ValidationError<Change>> getLumpsAndSplitsWithoutSharedConcepts(Project p) {
		NameClusterManager ncm = p.getNameClusterManager();
		
		return p.getChanges()
			.filter(ch -> ch.getType().equals(ChangeType.LUMP) || ch.getType().equals(ChangeType.SPLIT))
			.flatMap(ch -> {
				List<NameCluster> fromClusters = ncm.getClusters(ch.getFrom());
				Set<NameCluster> toClusters = new HashSet<>(ncm.getClusters(ch.getTo()));
				
				List<NameCluster> intersection = fromClusters.stream()
					.filter(cluster -> toClusters.contains(cluster))
					.collect(Collectors.toList());
				
				// Should be exactly one shared between the two ends
				if(intersection.size() == 1)
					return Stream.empty();
				else
					return Stream.of(new ValidationError<Change>(this, p, "Intersection between 'from' and 'to' in change is not one: " + intersection, ch));
			});
	}
	
	private Stream<ValidationError<Change>> getLumpsAndSplitsWithUnexpectedFromTos(Project p) {
		return p.getChanges()
			.flatMap(ch -> {
				if(ch.getType().equals(ChangeType.LUMP)) {
					// There should be more in 'from' than in 'to'
					if(ch.getFrom().size() < ch.getTo().size())
						return Stream.of(new ValidationError<Change>(this, p, "Lump results in more names than were lumped", ch));
					else if(ch.getTo().size() != 1)
						return Stream.of(new ValidationError<Change>(this, p, "Lump results in more than one name", ch));
					else
						return Stream.empty();
				} else if(ch.getType().equals(ChangeType.SPLIT)) {
					// There should be more in 'to' than in 'from'
					if(ch.getFrom().size() > ch.getTo().size())
						return Stream.of(new ValidationError<Change>(this, p, "Split results in fewer names than were split", ch));
					else if(ch.getFrom().size() != 1)
						return Stream.of(new ValidationError<Change>(this, p, "Split results in more than one name", ch));
					else
						return Stream.empty();
				} else
					return Stream.empty();
			});
	}
	
	private Stream<ValidationError<Change>> checkFromWasPreviouslyRecognized(Project p) {
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
	
	private Stream<ValidationError<Change>> changesOfNonRecognizedTypes(Project p) {
		return p.getChanges()
			.filter(ch -> !ChangeType.RECOGNIZED_TYPES.contains(ch.getType()))
			.map(ch -> new ValidationError<Change>(this, p, "Change type '" + ch.getType().toString() + "' not recognized", ch));	
	}
	
	private Stream<ValidationError<Change>> checkForDuplicateNameClustersOnSameSide(Project p) {
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
	private Stream<ValidationError<Change>> checkNameClusters(Project p, Change ch, String nameSetName, Set<Name> names) {
		Map<NameCluster, Name> clustersSeen = new HashMap<>();
		
		return names.stream().flatMap(name -> {
			Optional<NameCluster> optCluster = p.getNameClusterManager().getCluster(name);
			if(!optCluster.isPresent()) 
				return Stream.of(
					new ValidationError<Change>(this, p, "Change " + ch + " contains name '" + name + "' missing a name cluster", ch)
				);
			else {
				NameCluster cluster = optCluster.get();
				Stream<ValidationError<Change>> errors = Stream.empty();
				
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
	
	/**
	 * Find cases where a name is added or deleted from a single checklist in multiple changes,
	 * for example, a rename and a delete that both remove the same name.
	 * 
	 * @param p
	 * @return
	 */
	private Stream<ValidationError<Dataset>> findDuplicateAdditionsOrDeletions(Project p) {
		return p.getDatasets().stream().flatMap(ds -> findDuplicateAdditionsOrDeletions(p, ds));
	}
	
	private Stream<ValidationError<Dataset>> findDuplicateAdditionsOrDeletions(Project p, Dataset ds) {
		Map<Name, Long> namesDeleted = ds.getChanges(p).flatMap(ch -> ch.getFromStream())
			.collect(Collectors.groupingBy(
				Function.identity(),
				Collectors.counting()
			));
		Map<Name, Long> namesAdded = ds.getChanges(p).flatMap(ch -> ch.getToStream())
			.collect(Collectors.groupingBy(
				Function.identity(),
				Collectors.counting()
			));
		
		return Stream.concat(
			// Duplicate additions.
			namesDeleted.entrySet().stream().filter(entry -> entry.getValue().longValue() > 1).map(entry -> {
				List<Change> changes = ds.getChanges(p).filter(ch -> ch.getFrom().contains(entry.getKey())).collect(Collectors.toList());
				
				return new ValidationError<>(this, p, "Name '" + entry.getKey() + "' deleted " + entry.getValue() + " times in " + changes, ds);
			}),
			
			// Duplicate deletions.
			namesAdded.entrySet().stream().filter(entry -> entry.getValue().longValue() > 1).map(entry -> {
				List<Change> changes = ds.getChanges(p).filter(ch -> ch.getTo().contains(entry.getKey())).collect(Collectors.toList());
				
				return new ValidationError<>(this, p, "Name '" + entry.getKey() + "' added " + entry.getValue() + " times in " + changes, ds);
			})
		);
	}

	@Override
	public String getName() {
		return "Change validator";
	}
}
