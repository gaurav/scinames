/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ggvaidya.scinames.model.change;

import com.ggvaidya.scinames.model.ChangeType;

import javafx.util.StringConverter;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeTypeStringConverter extends StringConverter<ChangeType> {
	public ChangeTypeStringConverter() {}

	@Override
	public String toString(ChangeType type) {
		return type.getType();
	}

	@Override
	public ChangeType fromString(String string) {
		return ChangeType.of(string);
	}
}
