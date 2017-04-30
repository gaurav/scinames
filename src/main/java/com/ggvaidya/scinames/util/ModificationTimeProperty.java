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
package com.ggvaidya.scinames.util;

import java.time.Instant;
import javafx.beans.property.SimpleObjectProperty;

/**
 * An interface for objects that may be edited in place. Allows 
 * other objects to track when they are modified.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ModificationTimeProperty extends SimpleObjectProperty<Instant> {
	private Instant lastSaved;
	
	/**
	 * Create a new modification time property.
	 */
	public ModificationTimeProperty() {
		Instant now = Instant.now();
		lastSaved = now;
		set(now);
	}
	
	/**
	 * Let this modification time property know that the object it's tracking
	 * has been modified.
	 */
	public void modified() {
		this.set(Instant.now());
	}
	
	/**
	 * Let this modification time property know that this object has been
	 * saved to disk, so no modification is necessary.
	 */
	public void saved() {
		Instant now = Instant.now();
		lastSaved = now;
		this.set(now);
	}
	
	/**
	 * Check whether this modification time property has changed since it was 
	 * last saved.
	 * 
	 * @return True if modified, false otherwise.
	 */
	public boolean isModified() {
		return lastSaved.isBefore(get());
	}
}
