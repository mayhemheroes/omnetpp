/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.StringUtils;

public class NumberPropertyDescriptor extends TextPropertyDescriptor {

    double minValue;
    double maxValue;

    public NumberPropertyDescriptor(Object id, String displayName) {
        this(id, displayName, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public NumberPropertyDescriptor(Object id, String displayName, double min, double max) {
        super(id, displayName);
        minValue = min;
        maxValue = max;
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new NumberCellEditor(parent, minValue, maxValue);
    }
}

class NumberCellEditor extends TextCellEditor
{
    public NumberCellEditor(Composite parent, double min, double max) {
        super(parent);
        setValidator(new NumberCellEditorValidator(min, max));
    }

    @Override
    protected Object doGetValue() {
        return Converter.tolerantStringToOptionalDouble((String)super.doGetValue());
    }

    @Override
    protected void doSetValue(Object value) {
        super.doSetValue(StringUtils.defaultString(Converter.doubleToString((Double)value)));
    }
}

class NumberCellEditorValidator implements ICellEditorValidator
{
    double minValue = -Double.MAX_VALUE;
    double maxValue = Double.MAX_VALUE;

    public NumberCellEditorValidator(double min, double max) {
        minValue = min;
        maxValue = max;
    }

    public String isValid(Object value) {
        if (value instanceof Double)
            return null;

        if (value != null && !(value instanceof String))
            return "Unexpected type: " + value.getClass().getName();

        String strValue = (String)value;
        if (StringUtils.isEmpty(strValue))
            return null;

        double doubleValue;
        try {
            doubleValue = Double.parseDouble(strValue);
        } catch (NumberFormatException e) {
            return "Not a number";
        }

        if ((doubleValue < minValue) || (doubleValue > maxValue)) {
            return "The value " + doubleValue + " is out of range. Must be between " + minValue + " and " + maxValue + ".";
        }

        return null;
    }
}
