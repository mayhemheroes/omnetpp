package org.omnetpp.ned.editor.graph.dialogs;

import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.omnetpp.common.ui.TableLabelProvider;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.editor.graph.dialogs.PointListDialog.LocalTableNumberCellEditorValidator;

/**
 * WORK IN PROGRESS, DO NOT USE
 */
public class PathDialog extends Dialog {

    private List<Point> Path;

    // constants
    private static final String COLUMN_X = "X";
    private static final String COLUMN_Y = "Y";
    private static final String[] COLUMNS = new String[] {COLUMN_X, COLUMN_Y};

    private static final int BUTTON_ADD_ID = 500;
    private static final int BUTTON_REMOVE_ID = 501;

    private final String dialogTitle;

    // widgets
    private TableViewer listViewer;

    // sizing constants
    private final static int SIZING_SELECTION_WIDGET_HEIGHT = 200;
    private final static int SIZING_SELECTION_WIDGET_WIDTH = 100;

    private final class PathTableLabelProvider extends TableLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            Point point = (Point)element;

            switch (columnIndex) {
                case 0: return Converter.doubleToString(point.preciseX());
                case 1: return Converter.doubleToString(point.preciseY());
                default: throw new RuntimeException();
            }
        }
    }


    /**
     * Creates the dialog.
     */
    public PathDialog(Shell parentShell, List<Point> Path) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
        this.dialogTitle = "Edit Point List";
        this.Path = Path;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (dialogTitle != null)
            shell.setText(dialogTitle);
        shell.setMinimumSize(250, 300);
        shell.setSize(250, 300);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        // page group
        Composite dialogArea = (Composite)super.createDialogArea(parent);

        Composite composite = new Composite(dialogArea, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(1,false));

        // table group
        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        group.setLayout(new GridLayout(1, false));

        // table and buttons
        listViewer = createAndConfigureTable(group);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
        data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
        listViewer.getTable().setLayoutData(data);

        addEditButtons(group);

        Dialog.applyDialogFont(composite);
        return composite;
    }

    protected Label createLabel(Composite composite, String text) {
        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, false));
        label.setText(text);
        return label;
    }

    private void addEditButtons(Composite composite) {
        Composite buttonComposite = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

        // add button
        Button addButton = createButton(buttonComposite, BUTTON_ADD_ID, "Add", false);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point newPoint = new PrecisionPoint();
                int selectedLine = listViewer.getTable().getSelectionIndex();
                Path.add(selectedLine + 1, newPoint);
                listViewer.refresh();
                listViewer.getTable().setSelection(selectedLine + 1);
            }
        });

        // remove button
        final Button removeButton = createButton(buttonComposite, BUTTON_REMOVE_ID, "Remove", false);
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = -1;
                ISelection selection = listViewer.getSelection();
                StructuredSelection structuredSelection = (StructuredSelection)selection;

                for (Object element : structuredSelection.toList()) {
                    Point point = (Point)element;

                    if (index == -1) {
                        index = Path.indexOf(point);
                    }

                    Path.remove(point);
                }

                if (Path.size() != 0)
                    listViewer.setSelection(new StructuredSelection(index), true);
                else
                    listViewer.setSelection(null);

                //validateDialogContent();
                listViewer.refresh();
            }
        });
        listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                StructuredSelection structuredSelection = (StructuredSelection)selection;

                removeButton.setEnabled(structuredSelection.size() != 0);
            }
        });
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private TableColumn addTableColumn(final Table table, String label, int width) {
        final TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(label);

        if (width != -1)
            column.setWidth(width);
        else {
            table.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int totalWidth = 0;

                    for (TableColumn otherColumn : table.getColumns())
                        if (!column.equals(otherColumn))
                            totalWidth += otherColumn.getWidth();

                    column.setWidth(table.getSize().x - totalWidth - 2 * table.getBorderWidth() - 20); // image size
                }
            });
        }

        return column;
    }

    private TableViewer createAndConfigureTable(Composite parent) {
        Table table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        addTableColumn(table, "X", 100);
        addTableColumn(table, "Y", 100);

        // set up tableViewer, content and label providers
        final TableViewer tableViewer = new TableViewer(table);
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setLabelProvider(new PathTableLabelProvider());

        // edit support
        tableViewer.setCellEditors(new CellEditor[] {
                new LocalTableNumberCellEditor((Composite)tableViewer.getControl()),
                new LocalTableNumberCellEditor((Composite)tableViewer.getControl())
        });
        tableViewer.setColumnProperties(COLUMNS);

        tableViewer.setCellModifier(new ICellModifier() {
            public boolean canModify(Object element, String property) {
                return true;
            }

            public Object getValue(Object element, String property) {
                Point point = (Point)element;

                if (COLUMN_X.equals(property)) {
                    return point.preciseX();
                } else if (COLUMN_Y.equals(property)) {
                    return point.preciseY();
                } else {
                    throw new RuntimeException();
                }
            }

            public void modify(Object element, String property, Object value) {
                if (element instanceof Item)
                    element = ((Item)element).getData(); // workaround, see super's comment

                if (value == null) {
                    return;
                }

                Point point = (Point)element;

                if (COLUMN_X.equals(property)) {
                    ((PrecisionPoint)point).setPreciseX((Double)value);
                } else if (COLUMN_Y.equals(property)) {
                    ((PrecisionPoint)point).setPreciseY((Double)value);
                } else {
                    throw new RuntimeException();
                }

                tableViewer.refresh();
            }
        });

        tableViewer.setInput(Path);
        return tableViewer;
    }

    class LocalTableNumberCellEditor extends TextCellEditor
    {
        public LocalTableNumberCellEditor(Composite parent) {
            super(parent);
            setValidator(new LocalTableNumberCellEditorValidator());
        }

        @Override
        protected Object doGetValue() {
            return Converter.stringToDouble((String)super.doGetValue());
        }

        @Override
        protected void doSetValue(Object value) {
            super.doSetValue(StringUtils.defaultString(Converter.doubleToString((Double)value)));
        }
    }

    class LocalTableNumberCellEditorValidator implements ICellEditorValidator
    {
        public String isValid(Object value) {
            if (value instanceof Double)
                return null;

            if (value != null && !(value instanceof String))
                return "Unexpected type: " + value.getClass().getName();

            String strValue = (String)value;
            if (StringUtils.isEmpty(strValue))
                return null;

            try {
                Double.parseDouble(strValue);
                return null;
            } catch (NumberFormatException e) {
                return "Not a number";
            }
        }
    }
}
