/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.omnetpp.ned.core.INedResources;
import org.omnetpp.ned.core.NedResourcesPlugin;

/**
 * Custom TextFileChange that also updates the NED model for files with
 * connected editors. This ensures editors pick up the changes immediately
 * via the model change notification mechanism.
 */
public class NedTextFileChange extends TextFileChange {
    private final String newSource;

    public NedTextFileChange(String name, IFile file, String newSource) {
        super(name, file);
        this.newSource = newSource;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
        // First, apply the text change (updates file on disk)
        Change undoChange = super.perform(pm);

        // Then, update the NED model if there's a connected editor
        // NedResources.resourceChanged() skips re-parsing files with connected
        // editors (the editor "owns" the NED tree). We must explicitly push the
        // new text into the NED model so that the tree gets updated and model
        // change events fire, causing editors to pull the new source.
        IFile file = (IFile) getModifiedElement();
        syncNedModel(file, newSource);

        // Wrap the undo change so that undoing also updates the NED model
        return new NedModelSyncChange(undoChange, file);
    }

    /**
     * Pushes the given source text into the NED model if the file has a
     * connected editor. This is necessary because NedResources.resourceChanged()
     * skips re-parsing files with connected editors.
     */
    private static void syncNedModel(IFile file, String source) {
        INedResources nedResources = NedResourcesPlugin.getNedResources();
        if (nedResources.hasConnectedEditor(file))
            nedResources.setNedFileText(file, source);
    }

    /**
     * Wrapping Change that delegates to an inner undo/redo Change and then
     * synchronizes the NED model with the file's current content.
     */
    private static class NedModelSyncChange extends Change {
        private final Change delegate;
        private final IFile file;

        NedModelSyncChange(Change delegate, IFile file) {
            this.delegate = delegate;
            this.file = file;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm) {
            delegate.initializeValidationData(pm);
        }

        @Override
        public Object getModifiedElement() {
            return delegate.getModifiedElement();
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
            return delegate.isValid(pm);
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException {
            Change redoChange = delegate.perform(pm);

            // After the undo/redo text change has been applied, read the file's
            // current content and push it into the NED model.
            String content = NedRefactoringUtils.readFileContent(file);
            if (content != null)
                syncNedModel(file, content);

            return new NedModelSyncChange(redoChange, file);
        }
    }
}
