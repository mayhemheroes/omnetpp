/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.ned.refactoring.AbstractNedMemberRenameRefactoring;
import org.omnetpp.ned.refactoring.RenameGateRefactoring;
import org.omnetpp.ned.refactoring.RenameNedTypeRefactoring;
import org.omnetpp.ned.refactoring.RenameParamRefactoring;
import org.omnetpp.ned.refactoring.RenameSubmoduleRefactoring;
import org.omnetpp.ned.editor.text.actions.RenameWizard;
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;

/**
 * GEF command that launches an LTK refactoring wizard for renaming a NED type,
 * submodule, gate, or parameter. The wizard is opened with the new name pre-filled
 * (as entered in the inline cell editor), providing the user with a preview of all
 * cross-file changes before confirming.
 *
 * @author andras
 */
public class RenameRefactoringCommand extends Command {

    private final AbstractNedMemberRenameRefactoring refactoring;
    private final String title;

    public RenameRefactoringCommand(INedTypeElement typeElement, String newName) {
        super("Rename " + typeElement.getName());
        RenameNedTypeRefactoring r = new RenameNedTypeRefactoring(typeElement);
        r.setNewName(newName);
        this.refactoring = r;
        this.title = "Rename NED Type";
    }

    public RenameRefactoringCommand(SubmoduleElementEx submodule, String newName) {
        super("Rename " + submodule.getName());
        RenameSubmoduleRefactoring r = new RenameSubmoduleRefactoring(submodule);
        r.setNewName(newName);
        this.refactoring = r;
        this.title = "Rename Submodule";
    }

    public RenameRefactoringCommand(GateElementEx gate, String newName) {
        super("Rename " + gate.getName());
        RenameGateRefactoring r = new RenameGateRefactoring(gate);
        r.setNewName(newName);
        this.refactoring = r;
        this.title = "Rename Gate";
    }

    public RenameRefactoringCommand(ParamElementEx param, String newName) {
        super("Rename " + param.getName());
        RenameParamRefactoring r = new RenameParamRefactoring(param);
        r.setNewName(newName);
        this.refactoring = r;
        this.title = "Rename Parameter";
    }

    @Override
    public void execute() {
        try {
            // Skip the input page — the new name was already entered in the inline cell editor.
            // Pass null as the input page so RenameWizard goes straight to the preview page.
            RenameWizard wizard = new RenameWizard(refactoring, title, null, true);
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
            op.run(Display.getCurrent().getActiveShell(), title);
        }
        catch (Exception e) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Rename", "Error during rename: " + e.getMessage());
        }
    }

    @Override
    public boolean canUndo() {
        return false; // LTK tracks undo via its own refactoring history
    }
}
