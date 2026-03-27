package org.omnetpp.ned.editor.graph.properties.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.common.properties.TextCellEditorEx;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.figures.misc.TransformDescription;
import org.omnetpp.ned.core.NedCanvasFigureValidator;

public class TransformPropertyDescriptor extends PropertyDescriptor {

    String transformsToString(List<TransformDescription> transforms) {
        if (transforms.size() == 1 && transforms.get(0).getOperation().equals("matrix")) {
            return transforms.get(0).getTransform().toString(); // ((a b) (c d) (t1 t2)) form in this case
        } else { // list of operations otherwise
            List<String> strings = new ArrayList<String>();

            for (TransformDescription transform : transforms) {
                strings.add((transform == null) ? "" : transform.toString());
            }

            return StringUtils.join(strings, ", ");
        }
    }

    List<TransformDescription> stringToTransforms(String string) {
        List<TransformDescription> transforms = new ArrayList<TransformDescription>();

        for (String desc : string.split("\\)\\s*,")) {
            transforms.add(TransformDescription.parse(desc));
        }

        return transforms;
    }

    class TransformLabelProvider extends LabelProvider {
        @SuppressWarnings("unchecked")
        public String getText(Object element) {
            return transformsToString((List<TransformDescription>)element);
        }
    }

    public TransformPropertyDescriptor(Object id, String displayName) {
        super(id, displayName);
        setLabelProvider(new TransformLabelProvider());
        setValidator(new TransformValidator());
    }

    class TransformValidator implements ICellEditorValidator {
        @Override
        public String isValid(Object value) {
            if (value instanceof String) {
                if (((String) value).isEmpty()) {
                    return null;
                }

                List<String> transforms = Arrays.asList(((String)value).trim().split("(?<=\\))\\s*,"));
                List<String> errors = NedCanvasFigureValidator.checkTransform(transforms);

                return errors.isEmpty() ? null : StringUtils.join(errors, " ");
            } else {
                return null;
            }
        }

    }

    class TransformCellEditor extends TextCellEditorEx {
        public TransformCellEditor(Composite parent) {
            super(parent);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void doSetValue(Object value) {
            super.doSetValue(transformsToString((List<TransformDescription>)value));
        }

        @Override
        protected Object doGetValue() {
            return stringToTransforms((String)super.doGetValue());
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Object openDialogBox(Control cellEditorWindow) {
            List<TransformDescription> oldList = (List<TransformDescription>)this.getValue();
            List<TransformDescription> newList = new ArrayList<TransformDescription>(oldList.size());

            // making a deep copy to avoid direct modification of the source data
            for (TransformDescription transform : oldList) {
                if (transform != null) {
                    newList.add(new TransformDescription(transform));
                }

            }
/*  XXX
            TransformDialog dialog = new TransformDialog(cellEditorWindow.getShell(), newList);

            int result = dialog.open();

            return (result == Window.OK) ? newList : oldList;*/
            return oldList;
        }
    }

    public CellEditor createPropertyEditor(Composite parent) {
        CellEditor editor = new TransformCellEditor(parent);

        if (getValidator() != null) {
            editor.setValidator(getValidator());
        }
        return editor;
    }

}
