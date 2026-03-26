/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.ex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.pojo.LiteralElement;
import org.omnetpp.ned.model.pojo.PropertyElement;
import org.omnetpp.ned.model.pojo.PropertyKeyElement;

/**
 * Extended property element.
 * @author rhornig, andras
 */
public class PropertyElementEx extends PropertyElement implements IHasName {
    public final static String DEFAULT_PROPERTY_INDEX = "";

    public PropertyElementEx() {
        super();
    }

    public PropertyElementEx(INedElement parent) {
        super(parent);
    }

    /**
     * Returns the first value from the default key (named "") if exists;
     * otherwise returns null.
     */
    public String getSimpleValue() {
        return getValue(DEFAULT_PROPERTY_INDEX);
    }

    /**
     * Return the first value from the specified key's value list, or null.
     */
    public String getValue(String key) {
        for (INedElement child : this)
            if (child instanceof PropertyKeyElement && key.equals(((PropertyKeyElement)child).getName()))
                for (INedElement grandChild : child)
                    if (grandChild instanceof LiteralElement)
                        return ((LiteralElement)grandChild).getValue();
        return null;
    }

    /**
     * Return the default key's value list, or null if there is no such key.
     */
    public List<String> getValueAsList() {
        return getValueAsList(DEFAULT_PROPERTY_INDEX);
    }

    /**
     * Return the specified key's value list, or null if there is no such key.
     */
    public List<String> getValueAsList(String key) {
        for (INedElement child : this) {
            if (child instanceof PropertyKeyElement && key.equals(((PropertyKeyElement)child).getName())) {
                List<String> result = new ArrayList<>();
                for (INedElement grandChild : child)
                    if (grandChild instanceof LiteralElement)
                        result.add(((LiteralElement)grandChild).getValue());
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the PropertyKeyElement child with the given name, or null.
     */
    public PropertyKeyElement getKeyElement(String name) {
        for (INedElement child : this) {
            if (child instanceof PropertyKeyElement && name.equals(((PropertyKeyElement)child).getName())) {
                return (PropertyKeyElement)child;
            }
        }

        return null;
    }

    /**
     * Removes the PropertyKeyElement child with the given key, if it exists.
     */
    public void removeKey(String key) {
        PropertyKeyElement element = getKeyElement(key);

        if (element != null) {
            removeChild(element);
        }
    }

    /**
     * Sets a single value for the given key.
     */
    public void setValue(String key, String value) {
        setValues(key, Arrays.asList(value));
    }

    /**
     * Sets the value at the given index for the given key, padding with
     * nulls if necessary. Removes the key if all values become empty.
     */
    public void setValue(String key, int index, String value) {
        List<String> values = getValueAsList(key);
        if (values == null)
            values = new ArrayList<>();

        // padding with nulls if there aren't enough values
        while (values.size() <= index) {
            values.add(null);
        }

        values.set(index, value);

        boolean hasContent = false;

        for (String v : values) {
            if ((v != null) && !(v.trim().isEmpty())) {
                hasContent = true;
                break;
            }
        }

        if (hasContent) {
            setValues(key, values);
        } else {
            removeKey(key);
        }
    }

    /**
     * Sets the value list for the given key. If the list is null or empty,
     * removes the key.
     */
    public void setValues(String key, List<String> values) {
        if ((values == null) || (values.isEmpty())) {
            removeKey(key);
        } else {
            PropertyKeyElement keyElement = getKeyElement(key);

            // if no KeyElement with this name exists yet, creating a new one
            if (keyElement == null) {
                keyElement = (PropertyKeyElement)NedElementFactoryEx.getInstance().createElement(NED_PROPERTY_KEY);
                keyElement.setName(key);
                appendChild(keyElement);
            } else { // if it already exists, clearing its values
                keyElement.removeAllChildren();
            }

            // adding the values as literal children
            for (String value : values) {
                LiteralElement literal = (LiteralElement)NedElementFactoryEx.getInstance().createElement(NED_LITERAL);
                literal.setType(NED_CONST_STRING);

                literal.setValue(StringUtils.nullToEmpty(value));
                literal.setText(StringUtils.sanitizePropertyValue(value));

                keyElement.appendChild(literal);
            }
        }
    }
}
