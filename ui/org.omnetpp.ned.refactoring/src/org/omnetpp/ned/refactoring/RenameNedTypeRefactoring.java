/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.refactoring;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.omnetpp.ned.core.refactoring.RefactoringTools;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;

/**
 * LTK-based refactoring for renaming a NED type. Performs tree-level mutations
 * via {@link RefactoringTools#renameNedType} and wraps the resulting file changes
 * as LTK {@link Change} objects, providing undo/redo and preview support.
 *
 * @author andras
 */
public class RenameNedTypeRefactoring extends AbstractNedMemberRenameRefactoring {

    private String snapshotOldQName;

    public RenameNedTypeRefactoring(INedTypeElement typeElement) {
        super(typeElement);
    }

    public INedTypeElement getTypeElement() {
        return declaringType;
    }

    public INedTypeInfo getTypeInfo() {
        return declaringTypeInfo;
    }

    @Override
    public String getName() {
        return "Rename NED Type";
    }

    @Override
    public String getOldName() {
        return declaringTypeInfo.getName();
    }

    @Override
    protected String getElementKindLabel() {
        return "NED type";
    }

    @Override
    protected void preRename() {
        snapshotOldQName = declaringTypeInfo.getFullyQualifiedName();
    }

    @Override
    protected void checkAdditionalFinalConditions(RefactoringStatus status) throws CoreException {
        IProject project = declaringTypeInfo.getProject();
        String newQName = declaringTypeInfo.getNamePrefix() + newName;
        if (resolver.getToplevelNedType(newQName, project) != null)
            status.addFatalError("A type named '" + newQName + "' already exists.");

        // Warn if the new simple name collides with types in other packages,
        // which could cause ambiguity in files using wildcard imports.
        Collection<INedTypeInfo> sameSimpleName = resolver.getToplevelNedTypesBySimpleName(newName, project);
        for (INedTypeInfo other : sameSimpleName) {
            if (!other.getFullyQualifiedName().equals(newQName))
                status.addWarning("A type named '" + other.getFullyQualifiedName()
                        + "' also has the simple name '" + newName
                        + "'. This may cause ambiguity in files using wildcard imports.");
        }
    }

    @Override
    protected String getCompositeLabel(String oldName, String newName) {
        String newQName = declaringTypeInfo.getNamePrefix() + newName;
        return "Rename '" + snapshotOldQName + "' to '" + newQName + "'";
    }

    @Override
    protected Set<NedFileElementEx> performRename(String name) throws CoreException {
        return RefactoringTools.renameNedType(declaringType, name);
    }

    @Override
    protected Map<IFile, List<NedRefactoringUtils.Match>> findIniMatches(IProject project, String oldName) throws CoreException {
        return NedRefactoringUtils.findTypeInIniFiles(project, declaringTypeInfo.getName(), snapshotOldQName);
    }

    @Override
    protected void addIniChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
        String source = NedRefactoringUtils.readFileContent(file);
        if (source == null)
            return;
        String oldSimpleName = declaringTypeInfo.getName();
        String newQName = declaringTypeInfo.getNamePrefix() + this.newName;
        NedRefactoringUtils.addIniFileChangesForType(group, file, matches, source, oldSimpleName, snapshotOldQName, this.newName, newQName);
    }

}
