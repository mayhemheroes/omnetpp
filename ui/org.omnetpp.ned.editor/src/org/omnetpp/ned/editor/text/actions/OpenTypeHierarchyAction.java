/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.text.actions;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.omnetpp.common.ui.GenericTreeContentProvider;
import org.omnetpp.common.ui.GenericTreeLabelProvider;
import org.omnetpp.common.ui.GenericTreeNode;
import org.omnetpp.ned.core.IGotoNedElement;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.core.NedTypeHierarchyUtil;
import org.omnetpp.ned.core.NedTypeHierarchyUtil.HierarchyResult;
import org.omnetpp.ned.editor.text.TextualNedEditor;
import org.omnetpp.ned.editor.text.util.NedTextUtils;
import org.omnetpp.ned.editor.text.util.NedTextUtils.Info;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.ui.NedModelLabelProvider;

/**
 * Opens a popup showing the type hierarchy of the NED type at the cursor,
 * similar to Eclipse JDT's "Quick Type Hierarchy" (Ctrl+T).
 */
public class OpenTypeHierarchyAction extends NedTextEditorAction {
    public static final String ID = "OpenTypeHierarchy";

    public OpenTypeHierarchyAction(TextualNedEditor editor) {
        super(ID, editor);
    }

    @Override
    protected void doRun() {
        INedTypeInfo typeInfo = getTypeInfoAtCursor();
        if (typeInfo == null)
            return;

        Shell shell = getTextEditor().getSite().getShell();
        TypeHierarchyPopup popup = new TypeHierarchyPopup(shell, typeInfo);
        popup.open();
    }

    private INedTypeInfo getTypeInfoAtCursor() {
        ISourceViewer viewer = ((TextualNedEditor) getTextEditor()).getSourceViewerPublic();
        if (viewer == null)
            return null;
        IRegion region = new Region(viewer.getSelectedRange().x, viewer.getSelectedRange().y);
        Info info = NedTextUtils.getNedReferenceFromSource(getTextEditor(), viewer, region);
        if (info != null && info.referredElement instanceof INedTypeElement)
            return ((INedTypeElement) info.referredElement).getNedTypeInfo();
        return null;
    }

    /**
     * Popup dialog that displays the type hierarchy tree.
     */
    private static class TypeHierarchyPopup extends PopupDialog {
        private INedTypeInfo focusType;
        private TreeViewer treeViewer;

        TypeHierarchyPopup(Shell parent, INedTypeInfo focusType) {
            super(parent, SWT.RESIZE, true, true, true, false, false,
                    "Type Hierarchy - " + focusType.getName(), null);
            this.focusType = focusType;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(400, 300);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            treeViewer.setLabelProvider(new GenericTreeLabelProvider(NedModelLabelProvider.getInstance()));
            treeViewer.setContentProvider(new GenericTreeContentProvider());

            HierarchyResult result = NedTypeHierarchyUtil.buildInheritanceTree(focusType);
            treeViewer.setInput(result.rootNode);
            treeViewer.expandAll();
            treeViewer.setSelection(new StructuredSelection(result.focusNode), true);

            treeViewer.addDoubleClickListener(event -> navigateToSelection());

            treeViewer.getTree().addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.character == SWT.CR || e.character == SWT.LF) {
                        navigateToSelection();
                    }
                }
            });

            return treeViewer.getTree();
        }

        private void navigateToSelection() {
            Object sel = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
            if (sel instanceof GenericTreeNode) {
                Object payload = ((GenericTreeNode) sel).getPayload();
                if (payload instanceof INedElement) {
                    NedResourcesPlugin.openNedElementInEditor((INedElement) payload, IGotoNedElement.Mode.TEXT);
                    close();
                }
            }
        }
    }
}
