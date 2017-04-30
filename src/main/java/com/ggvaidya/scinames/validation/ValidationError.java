/*
 *
 *  ValidationError
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
import com.ggvaidya.scinames.model.Project;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An instance of an error reported by a validator. This will be displayed
 * in a tabular view, so it'd better be easy to read!
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ValidationError<T> {
	private Project project;
	public Project getProject() { return project; }
	
	private Validator validator;
	public Validator getValidator() { return validator; }
	
	private String message;
	public String getMessage() { return message; }
	
	private T target;
	public T getTarget() { return target; }
	
	public Optional<Change> getChange() { 
		if(Change.class.isAssignableFrom(target.getClass())) 
			return Optional.of((Change) target);
		else
			return Optional.empty();
	}
	
	public Optional<Dataset> getDataset() { 
		if(Dataset.class.isAssignableFrom(target.getClass())) 
			return Optional.of((Dataset) target);
		else if(Change.class.isAssignableFrom(target.getClass()))
			return Optional.of(((Change)target).getDataset());
		else
			return Optional.empty();
	}
	
	public ValidationError(Validator v, Project p, String m, T t) {
		validator = v;
		project = p;
		message = m;
		target = t;
	}
}
