/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.omnetpp.ned.editor.NedEditorPlugin;
import org.omnetpp.ned.editor.graph.parts.CompoundModuleEditPart;

/**
 * Toggle Connection Bundling action.
 *
 * @author andras
 */
public class ToggleConnectionBundlingAction extends CompoundModuleAction {

    public static final String ID = "ToggleConnectionBundling";
    public static final String MENUNAME = "Distribute Connection Arrows";
    public static final String TOOLTIP = "Distribute Connection Arrows";
    public static final ImageDescriptor IMAGE = NedEditorPlugin.getImageDescriptor("icons/full/etool16/connectionbundle.png");

    public ToggleConnectionBundlingAction(IWorkbenchPart part) {
        super(part);
        setText(MENUNAME);
        setId(ID);
        setToolTipText(TOOLTIP);
        setImageDescriptor(IMAGE);
        setActionDefinitionId("org.omnetpp.ned.editor.graph.ToggleConnectionBundling");
        setAccelerator(SWT.CTRL | 'T');
        setChecked(getSelectionCompoundModule() != null && getSelectionCompoundModule().isConnectionBundlingEnabled());
    }

    @Override
    public void run() {
        CompoundModuleEditPart compoundModule = getSelectionCompoundModule();
        boolean newState = !compoundModule.isConnectionBundlingEnabled();
        compoundModule.setConnectionBundlingEnabled(newState);
        setChecked(newState);
        compoundModule.refresh();
    }

}
