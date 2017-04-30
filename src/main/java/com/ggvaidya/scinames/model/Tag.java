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

import java.util.HashMap;
import java.util.Map;

/**
 * A tag is a keyword that can be associated with a Taggable object.
 * A taggable object can have any number of distinct tags.
 * 
 * We primarily use tags for evidence on Changes, but this is designed
 * to be as flexible as possible. It uses singleton objects to ensure
 * that attempts to create the same tag, in whatever namespace, result
 * in the same tag object being returned.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Tag {
	private static final Map<String, Tag> tags = new HashMap<>();
	private final String name;
	
	/* Accessors */
	public String getName() { return name; }
	
	/* Constructors */
	private Tag(String name) {
		this.name = name;
	}
	
	public static Tag fromName(String name) {
		if(!tags.containsKey(name))
			tags.put(name, new Tag(name));
		
		return tags.get(name);
	}
}
