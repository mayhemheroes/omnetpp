/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.refactoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.omnetpp.common.editor.text.TextDifferenceUtils;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;

/**
 * Abstract base class for LTK-based refactorings that rename a member element
 * (gate, parameter, or submodule) within a NED type. Subclasses supply the
 * element-specific operations via the abstract hook methods; all shared
 * scaffolding (snapshotting, undo, change building, condition checking) lives here.
 *
 * @author andras
 */
public abstract class AbstractNedMemberRenameRefactoring extends Refactoring {

    public static final String IDENTIFIER_REGEX = "[A-Za-z_][A-Za-z0-9_]*";

    protected final INedTypeElement declaringType;
    protected final INedTypeResolver resolver;
    protected final INedTypeInfo declaringTypeInfo;
    protected String newName;

    protected AbstractNedMemberRenameRefactoring(INedTypeElement declaringType) {
        this.declaringType = declaringType;
        this.resolver = declaringType.getResolver();
        this.declaringTypeInfo = declaringType.getNedTypeInfo();
    }

    // -- Abstract hook methods --------------------------------------------------

    /**
     * Returns the current (old) name of the element being renamed.
     */
    public abstract String getOldName();

    /**
     * Returns the human-readable element kind label used in messages,
     * e.g. {@code "gate"}, {@code "parameter"}, {@code "submodule"}.
     */
    protected abstract String getElementKindLabel();

    /**
     * Performs the rename on the live NED trees and returns the set of modified
     * file elements.
     */
    protected abstract Set<NedFileElementEx> performRename(String name) throws CoreException;

    /**
     * Searches for occurrences of the old name in INI files under the given project.
     */
    protected abstract Map<IFile, List<NedRefactoringUtils.Match>> findIniMatches(IProject project, String oldName) throws CoreException;

    /**
     * Adds the appropriate INI-file {@link Change} entries to {@code group} for the
     * given file and matches.
     */
    protected abstract void addIniChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException;

    /**
     * Searches for occurrences of the old name in C++ files under the given project.
     * Default implementation returns an empty map (no C++ search).
     */
    protected Map<IFile, List<NedRefactoringUtils.Match>> findCppMatches(IProject project, String oldName) throws CoreException {
        return new HashMap<>();
    }

