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

/**
 * Generic input page for NED rename refactoring wizards. Works with any
 * {@link AbstractNedMemberRenameRefactoring} subclass; the caller supplies
 * the page title, description, and initial name to pre-fill the field with.
 *
 * @author andras
 */
public class RenameInputPage extends UserInputWizardPage {

    private final AbstractNedMemberRenameRefactoring refactoring;
    private final String initialName;
    private Text nameField;

    public RenameInputPage(String pageId, String title, String description,
            AbstractNedMemberRenameRefactoring refactoring, String initialName) {
        super(pageId);
        this.refactoring = refactoring;
        this.initialName = initialName;
        setTitle(title);
        setDescription(description);
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
        nameField.setText(refactoring.getNewName() != null ? refactoring.getNewName() : initialName);
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

        String oldName = refactoring.getOldName();

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
            setErrorMessage(null);
            setPageComplete(true);
        }
    }
}
