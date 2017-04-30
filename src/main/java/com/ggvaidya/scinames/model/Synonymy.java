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

/**
 * A synonymy is a statement that one name is synonymous with another.
 * It's actually just a NameCluster! So neat.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Synonymy extends NameCluster {
	public Synonymy(Name from, Name to, Dataset foundIn) {
		super(foundIn, from, to);
	}
}