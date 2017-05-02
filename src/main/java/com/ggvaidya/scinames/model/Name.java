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

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A scientific name.
 * 
 * These are implemented as singletons: the same full name should be represented by the
 * same Name object. This should simplify renames eventually.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Name implements Comparable<Name> {
	private static final Logger LOGGER = Logger.getLogger(Name.class.getSimpleName());
	
	/** Denotes an empty name. */
	public static final Name EMPTY = new Name();
	
	/** What string is used to separate name components? */
	public static final String SEPARATOR = " ";
        
    /** 
     * What do we call a name with a genus and potentially infraspecific
     * names, but no specific epithet? Think of genus names as either
     * 'Alpha' or 'Alpha ' + GENUS_SP.
     */
    public static final String GENUS_SP = "sp";
    
    /* Regular expressions */
    public static final Pattern PATTERN_SPECIFICEPITHET = Pattern.compile("^[a-z\\-]+$");
	
    /* 
     * Internal variables 
     * 
     * Note that 'genus' is guaranteed to be set, but all of the other variables may be null.
     */
	private String genus;
	private String specificEpithet;
	private List<InfraspecificEpithet> infraspecificEpithets = new LinkedList<>();
	
	/**
	 * InfraspecificEpithet wraps up the idea that infraspecific epithets can be
	 * identifiers (e.g. 'tigris') or structural (e.g. "var."). Where possible,
	 * we'd like to pair that information up (e.g. "var.": "tigris"), which
	 * InfraspecificEpithet allows us to do. However it's represented, we should
	 * be able to unroll it back into a string (e.g. "var. tigris"). InfraspecificEpithet
	 * allows us to do all this.
	 */
	public static class InfraspecificEpithet {
		private String name = null;
		private String value = "";		
		
		public Optional<String> getName() { return Optional.ofNullable(name); }
		public String getValue() { return value; }
		
		public InfraspecificEpithet(String val)			{ value = val; }
		public InfraspecificEpithet(String n, String v) { name = n; value = v; }
		public String getCombination() {
			if(name == null) return value;
			else return name + Name.SEPARATOR + value;
		}
	}
	
	/**
	 * Replace the infraspecific epithets with the string of names provided.
	 * This destroys the current infraspecific epithets, which is why it's
	 * private. 
	 * 
	 * @param otherEpithets The infraspecific epithets to add.
	 */
	private void setInfraspecificEpithets(String... otherEpithets) {
		infraspecificEpithets.clear();
		if(otherEpithets.length > 0) {		
			int x;
			for(x = 0; x < otherEpithets.length; x += 2) {
				if(x + 1 == otherEpithets.length) continue;
				
				String name = otherEpithets[x];
				String value = otherEpithets[x + 1];
				infraspecificEpithets.add(new InfraspecificEpithet(name, value));
			}

			if(x > otherEpithets.length) {
				// This will only happen if we 'jump' over the ending.
				infraspecificEpithets.add(new InfraspecificEpithet(otherEpithets[otherEpithets.length - 1]));
			}
		}
	}
	
	public void setInfraspecificEpithetsFromString(String str) {
		setInfraspecificEpithets(str.split("\\s+"));
	}	
	
	private static Set<String> specificEpithetsThatArentLowercase = new HashSet(Arrays.asList(
		"sp",
		"spp",
		"sp.",
		"spp.",
		"af",
		"af.",
		"aff",
		"aff.",
		"cf",
		"cf."
	));
	
	/* 
	 * Name constructors. These should not be used outside this object: instead,
	 * use Name.getFromFullName() or Name.get().
	 */
	
	private Name() {
		// Create an empty name.
		this.genus = "(empty name)";
	}

	private Name(String genus) {
		this.genus = genus;
	}
	
	private Name(String genus, String specificEpithet) {
		this.genus = genus;
		
		// Is putative specific epithet lowercase and alphanumeric?
		if(PATTERN_SPECIFICEPITHET.matcher(specificEpithet).matches())
		    this.specificEpithet = specificEpithet;
		else
			setInfraspecificEpithetsFromString(specificEpithet);
	}
	
	/*
	 * We provide singletons for provided names. This is how we do it.
	 */
	
	private static Map<String, Name> namesByFullName = new HashMap<>();
	private static Map<String, Name> namesByBinomial = new HashMap<>(); // TODO delete
	
	public static Name get(String genus, String specificEpithet, String subspecificEpithets) {
        genus = genus.trim();
        specificEpithet = specificEpithet.trim();
        subspecificEpithets = subspecificEpithets.trim();
		String fullName = genus + SEPARATOR + specificEpithet + SEPARATOR + subspecificEpithets;
		
		if(namesByFullName.containsKey(fullName))
			return namesByFullName.get(fullName);
		
		// Now hang on. Is that a REAL specific epithet?
		Name name;
		if(!PATTERN_SPECIFICEPITHET.matcher(specificEpithet).matches() || specificEpithetsThatArentLowercase.contains(specificEpithet.toLowerCase())) {
			// Oops, it's actually a genus name.
			name = new Name(genus);
			subspecificEpithets = specificEpithet + SEPARATOR + subspecificEpithets;
		} else {
			name = new Name(genus, specificEpithet);
			namesByBinomial.put(name.getBinomialName(), name);			
		}
		
		name.setInfraspecificEpithets(subspecificEpithets);
		namesByFullName.put(fullName, name);
		
		return name;
	}

	public static Name get(String genus, String specificEpithet) {
        genus = genus.trim();
        specificEpithet = specificEpithet.trim();
            
		String fullName = genus + SEPARATOR + specificEpithet;
		
		if(namesByFullName.containsKey(fullName))
			return namesByFullName.get(fullName);
		
		Name name;
		if(!PATTERN_SPECIFICEPITHET.matcher(specificEpithet).matches() || specificEpithetsThatArentLowercase.contains(specificEpithet.toLowerCase())) {
			// Oops, it's actually a genus name.
			name = new Name(genus);
			name.setInfraspecificEpithets(specificEpithet);
		} else {
			name = new Name(genus, specificEpithet);
			namesByBinomial.put(name.getBinomialName(), name);
		}

		namesByFullName.put(fullName, name);
		return name;
	}
	
	public static Name get(String genus) {
		String fullName = genus.trim();		
		if(fullName.equals("")) return EMPTY;
		
		if(namesByFullName.containsKey(fullName))
			return namesByFullName.get(fullName);
		
		Name name = new Name(genus);
		namesByFullName.put(fullName, name);
		
		return name;
	}
	
	/**
	 * Attempts to parse the full name in the provided string. Returns
	 * Optional.empty() if the name couldn't be parsed.
	 * 
	 * @param name Name to parse
	 * @return Name object resulting from the parse.
	 */
	public static Optional<Name> getFromFullName(String name) {
        name = name.trim();
        
		if(name == null || name.equals("")) return Optional.empty();
		String[] components = name.split("\\s+");
		
		if(components.length > 3) {
			String infraspecificEpithets = Arrays.asList(components)
				.subList(2, components.length)						// Ignore the first two components, which are
																	// the genus and specificEpithet.
				.stream().collect(Collectors.joining(SEPARATOR)); 	// Join with 'SEPARATOR'
                        
			return Optional.ofNullable(Name.get(components[0], components[1], infraspecificEpithets));
			
		} else if(components.length == 3) {
			return Optional.ofNullable(Name.get(components[0], components[1], components[2]));
			
		} else if(components.length == 2) {
			return Optional.ofNullable(Name.get(components[0], components[1]));
			
		} else if(components.length == 1) {
			return Optional.ofNullable(Name.get(components[0]));
			
		} else {
			LOGGER.warning("Name '" + name + "' could not be parsed into a Name.");
			return Optional.empty();
			
		}
	}
	
	/**
	 * Returns a binomial name for this Name. It's probably cleaner to use
	 * asBinomial().
	 * 
	 * @return The binomial name if it exists, otherwise 'null'.
	 */
	public String getBinomialName() {
		if(specificEpithet == null)
			return null;
		else
			return genus + SEPARATOR + specificEpithet;
	}
	
	/**
	 * Attempt to convert this Name to a binomial name. If impossible,
	 * this will return Stream.empty(). It is designed to be used as
	 * 	names.stream().flatMap(n -> n.asBinomial())
	 * 
	 * @return Optionally return a Binomial name.
	 */
	public Stream<Name> asBinomial() {
		if(genus == null || specificEpithet == null)
			return Stream.empty();
		
		// We definitely have a binomial name. But do we have more?
		if(hasSubspecificEpithet())
			return Stream.of(Name.get(genus, specificEpithet));
					
		// No more, so we're already binomial? Fine, this name will do.
		return Stream.of(this);
	}
	
	public boolean hasSpecificEpithet() {
		return (genus != null && specificEpithet != null);
	}	
	
	public String getInfraspecificEpithetsAsString() {
		return infraspecificEpithets.stream().map(ep -> ep.getCombination()).collect(Collectors.joining(SEPARATOR));
	}
	
	public String getFullName() {
		if(specificEpithet == null) {
			// Genus name only.
			
			if(infraspecificEpithets.isEmpty())
				return genus;
			else
				return genus + SEPARATOR + GENUS_SP + SEPARATOR + getInfraspecificEpithetsAsString();
		}
		
		// Genus and specific epithet, but no infraspecific epithets.
		if(infraspecificEpithets.isEmpty())
			return genus + SEPARATOR + specificEpithet;
		
		// Genus, specific epithet and infraspecific epithets.
		return genus + SEPARATOR + specificEpithet + SEPARATOR + getInfraspecificEpithetsAsString();
	}
	
	public String getComparableName() {
		return getFullName().toLowerCase();
	}
	
	public String getGenus() {
		return genus;
	}
	
	public String getSpecificEpithet() {
		return specificEpithet;
	}
	
	public List<InfraspecificEpithet> getInfraspecificEpithets() {
		return infraspecificEpithets;
	}
	
	public boolean hasSubgenericEpithet() {
		return (genus != null && (specificEpithet != null || !infraspecificEpithets.isEmpty()));
	}
	
	public boolean hasSubspecificEpithet() {
		return (genus != null && specificEpithet != null && !infraspecificEpithets.isEmpty());
	}
	
	@Override
	public String toString() {
		return getFullName();
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 71 * hash + Objects.hashCode(getFullName());
		return hash;
	}

	public int compareTo(Name n) {
		if(n == null || n == Name.EMPTY) return -1;
		
		int c = this.getComparableName().compareTo(n.getComparableName());
		if(c != 0) return c;
		
		return this.getFullName().compareTo(n.getFullName());
	}
	
	public Element serializeToElement(Document doc) {
		Element nameElement = doc.createElement("name");
		
		nameElement.setAttribute("genus", getGenus());
		if(getSpecificEpithet() != null)
			nameElement.setAttribute("specificEpithet", getSpecificEpithet());
		
		if(infraspecificEpithets.isEmpty()) {
			nameElement.setAttribute("infraspecificEpithets", getInfraspecificEpithetsAsString());
		}
			
		nameElement.setTextContent(getFullName());

		return nameElement;
	}
	
	public static Name serializeFromNode(Node nameNode) throws SAXException {
		throw new UnsupportedOperationException("Serializing Name to Node is no longer supported");
		/*
		
		if(!nameNode.getNodeName().equals("name"))
			throw new SAXException("Name.serializeFromNode called with a non-Name node: " + nameNode);
		
		NamedNodeMap attr = nameNode.getAttributes();
		String genus = attr.getNamedItem("genus").getNodeValue();
		Node specificEpithetNode = attr.getNamedItem("specificEpithet");
		String specificEpithet = null;
		if(specificEpithetNode != null)
			specificEpithet = specificEpithetNode.getNodeValue();
		
		// TODO add infraspecificEpithetType
		
		Node infraspecificEpithetNode = attr.getNamedItem("infraspecificEpithet");
		String infraspecificEpithet = null;
		if(infraspecificEpithetNode != null)
			infraspecificEpithet = infraspecificEpithetNode.getNodeValue();

		Name name;
		if(infraspecificEpithet != null)
			name = Name.get(genus, specificEpithet, infraspecificEpithet);
		else if(specificEpithet != null)
			name = Name.get(genus, specificEpithet);
		else
			name = Name.get(genus);
		
		return name;
		*/
	}
}
