package com.ggvaidya.scinames.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * Type of change. This is pretty disconnected from the rest of Change, i.e. an ChangeType.ADDITION
 * doesn't *have* to have only "to" names. But the validator looks for cases where you break the
 * assumptions here.
 */
public final class ChangeType implements Comparable<ChangeType> {
	/** Stores singletons of ChangeTypes */
	private static Map<String, ChangeType> singletons = new HashMap<>();
	
	/* Recognized ChangeTypes to be used in SciNames. */
	public static final ChangeType ADDITION = of("added");
	public static final ChangeType DELETION = of("deleted");
	public static final ChangeType RENAME = of("rename");
	public static final ChangeType LUMP = of("lump");
	public static final ChangeType SPLIT = of("split");
	public static final ChangeType COMPLEX = of("complex");
	public static final ChangeType ERROR = of("error");
	public static final List<ChangeType> RECOGNIZED_TYPES = Arrays.asList(
		ADDITION,
		DELETION,
		RENAME,
		LUMP,
		SPLIT,
		COMPLEX,
		ERROR
	);
	
	/**
	 * Return the singleton corresponding to a particular name of a change type.
	 * 
	 * @param text String representation of a change type.
	 * @return ChangeType corresponding to the string representation.
	 */
	public static ChangeType of(String text) {
		text = text.toLowerCase();
		
		if(!singletons.containsKey(text))
			singletons.put(text, new ChangeType(text));
			
		return singletons.get(text);
	}
	
	/* Non-static object begins here. */
	/** Type of this ChangeType. */
	private String type;

	/**
	 * @return The "inversion" of this type: additions become deletions,
	 * splits become lumps and so on.
	 */
	public ChangeType invert() {
		if(equals(ADDITION))		return DELETION;
		else if(equals(DELETION))	return ADDITION;
		else if(equals(RENAME))		return RENAME;
		else if(equals(LUMP))		return SPLIT;
		else if(equals(SPLIT))		return LUMP;
		else if(equals(COMPLEX))	return COMPLEX;
		else if(equals(ERROR))		return ERROR;
		else throw new RuntimeException("Unable to invert Change.Type: " + this);
	}

	/** Return the string representation of this ChangeType. */
	public String getType() { return type; }		

	/** Return the string representation of this ChangeType. */	
	@Override public String toString() { return type; }
	
	/** Construct a ChangeType. This is private -- you should use ChangeType.of("changeType")! */
	private ChangeType(String s) { type = s; }

	/** How do you sort ChangeTypes? Alphabetically. */
	@Override
	public int compareTo(ChangeType ty) {
		return type.compareToIgnoreCase(ty.type);
	}
}