/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.text.actions;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

/**
 * Generic LTK refactoring wizard for NED rename operations.
 * Accepts any refactoring and an optional input page; when {@code skipInputPage}
 * is true (e.g. when the new name was already entered in an inline cell editor)
 * the wizard goes straight to the preview page.
 *
 * @author andras
 */
public class RenameWizard extends RefactoringWizard {

    private final UserInputWizardPage inputPage;
    private final boolean skipInputPage;

    public RenameWizard(Refactoring refactoring, String title, UserInputWizardPage inputPage) {
        this(refactoring, title, inputPage, false);
    }

    public RenameWizard(Refactoring refactoring, String title, UserInputWizardPage inputPage, boolean skipInputPage) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE);
        this.inputPage = inputPage;
        this.skipInputPage = skipInputPage;
        setDefaultPageTitle(title);
    }

    @Override
    protected void addUserInputPages() {
        if (!skipInputPage)
            addPage(inputPage);
    }
}
