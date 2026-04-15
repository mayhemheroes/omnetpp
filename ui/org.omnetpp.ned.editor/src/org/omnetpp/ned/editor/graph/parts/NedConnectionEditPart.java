/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.figures.ConnectionFigure;
import org.omnetpp.figures.ConnectionKindFigure;
import org.omnetpp.figures.ITooltipTextProvider;
import org.omnetpp.figures.routers.ConnectionRoutingConstraint;
import org.omnetpp.ned.editor.graph.actions.ToggleConnectionBundlingAction;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.editor.NedEditor;
import org.omnetpp.ned.editor.graph.dialogs.PropertiesDialog;
import org.omnetpp.ned.editor.graph.parts.policies.NedConnectionEditPolicy;
import org.omnetpp.ned.editor.graph.parts.policies.NedConnectionEndpointEditPolicy;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.ConnectionElementEx;
import org.omnetpp.ned.model.interfaces.INedModelProvider;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.pojo.ConnectionGroupElement;


/**
 * Implements a Connection editpart to represent a wire-like connection between modules.
 *
 * @author rhornig
 */
public class NedConnectionEditPart extends AbstractConnectionEditPart
                    implements IReadOnlySupport, INedModelProvider {

    private EditPart sourceEditPartEx;
    private EditPart targetEditPartEx;
    private boolean editable = true;

    @Override
    public void activate() {
        if (isActive()) return;
        super.activate();
    }

    @Override
    public void deactivate() {
        if (!isActive()) return;
        super.deactivate();
    }

    @Override
    public void activateFigure() {
        ConnectionFigure cfig = getConnectionFigure();
        ConnectionElementEx connectionModel = getModel();

        // Get the connection layer and its router
        org.eclipse.draw2d.ConnectionLayer layer = (ConnectionLayer)((CompoundModuleEditPart)getParent()).getFigure().
             getSubmoduleArea().getConnectionLayer();
        org.eclipse.draw2d.ConnectionRouter router = layer.getConnectionRouter();

        // Set routing constraint - the router will store it
        ConnectionRoutingConstraint routingConstraint = new ConnectionRoutingConstraint();
        parseRoutingConstraint(connectionModel, routingConstraint);

        // compute connection bundling: group connections by (srcModule, destModule, srcDir)
        computeBundling(connectionModel, routingConstraint);

        // Set constraint in router BEFORE setting router on connection
        router.setConstraint(cfig, routingConstraint);

        // add the connection to the compound module's connection layer instead of the global one
        layer.add(getConnectionFigure());

        // Set the router AFTER adding to layer (triggers routing, which will read constraint from router)
        cfig.setConnectionRouter(router);
    }

    @Override
    public void deactivateFigure() {
        // Remove constraint from router
        ConnectionFigure cfig = getConnectionFigure();
        if (cfig.getConnectionRouter() != null)
            cfig.getConnectionRouter().remove(cfig);
        // remove the connection figure from the parent
        getFigure().getParent().remove(getFigure());
        cfig.setSourceAnchor(null);
        cfig.setTargetAnchor(null);
    }

    @Override
    public EditPart getSource() {
        return sourceEditPartEx;
    }

    @Override
    public EditPart getTarget() {
        return targetEditPartEx;
    }

    /**
     * Sets the source EditPart of this connection. Overrides the original implementation
     * to add the connection as the child of a compound module
     *
     * @param editPart  EditPart which is the source.
     */
    @Override
    public void setSource(EditPart editPart) {
        if (sourceEditPartEx == editPart)
            return;

        sourceEditPartEx = editPart;
        if (sourceEditPartEx != null) {
            // attach the connection edit part to the compound module as a parent
            if (sourceEditPartEx instanceof CompoundModuleEditPart)
                setParent(sourceEditPartEx);
            else if (sourceEditPartEx instanceof SubmoduleEditPart)
                setParent(sourceEditPartEx.getParent());
        }
        else if (targetEditPartEx == null)
            setParent(null);
        if (sourceEditPartEx != null && targetEditPartEx != null)
            refresh();
    }

    /**
     * Sets the target EditPart of this connection. Overrides the original implementation
     * to add the connection as the child of a compound module
     * @param editPart  EditPart which is the target.
     */
    @Override
    public void setTarget(EditPart editPart) {
        if (targetEditPartEx == editPart)
            return;
        targetEditPartEx = editPart;
        if (targetEditPartEx != null) {
            // attach the connection edit part to the compound module as a parent
            if (targetEditPartEx instanceof CompoundModuleEditPart)
                setParent(targetEditPartEx);
            else if (targetEditPartEx instanceof SubmoduleEditPart)
                setParent(targetEditPartEx.getParent());
        }
        else if (sourceEditPartEx == null)
            setParent(null);
        if (sourceEditPartEx != null && targetEditPartEx != null)
            refresh();
    }

    /**
     * Adds extra EditPolicies as required.
     */
    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new NedConnectionEndpointEditPolicy());
        installEditPolicy(EditPolicy.CONNECTION_ROLE, new NedConnectionEditPolicy());
    }

    /**
     * Creates and returns a figure to represent the connection.
     */
    @Override
    protected IFigure createFigure() {
        ConnectionFigure conn = new ConnectionFigure();
        return conn;
    }

    @Override
    public ConnectionFigure getConnectionFigure() {
        return (ConnectionFigure)super.getConnectionFigure();
    }

    /**
     * Refreshes the visual aspects of this, based upon the model (Wire). It
     * changes the wire color depending on the state of Wire.
     *
     */
    @Override
    protected void refreshVisuals() {
        ConnectionElementEx connectionModel = getModel();
        ConnectionFigure cfig = getConnectionFigure();

        cfig.setDisplayString(connectionModel.getDisplayString());
        cfig.setArrowHeadEnabled(!connectionModel.getIsBidirectional());

        // set routing constraint for the connection router (arrowcoords)
        ConnectionRoutingConstraint routingConstraint = new ConnectionRoutingConstraint();
        parseRoutingConstraint(connectionModel, routingConstraint);

        // compute connection bundling: group connections by (srcModule, destModule, srcDir)
        computeBundling(connectionModel, routingConstraint);

        cfig.setRoutingConstraint(routingConstraint);

        boolean isConditional = connectionModel.getFirstConditionChild() != null;
        boolean isGroup = connectionModel.getFirstLoopChild() != null;
        if (connectionModel.getParent() instanceof ConnectionGroupElement) {
            ConnectionGroupElement parent = (ConnectionGroupElement) connectionModel.getParent();
            if (!isConditional && parent.getFirstConditionChild() != null)
                isConditional = true;
            if (!isGroup && parent.getFirstLoopChild() != null)
                isGroup = true;
        }
        if (isConditional || isGroup)
            cfig.setMidpointDecoration(new ConnectionKindFigure(isConditional, isGroup));
        else
            cfig.setMidpointDecoration(null);

        // set the error marker on the figure
        ITooltipTextProvider textProvider = new ITooltipTextProvider() {
            public String getTooltipText(int x, int y) {
                String message = "";
                if (getModel().getMaxProblemSeverity() >= IMarker.SEVERITY_INFO) {
                    IMarker[] markers = NedResourcesPlugin.getNedResources().getMarkersForElement(getModel(), true, 11);
                    int i = 0;
                    for (IMarker marker : markers) {
                        message += marker.getAttribute(IMarker.MESSAGE , "")+"\n";
                        // we allow 10 markers maximum in a single message
                        if (++i > 10) {
                            message += "and some more...\n";
                            break;
                        }
                    }
                }
                return StringUtils.strip(message);
            }
        };

        getConnectionFigure().setProblemDecoration(getModel().getMaxProblemSeverity(), textProvider);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        boolean isEditable
            = editable && getParent().getModel() == getModel().getCompoundModule();
        if (!isEditable)
            return false;
        // otherwise check what about the parent. if parent is read only we should return its state
        if (getParent() instanceof IReadOnlySupport)
            return ((IReadOnlySupport)getParent()).isEditable();
        return true;
    }

    @Override
    public void performRequest(Request req) {
        super.performRequest(req);
        // let's open or activate a new editor if someone has double clicked the component
        if (RequestConstants.REQ_OPEN.equals(req.getType()) && isEditable()) {
            INedElement[] elements = new INedElement[] { getModel() };

            PropertiesDialog dialog = new PropertiesDialog(Display.getDefault().getActiveShell(), elements);
            if (dialog.open() != Dialog.OK)
                return; // canceled

            // get the command stack of the active editor and execute the command with it
            IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (activeEditor instanceof NedEditor) {
                Command command = dialog.getResultCommand();
                ((NedEditor)activeEditor).getGraphEditor().getEditDomain().getCommandStack().execute(command);
            }
        }
    }

    /**
     * Returns the compound module part this connection part belongs to
     */
    public CompoundModuleEditPart getCompoundModulePart() {
        return (CompoundModuleEditPart)getParent();
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.editparts.AbstractEditPart#registerModel()
     * Override the default behavior because each compound module has it's own registry for connection model
     * connection part mapping (instead of the default impl. one map for the whole viewer)
     * The connection part creation is looking also in this registry
     * @see org.omnetpp.ned.editor.graph.edit.ModuleEditPart#createOrFindConnection(java.lang.Object)
     */
    @Override
    protected void registerModel() {
        getCompoundModulePart().getModelToConnectionPartsRegistry().put(getModel(), this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.editparts.AbstractEditPart#unregisterModel()
     * Override the default behavior because each compound module has it's own registry for connection model
     * connection part mapping (instead of the default impl. one map for the whole viewer)
     * The connection part creation is looking also in this registry
     * @see org.omnetpp.ned.editor.graph.edit.ModuleEditPart#createOrFindConnection(java.lang.Object)
     */
    @Override
    protected void unregisterModel() {
        Map<Object,ConnectionEditPart> registry = getCompoundModulePart().getModelToConnectionPartsRegistry();
        if (registry.get(getModel()) == this)
            registry.remove(getModel());
    }

    public ConnectionElementEx getModel() {
        return (ConnectionElementEx)super.getModel();
    }

    /**
     * Computes the bundle index and size for connection arrow distribution.
     * Connections between the same module pair with the same routing mode are
     * grouped together. Bidirectional connections count as a single visual line.
     */
    private void computeBundling(ConnectionElementEx connectionModel, ConnectionRoutingConstraint constraint) {
        CompoundModuleEditPart compoundPart;
        try {
            compoundPart = getCompoundModulePart();
        } catch (Exception e) {
            return;
        }
        if (compoundPart == null || compoundPart.getModel() == null)
            return;

        // Skip bundling if disabled for this compound module type
        if (!compoundPart.isConnectionBundlingEnabled())
            return;

        String srcMod = connectionModel.getSrcModule();
        String destMod = connectionModel.getDestModule();
        if (srcMod == null || destMod == null)
            return;

        // canonical ordering: alphabetically smaller module name first
        String modA, modB;
        if (srcMod.compareTo(destMod) <= 0) {
            modA = srcMod;
            modB = destMod;
        } else {
            modA = destMod;
            modB = srcMod;
        }

        // get all connections between this module pair from the model
        // (connections in both directions)
        CompoundModuleElementEx compoundModel = compoundPart.getModel();
        List<ConnectionElementEx> abConns = compoundModel.getConnections(modA, null, modB, null);
        List<ConnectionElementEx> baConns = modA.equals(modB) ?
                java.util.Collections.emptyList() :
                compoundModel.getConnections(modB, null, modA, null);

        // Build ordered list of all connections between this module pair with
        // the same routing srcDir. Each connection is one visual line.
        // In NED, a <--> connection is a single model element (not two),
        // so no deduplication of reverse directions is needed.
        List<ConnectionElementEx> visualLines = new ArrayList<>();
        for (ConnectionElementEx conn : abConns)
            if (getSrcDir(conn) == constraint.srcDir)
                visualLines.add(conn);
        for (ConnectionElementEx conn : baConns)
            if (getSrcDir(conn) == constraint.srcDir)
                visualLines.add(conn);

        // find this connection's index
        int index = visualLines.indexOf(connectionModel);

        if (index >= 0 && visualLines.size() > 1) {
            constraint.bundleIndex = index;
            constraint.bundleSize = visualLines.size();
        }
    }

    private char getSrcDir(ConnectionElementEx conn) {
        String str = conn.getDisplayString().getAsString(IDisplayString.Prop.ROUTING_CONSTRAINT);
        if (str != null && str.length() > 0 && "newshv".indexOf(str.charAt(0)) >= 0)
            return str.charAt(0);
        return '\0';
    }

    private void parseRoutingConstraint(ConnectionElementEx connectionModel, ConnectionRoutingConstraint rc) {
        String arg0 = connectionModel.getDisplayString().getAsString(IDisplayString.Prop.ROUTING_CONSTRAINT);
        char ch = (arg0 != null && arg0.length() > 0) ? arg0.charAt(0) : '\0';
        if (ch == 'a') {
            // legacy 'a' (auto) = both unconstrained (default)
        }
        else if (ch == 'm') {
            // manual mode: m=m,srcAnchX,srcAnchY,destAnchX,destAnchY
            rc.srcDir = 'm';
            // m[1] is PropType.STRING (dual-purpose), so parse as string and convert
            String srcAnchXStr = connectionModel.getDisplayString().getAsString(IDisplayString.Prop.ROUTING_ANCHOR_SRCX);
            if (srcAnchXStr != null && !srcAnchXStr.isEmpty()) {
                try { rc.srcAnchX = Integer.parseInt(srcAnchXStr); } catch (NumberFormatException e) { }
            }
            rc.srcAnchY = connectionModel.getDisplayString().getAsInt(IDisplayString.Prop.ROUTING_ANCHOR_SRCY, 50);
            rc.destAnchX = connectionModel.getDisplayString().getAsInt(IDisplayString.Prop.ROUTING_ANCHOR_DESTX, 50);
            rc.destAnchY = connectionModel.getDisplayString().getAsInt(IDisplayString.Prop.ROUTING_ANCHOR_DESTY, 50);
        }
        else {
            if ("newshv".indexOf(ch) >= 0)
                rc.srcDir = ch;
            // arg 1 is dest direction (if present and a valid direction letter)
            String arg1 = connectionModel.getDisplayString().getAsString(IDisplayString.Prop.ROUTING_ANCHOR_SRCX);
            if (arg1 != null && arg1.length() > 0 && "newshv".indexOf(arg1.charAt(0)) >= 0)
                rc.destDir = arg1.charAt(0);
        }
    }

    public INedTypeElement getNedTypeElementToOpen() {
        INedTypeElement typeToOpen = getModel().getTypeOrLikeTypeRef();
        // detect built-in types (that are not defined in a file) and return null (they cannot be opened)
        if (typeToOpen != null && typeToOpen.getNedTypeInfo().getNedFile()==null)
            return null;

        return typeToOpen;  // open the effective type if pressed F3
    }
}


