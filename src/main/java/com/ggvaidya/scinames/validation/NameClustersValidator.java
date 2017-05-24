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
import com.ggvaidya.scinames.model.Project;

/**
 * Validate individual changes.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameClustersValidator implements Validator {
	@SuppressWarnings("rawtypes")
	@Override
	public Stream<ValidationError> validate(Project p) {
		List<ValidationError> errors = new LinkedList<>();
		
		errors.addAll(ensureNameClustersAreUnique(p).collect(Collectors.toList()));
		
		// - This results in lots of spurious errors, so we're ignoring this for now.
		// errors.addAll(identifyNameClustersWithMultipleBinomials(p).collect(Collectors.toList()));
		
		return errors.stream();
	}
	
	private Stream<ValidationError<NameCluster>> ensureNameClustersAreUnique(Project p) {
		Map<Name, NameCluster> nameClustersByName = new HashMap<>(); 
		LinkedList<ValidationError<NameCluster>> errors = new LinkedList<>();
		
		for(NameCluster nc: p.getNameClusterManager().getClusters().collect(Collectors.toList())) {
			for(Name n: nc.getNames()) {
				if(nameClustersByName.containsKey(n))
					errors.add(new ValidationError<NameCluster>(this, p, "Name '" + n + "' found in multiple name clusters: " + nc + ", " + nameClustersByName.get(n), nc));
				
				nameClustersByName.put(n, nc);
			}
		}
		
		return errors.stream();
	}
	
	private Stream<ValidationError<NameCluster>> identifyNameClustersWithMultipleBinomials(Project p) {
		Map<Name, NameCluster> nameClustersByName = new HashMap<>(); 
		LinkedList<ValidationError<NameCluster>> errors = new LinkedList<>();
		
		for(NameCluster nc: p.getNameClusterManager().getClusters().collect(Collectors.toList())) {
			List<Name> binomials = nc.getNames().stream().flatMap(n -> n.asBinomial()).distinct().collect(Collectors.toList());
			
			if(binomials.size() > 1)
				errors.add(new ValidationError<NameCluster>(this, p, "Name cluster contains multiple binomials: " + binomials, nc));
		}
		
		return errors.stream();
	}
	
	@Override
	public String getName() {
		return "Name cluster validator";
	}
}
