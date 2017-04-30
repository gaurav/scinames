/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ggvaidya.scinames.model.change;

import com.ggvaidya.scinames.model.Change;
import javafx.util.StringConverter;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeTypeStringConverter extends StringConverter<Change.Type> {
	public ChangeTypeStringConverter() {}

	@Override
	public String toString(Change.Type type) {
		return type.getType();
	}

	@Override
	public Change.Type fromString(String string) {
		return Change.Type.of(string);
	}
}
