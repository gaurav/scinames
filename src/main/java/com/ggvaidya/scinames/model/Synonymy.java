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
	private Dataset dataset;
	private Name fromName;
	private Name toName;
	private String note;

	public Synonymy(Name n1, Name n2, Dataset foundIn) {
		this(n1, n2, foundIn, null);
	}
	
	public Synonymy(Name n1, Name n2, Dataset foundIn, String n) {
		super(foundIn, n1, n2);
		
		fromName = n1;
		toName = n2;
		dataset = foundIn;
		note = n;
	}
	
	public Dataset getDataset() { return dataset; }
	public Name getFrom() { return fromName; }
	public Name getTo() { return toName; }
	public String getNote() { return (note == null) ? "" : note; }

	@Override
	public String toString() {
		return "Synonymy from '" + fromName + 
			"' to '" + toName + "' in dataset " + dataset + 
			(note == null ? "" : " (note: " + note + ")");
	}
	
	/**
	 * Compare to another Synonymy. Note that the order of the names is important,
	 * so Synonymy(n1, n2, ds) is different from Synonymy(n2, n1, ds). Notes are
	 * ignored in comparison.
	 */
	public boolean equals(Synonymy other) {
		if(other.dataset == dataset && other.fromName == fromName && other.toName == toName)
			return true;
		else
			return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Synonymy)
			return equals((Synonymy)obj);
		else if(obj instanceof NameCluster)
			return equals((NameCluster)obj);
		else
			return false;
	}

	// Based on http://stackoverflow.com/a/113600/27310
	public int hashCode() {
		int result = 918731;
		
		result = 37 * result + dataset.hashCode();
		result = 37 * result + fromName.hashCode();
		result = 37 * result + toName.hashCode();
		
		return result;
	}
}