    /**
     * Adds the appropriate C++-file {@link Change} entries to {@code group} for the
     * given file and matches. Default implementation does nothing.
     */
    protected void addCppChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
    }

    /**
     * Hook called by {@link #createChange} just before the rename is performed on the
     * live NED trees. Subclasses may override to snapshot tree-derived state (e.g.
     * qualified names) that will change once the trees are mutated.
     * Default implementation does nothing.
     */
    protected void preRename() {}

    /**
     * Hook for subclasses to add extra checks to {@link #checkFinalConditions}.
     * Called after the standard checks pass. Default implementation does nothing.
     */
    protected void checkAdditionalFinalConditions(RefactoringStatus status) throws CoreException {}

    /**
     * Returns the label for the composite change description.
     * Default: {@code "Rename <kind> '<old>' to '<new>'"}.
     */
    protected String getCompositeLabel(String oldName, String newName) {
        return "Rename " + getElementKindLabel() + " '" + oldName + "' to '" + newName + "'";
    }

    // -- Shared public API ------------------------------------------------------

    public INedTypeInfo getDeclaringTypeInfo() {
        return declaringTypeInfo;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    // -- LTK Refactoring implementation -----------------------------------------

    /**
     * Returns the current text content of the file as seen by LTK (i.e. the
     * editor document text if the file is open in an editor, disk content otherwise).
     * Returns null if the buffer is not connected.
     */
    private static String getDocumentText(IFile file) {
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        ITextFileBuffer buffer = bufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
        if (buffer != null)
            return buffer.getDocument().get();
        return null;
    }

    /**
     * Reads the full text content of the given file from disk.
     * Returns null if the file cannot be read.
     */
    private static String readFileContent(IFile file) {
        return NedRefactoringUtils.readFileContent(file);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        // check that the containing file has no syntax errors
        NedFileElementEx declarationFile = declaringType.getContainingNedFileElement();
        if (declarationFile.hasSyntaxError())
            status.addFatalError("Cannot rename: the file containing the declaration has syntax errors.");

        // check that it's not in a built-in type
        if (resolver.isBuiltInDeclaration(declaringTypeInfo))
            status.addFatalError("Cannot rename elements in built-in types.");

        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        String oldName = getOldName();

        if (newName == null || newName.isEmpty())
            status.addFatalError("Name must not be empty.");
        else if (newName.equals(oldName))
            status.addFatalError("Name is unchanged.");
        else if (!newName.matches(IDENTIFIER_REGEX))
            status.addFatalError("Invalid identifier.");
        else
            checkAdditionalFinalConditions(status);

        // warn about files with syntax errors that may not be fully updated
        for (IFile file : resolver.getNedFiles()) {
            if (pm.isCanceled())
                throw new OperationCanceledException();
            NedFileElementEx nedFileElement = resolver.getNedFileElement(file);
            if (nedFileElement.hasSyntaxError())
                status.addWarning("File '" + file.getFullPath() + "' has syntax errors and will not be updated.");
        }

        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        pm.beginTask("Computing rename changes", 7);

        String oldName = getOldName();
        IProject project = declaringTypeInfo.getProject();
        NedFileElementEx declarationFileElement = declaringType.getContainingNedFileElement();
        IFile declarationFile = resolver.getNedFile(declarationFileElement);

        // Step 1: snapshot old source for all NED files (only those that could be affected).
        // For files open in an editor, use the live document text (via ITextFileBufferManager)
        // so that oldSource matches exactly what LTK sees when validating the ReplaceEdit length.
        // For files without a connected editor, the on-disk content is authoritative.
        Map<IFile, String> oldSources = new HashMap<>();
        for (IFile file : resolver.getNedFiles()) {
            if (pm.isCanceled())
                throw new OperationCanceledException();
            NedFileElementEx nedFileElement = resolver.getNedFileElement(file);
            if (!nedFileElement.hasSyntaxError()) {
                String content;
                if (NedResourcesPlugin.getNedResources().hasConnectedEditor(file))
                    content = getDocumentText(file);
                else
                    content = readFileContent(file);
                if (content == null)
                    content = readFileContent(file);  // fallback if buffer not connected
                if (content != null)
                    oldSources.put(file, content);
            }
        }
        pm.worked(1);

        // Step 2: Snapshot tree-derived state and search for text matches in INI
        // files BEFORE renaming the NED trees, so that the NED model is still
        // consistent with the INI file content (semantic confirmation via
        // ParamCollector needs matching param names).
        preRename();
        Map<IFile, List<NedRefactoringUtils.Match>> iniMatches = findIniMatches(project, oldName);
        Map<IFile, List<NedRefactoringUtils.Match>> cppMatches = findCppMatches(project, oldName);
        pm.worked(1);

        if (pm.isCanceled())
            throw new OperationCanceledException();

        // Step 3: perform the rename on the NED trees
        try {
            NedResourcesPlugin.getNedResources().setRefactoringInProgress(true);
            NedResourcesPlugin.getNedResources().fireBeginChangeEvent();

            Set<NedFileElementEx> modifiedFileElements = performRename(newName);

            // Step 4: collect new sources for modified files
            Map<IFile, String> newSources = new HashMap<>();
            // the declaration file is always modified
            newSources.put(declarationFile, declarationFileElement.getNedSource());
            for (NedFileElementEx modifiedFileElement : modifiedFileElements) {
                IFile file = resolver.getNedFile(modifiedFileElement);
                if (file != null)
                    newSources.put(file, modifiedFileElement.getNedSource());
            }
            pm.worked(1);

            // Step 5: undo the rename on the trees so the model stays unchanged
            // (LTK will apply the changes itself via the Change objects)
            performRename(oldName);
            pm.worked(1);

            // Step 6: build Change objects from old→new source diffs
            CompositeChange composite = new CompositeChange(getCompositeLabel(oldName, newName));

            // Add NED file changes (from NED engine) as a group
            CompositeChange nedChangesGroup = new CompositeChange("NED file changes");
            for (Map.Entry<IFile, String> entry : newSources.entrySet()) {
                if (pm.isCanceled())
                    throw new OperationCanceledException();
                IFile file = entry.getKey();
                String oldSource = oldSources.get(file);
                if (oldSource == null)
                    continue;
                String newSource = TextDifferenceUtils.filterWhitespaceOnlyDiffs(oldSource, entry.getValue());

                if (!oldSource.equals(newSource)) {
                    NedTextFileChange textFileChange = new NedTextFileChange(file.getName(), file, newSource);
                    textFileChange.setEdit(new ReplaceEdit(0, oldSource.length(), newSource));
                    textFileChange.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
                    nedChangesGroup.add(textFileChange);
                }
            }
            if (nedChangesGroup.getChildren().length > 0)
                composite.add(nedChangesGroup);

            // Add INI file changes (from text search) as a separate group
            CompositeChange iniChangesGroup = new CompositeChange("Text matches in INI files");
            for (Map.Entry<IFile, List<NedRefactoringUtils.Match>> entry : iniMatches.entrySet()) {
                if (pm.isCanceled())
                    throw new OperationCanceledException();
                IFile file = entry.getKey();
                List<NedRefactoringUtils.Match> matches = entry.getValue();
                if (!matches.isEmpty())
                    addIniChanges(iniChangesGroup, file, matches, oldName, newName);
            }
            if (iniChangesGroup.getChildren().length > 0)
                composite.add(iniChangesGroup);

            // Add C++ file changes (from text search) as a separate group
            CompositeChange cppChangesGroup = new CompositeChange("Text matches in C++ files");
            for (Map.Entry<IFile, List<NedRefactoringUtils.Match>> entry : cppMatches.entrySet()) {
                if (pm.isCanceled())
                    throw new OperationCanceledException();
                IFile file = entry.getKey();
                List<NedRefactoringUtils.Match> matches = entry.getValue();
                if (!matches.isEmpty())
                    addCppChanges(cppChangesGroup, file, matches, oldName, newName);
            }
            if (cppChangesGroup.getChildren().length > 0)
                composite.add(cppChangesGroup);

            pm.worked(1);

            return composite;
        }
        finally {
            NedResourcesPlugin.getNedResources().fireEndChangeEvent();
            NedResourcesPlugin.getNedResources().setRefactoringInProgress(false);
            pm.done();
        }
    }

}
