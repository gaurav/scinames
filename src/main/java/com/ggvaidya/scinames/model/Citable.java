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
package com.ggvaidya.scinames.model;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * A citation to a publication. All citation objects have dates, with '0'
 * representing unknown values, as well as a string representation of the
 * citation itself.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public interface Citable {
	public String getCitation();
	public SimplifiedDate getDate();
	public void setDate(SimplifiedDate sd);
}
