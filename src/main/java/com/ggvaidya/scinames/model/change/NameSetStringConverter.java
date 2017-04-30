/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ggvaidya.scinames.model.change;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Name;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.util.StringConverter;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameSetStringConverter extends StringConverter<ObservableSet<Name>> {
	public NameSetStringConverter() {}

	@Override
	public String toString(ObservableSet<Name> names) {
		if(names.isEmpty())
			return "";
		
		if(names.size() == 1) {
			Name name = (Name) (names.toArray()[0]);
			return name.getFullName();
		}
		
		List<String> fullNames = names.stream().map(n -> "\"" + n.getFullName() + "\"").sorted().collect(Collectors.toList());
		return String.join(" and ", fullNames);
	}

	@Override
	public ObservableSet<Name> fromString(String string) {
		return FXCollections.observableSet(Change.convertAndStringToNames(string).collect(Collectors.toSet()));
	}
}
