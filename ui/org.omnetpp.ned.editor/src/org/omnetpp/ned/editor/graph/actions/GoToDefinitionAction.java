/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;
import org.omnetpp.ned.core.IGotoNedElement;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.editor.graph.parts.CompoundModuleEditPart;
import org.omnetpp.ned.editor.graph.parts.NedConnectionEditPart;
import org.omnetpp.ned.editor.graph.parts.SubmoduleEditPart;
import org.omnetpp.ned.editor.graph.parts.canvas.AbstractCanvasFigureEditPart;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;

/**
 * Opens the defining compound module for an inherited submodule, connection,
 * or @figure, and selects it there.
 */
public class GoToDefinitionAction extends SelectionAction {

    public static final String ID = "org.omnetpp.ned.editor.graph.GoToDefinition";
    public static final String MENUNAME = "Go to &Definition";
    public static final String TOOLTIP = "Go to the base type where this inherited element is defined";

    public GoToDefinitionAction(IWorkbenchPart part) {
        super(part);
        setText(MENUNAME);
        setId(ID);
        setActionDefinitionId(ID);
        setToolTipText(TOOLTIP);
    }

    @Override
    protected boolean calculateEnabled() {
        return getInheritedElement() != null;
    }

    /**
     * Returns the model element if the selected edit part represents an
     * inherited (non-local) submodule, connection, or @figure; null otherwise.
     */
    private INedElement getInheritedElement() {
        int size = getSelectedObjects().size();
        if (size == 0)
            return null;

        Object primarySelection = getSelectedObjects().get(size - 1);

        if (primarySelection instanceof SubmoduleEditPart) {
            SubmoduleEditPart ep = (SubmoduleEditPart) primarySelection;
            CompoundModuleElementEx displayedModule = ep.getCompoundModulePart().getModel();
            CompoundModuleElementEx definingModule = ep.getModel().getCompoundModule();
            if (definingModule != displayedModule)
                return ep.getModel();
        }
        else if (primarySelection instanceof NedConnectionEditPart) {
            NedConnectionEditPart ep = (NedConnectionEditPart) primarySelection;
            CompoundModuleElementEx displayedModule = ep.getCompoundModulePart().getModel();
            CompoundModuleElementEx definingModule = ep.getModel().getCompoundModule();
            if (definingModule != displayedModule)
                return ep.getModel();
        }
        else if (primarySelection instanceof AbstractCanvasFigureEditPart) {
            AbstractCanvasFigureEditPart ep = (AbstractCanvasFigureEditPart) primarySelection;
            CompoundModuleEditPart compoundPart = ep.getCompoundModulePart();
            if (compoundPart != null && ep.getModel() != null) {
                INedTypeElement definingType = ep.getModel().getSelfOrEnclosingTypeElement();
                if (definingType != compoundPart.getModel())
                    return ep.getModel();
            }
        }

        return null;
    }

    /**
     * Returns a human-readable name of the type where the inherited element
     * is defined. Useful for tooltips and labels.
     */
    public static String getDefiningTypeName(INedElement element, CompoundModuleElementEx displayedModule) {
        INedTypeElement definingType = element.getSelfOrEnclosingTypeElement();
        if (definingType != null && definingType != displayedModule)
            return definingType.getName();
        return null;
    }

    @Override
    public void run() {
        INedElement element = getInheritedElement();
        if (element != null)
            NedResourcesPlugin.openNedElementInEditor(element, IGotoNedElement.Mode.GRAPHICAL);
    }
}
