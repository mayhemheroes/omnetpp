/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.text.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.ned.refactoring.AbstractNedMemberRenameRefactoring;
import org.omnetpp.ned.refactoring.RenameGateRefactoring;
import org.omnetpp.ned.refactoring.RenameNedTypeRefactoring;
import org.omnetpp.ned.refactoring.RenameParamRefactoring;
import org.omnetpp.ned.refactoring.RenameSubmoduleRefactoring;
import org.omnetpp.ned.editor.text.TextualNedEditor;
import org.omnetpp.ned.editor.text.util.NedTextUtils;
import org.omnetpp.ned.editor.text.util.NedTextUtils.Info;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;

/**
 * Rename refactoring action for the textual NED editor. Renames the NED element
 * (type, submodule, gate, or parameter) under the cursor and updates all references
 * across all NED files and INI files.
 * Uses the Eclipse LTK (Language Toolkit) refactoring framework.
 *
 * @author andras
 */
public class RenameAction extends NedTextEditorAction {
    public static final String ID = "Rename";

    public RenameAction(TextualNedEditor editor) {
        super(ID, editor);
    }

    @Override
    protected void doRun() {
        TextualNedEditor textEditor = (TextualNedEditor) getTextEditor();

        // resolve the symbol under the cursor
        INedElement element = getRenamableElementAtCursor();
        if (element == null) {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
                    "Rename", "Please position the cursor on a renameable NED element (type, submodule, gate, or parameter name).");
            return;
        }

        AbstractNedMemberRenameRefactoring refactoring = null;
        UserInputWizardPage inputPage = null;
        String title = null;

        if (element instanceof INedTypeElement) {
            INedTypeElement typeElement = (INedTypeElement) element;
            if (typeElement.getNedTypeInfo() == null)
                return;
            RenameNedTypeRefactoring r = new RenameNedTypeRefactoring(typeElement);
            inputPage = new RenameNedTypeInputPage(r);
            title = "Rename NED Type";
            refactoring = r;
        }
        else if (element instanceof SubmoduleElementEx) {
            RenameSubmoduleRefactoring r = new RenameSubmoduleRefactoring((SubmoduleElementEx) element);
            inputPage = new RenameInputPage("RenameSubmoduleInputPage", "Rename Submodule",
                    "Enter the new name for submodule '" + r.getOldName() + "' in " + r.getDeclaringTypeInfo().getFullyQualifiedName() + ".",
                    r, r.getOldName());
            title = "Rename Submodule";
            refactoring = r;
        }
        else if (element instanceof GateElementEx) {
            RenameGateRefactoring r = new RenameGateRefactoring((GateElementEx) element);
            inputPage = new RenameInputPage("RenameGateInputPage", "Rename Gate",
                    "Enter the new name for gate '" + r.getOldName() + "' in " + r.getDeclaringTypeInfo().getFullyQualifiedName() + ".",
                    r, r.getOldName());
            title = "Rename Gate";
            refactoring = r;
        }
        else if (element instanceof ParamElementEx) {
            RenameParamRefactoring r = new RenameParamRefactoring((ParamElementEx) element);
            inputPage = new RenameInputPage("RenameParamInputPage", "Rename Parameter",
                    "Enter the new name for parameter '" + r.getOldName() + "' in " + r.getDeclaringTypeInfo().getFullyQualifiedName() + ".",
                    r, r.getOldName());
            title = "Rename Parameter";
            refactoring = r;
        }

        if (refactoring == null)
            return;

        // push current text editor changes into the NED model before refactoring
        textEditor.pushChangesIntoNedResources();

        try {
            RenameWizard wizard = new RenameWizard(refactoring, title, inputPage);
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
            op.run(Display.getCurrent().getActiveShell(), title);
        }
        catch (Exception e) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Rename", "Error during rename: " + e.getMessage());
        }

        // pull changes back into the text editor (LTK will have updated files on disk)
        textEditor.pullChangesFromNedResources();
    }

    /**
     * Resolves the renameable NED element at the current cursor position.
     * Returns an INedTypeElement, SubmoduleElementEx, GateElementEx, or ParamElementEx,
     * or null if the cursor is not on a renameable element.
     */
    private INedElement getRenamableElementAtCursor() {
        ISourceViewer viewer = ((TextualNedEditor) getTextEditor()).getSourceViewerPublic();
        if (viewer == null)
            return null;

        IRegion region = new Region(viewer.getSelectedRange().x, viewer.getSelectedRange().y);
        Info info = NedTextUtils.getNedReferenceFromSource(getTextEditor(), viewer, region);
        if (info == null || info.referredElement == null)
            return null;

        INedElement referred = info.referredElement;

        // if it's a type element, return it directly
        if (referred instanceof INedTypeElement)
            return referred;

        // if it's a submodule, return it
        if (referred instanceof SubmoduleElementEx)
            return referred;

        // if it's a gate declaration, return it
        if (referred instanceof GateElementEx)
            return referred;

        // if it's a parameter declaration, return it
        if (referred instanceof ParamElementEx)
            return referred;

        // otherwise, return null (not renameable)
        return null;
    }
}
