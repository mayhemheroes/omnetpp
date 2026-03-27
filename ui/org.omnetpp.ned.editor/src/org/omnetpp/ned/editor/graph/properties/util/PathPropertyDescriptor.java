package org.omnetpp.ned.editor.graph.properties.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.common.properties.TextCellEditorEx;
import org.omnetpp.common.util.Converter;
import org.omnetpp.ned.editor.graph.dialogs.PathDialog;

/**
 * WORK IN PROGRESS, DO NOT USE
 */
public class PathPropertyDescriptor extends PropertyDescriptor {

    private static String pathToString(List<Point> path) {
        StringBuilder builder = new StringBuilder();
        for (Point point : path) {
            builder.append(Converter.doubleToString(point.preciseX()));
            builder.append(",");
            builder.append(Converter.doubleToString(point.preciseY()));
            builder.append(", ");
        }

        String text = builder.toString();
        if (text.endsWith(", ")) {
            text = text.substring(0, text.length() - 2);
        }

        return text;
    }

    private static List<Point> stringToPath(String string) {
        List<String> coords = Arrays.asList(string.split(","));

        List<Point> points = new ArrayList<Point>(coords.size() / 2);

        try {
            for (int i = 0; i < (coords.size() - 1); i += 2) {
                points.add(new PrecisionPoint(Double.parseDouble(coords.get(i)), Double.parseDouble(coords.get(i + 1))));
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return points.isEmpty() ? null : points;
    }


    class PathLabelProvider extends LabelProvider {
        @SuppressWarnings("unchecked")
        public String getText(Object element) {
            return pathToString((List<Point>)element);
        }
    }

    public PathPropertyDescriptor(Object id, String displayName) {
        super(id, displayName);
        setLabelProvider(new PathLabelProvider());
        setValidator(new PathValidator());
    }

    class PathValidator implements ICellEditorValidator {

        @Override
        public String isValid(Object value) {
            if (value instanceof List<?>) {
                return null;
            } else {
                List<Point> list = stringToPath((String)value);

                return (list == null) ? "Invalid point list." : ( (list.size() < 2) ? "There must be at least 2 points in the list" : null );

            }
        }

    }

    class PathCellEditor extends TextCellEditorEx {
        public PathCellEditor(Composite parent) {
            super(parent);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void doSetValue(Object value) {
            super.doSetValue(pathToString((List<Point>)value));
        }

        @Override
        protected Object doGetValue() {
            return stringToPath((String)super.doGetValue());
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Object openDialogBox(Control cellEditorWindow) {
            List<Point> oldList = (List<Point>)this.getValue();
            List<Point> newList = new ArrayList<Point>(oldList.size());

            // making a deep copy to avoid direct modification of the source data
            for (Point point : oldList) {
                newList.add(new PrecisionPoint(point.preciseX(), point.preciseY()));
            }

            PathDialog dialog = new PathDialog(cellEditorWindow.getShell(), newList);

            int result = dialog.open();

            return (result == Window.OK) ? newList : oldList;
        }
    }

    public CellEditor createPropertyEditor(Composite parent) {
        CellEditor editor = new PathCellEditor(parent);

        if (getValidator() != null) {
            editor.setValidator(getValidator());
        }
        return editor;
    }

}
