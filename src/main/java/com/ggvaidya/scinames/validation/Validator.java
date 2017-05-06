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

import com.ggvaidya.scinames.model.Project;
import java.util.stream.Stream;

/**
 * A validator validates the Project for a particular type of consistency
 * error, and returns a Stream of ValidationErrors.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public interface Validator {
	public String getName();
	
	@SuppressWarnings("rawtypes")
	public Stream<ValidationError> validate(Project p);
}
