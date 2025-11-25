/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.msg.editor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.omnetpp.msg.editor.MsgEditorPlugin;

/**
 * Action to switch from a MSG file to its corresponding generated _m.h header file.
 *
 * @author Andras
 */
public class SwitchToGeneratedHeaderAction extends MsgTextEditorAction {
    public static final String ID = "SwitchToGeneratedHeader";

    public SwitchToGeneratedHeaderAction(TextEditor editor) {
        super(ID, editor);
    }

    @Override
    public void doRun() {
        try {
            IEditorInput editorInput = getTextEditor().getEditorInput();
            if (!(editorInput instanceof IFileEditorInput)) {
                showErrorMessage("Cannot determine file location");
                return;
            }

            IFile msgFile = ((IFileEditorInput) editorInput).getFile();
            String msgFileName = msgFile.getName();

            // Check if this is indeed a .msg file
            if (!msgFileName.endsWith(".msg")) {
                showErrorMessage("This action can only be used on .msg files");
                return;
            }

            // Generate the corresponding _m.h filename
            String headerFileName = msgFileName.substring(0, msgFileName.length() - 4) + "_m.h";

            // Look for the _m.h file in the same directory
            IFile headerFile = msgFile.getParent().getFile(new Path(headerFileName));

            // Refresh the parent folder to make sure we see any recently generated files
            try {
                msgFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
            } catch (CoreException e) {
                // Ignore refresh errors, continue with the operation
            }

            if (!headerFile.exists()) {
                String message = "The corresponding header file '" + headerFileName + "' does not exist.\n\n" +
                        "Make sure you have compiled your project to generate the MSG header files.";
                MessageDialog.openInformation(getTextEditor().getSite().getShell(),
                        "Header File Not Found", message);
                return;
            }

            // Open the header file in the editor
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try {
                IDE.openEditor(page, headerFile);
            } catch (PartInitException e) {
                MsgEditorPlugin.logError("Error opening header file", e);
                showErrorMessage("Failed to open the header file: " + e.getMessage());
            }

        } catch (Exception e) {
            MsgEditorPlugin.logError("Error in SwitchToGeneratedHeaderAction", e);
            showErrorMessage("An error occurred: " + e.getMessage());
        }
    }

    private void showErrorMessage(String message) {
        MessageDialog.openError(getTextEditor().getSite().getShell(), "Switch to Header", message);
    }
}
