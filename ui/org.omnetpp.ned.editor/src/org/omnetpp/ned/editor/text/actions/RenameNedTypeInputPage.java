/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.text.actions;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.omnetpp.ned.refactoring.AbstractNedMemberRenameRefactoring;
import org.omnetpp.ned.refactoring.RenameNedTypeRefactoring;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;

/**
 * Input page for the Rename NED Type refactoring wizard.
 * Provides a text field for entering the new type name with
 * real-time validation.
 *
 * @author andras
 */
public class RenameNedTypeInputPage extends UserInputWizardPage {

    private final RenameNedTypeRefactoring refactoring;
    private Text nameField;

    public RenameNedTypeInputPage(RenameNedTypeRefactoring refactoring) {
        super("RenameNedTypeInputPage");
        this.refactoring = refactoring;
        setTitle("Rename NED Type");
        setDescription("Enter the new name for '" + refactoring.getTypeInfo().getFullyQualifiedName() + "'.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(composite, SWT.NONE);
        label.setText("New name:");

        nameField = new Text(composite, SWT.BORDER);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        nameField.setText(refactoring.getNewName() != null ? refactoring.getNewName() : refactoring.getTypeInfo().getName());
        nameField.selectAll();

        nameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

        setControl(composite);
        nameField.setFocus();
        validateInput();
    }

    private void validateInput() {
        String newName = nameField.getText().trim();
        refactoring.setNewName(newName);

        INedTypeInfo typeInfo = refactoring.getTypeInfo();
        String oldName = typeInfo.getName();

        if (newName.isEmpty()) {
            setErrorMessage("Name must not be empty.");
            setPageComplete(false);
        }
        else if (newName.equals(oldName)) {
            setErrorMessage("Name is unchanged.");
            setPageComplete(false);
        }
        else if (!newName.matches(AbstractNedMemberRenameRefactoring.IDENTIFIER_REGEX)) {
            setErrorMessage("Invalid identifier.");
            setPageComplete(false);
        }
        else {
            // check for name collision
            String newQName = typeInfo.getNamePrefix() + newName;
            if (typeInfo.getResolver().getToplevelNedType(newQName, typeInfo.getProject()) != null) {
                setErrorMessage("A type named '" + newQName + "' already exists.");
                setPageComplete(false);
            }
            else {
                setErrorMessage(null);
                setPageComplete(true);
            }
        }
    }
}
