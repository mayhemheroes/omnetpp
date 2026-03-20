/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.refactoring;

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
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;

/**
 * LTK-based refactoring for renaming a submodule. Performs tree-level mutations
 * via {@link RefactoringTools#renameSubmodule} and wraps the resulting file changes
 * as LTK {@link Change} objects, providing undo/redo and preview support.
 *
 * @author andras
 */
public class RenameSubmoduleRefactoring extends AbstractNedMemberRenameRefactoring {

    private final SubmoduleElementEx submodule;

    public RenameSubmoduleRefactoring(SubmoduleElementEx submodule) {
        super(submodule.getEnclosingTypeElement());
        this.submodule = submodule;
    }

    public SubmoduleElementEx getSubmodule() {
        return submodule;
    }

    /**
     * Returns the type info of the compound module containing the submodule.
     * Equivalent to {@link #getDeclaringTypeInfo()}.
     */
    public INedTypeInfo getCompoundTypeInfo() {
        return declaringTypeInfo;
    }

    @Override
    public String getName() {
        return "Rename Submodule";
    }

    @Override
    public String getOldName() {
        return submodule.getName();
    }

    @Override
    protected String getElementKindLabel() {
        return "submodule";
    }

    @Override
    protected void checkAdditionalFinalConditions(RefactoringStatus status) throws CoreException {
        String fqn = declaringTypeInfo.getFullyQualifiedName();
        if (declaringTypeInfo.getSubmodules().containsKey(newName))
            status.addFatalError("A submodule named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getParamDeclarations().containsKey(newName))
            status.addFatalError("A parameter named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getGateDeclarations().containsKey(newName))
            status.addFatalError("A gate named '" + newName + "' already exists in " + fqn + ".");
    }

    @Override
    protected Set<NedFileElementEx> performRename(String name) throws CoreException {
        return RefactoringTools.renameSubmodule(submodule, name);
    }

    @Override
    protected Map<IFile, List<NedRefactoringUtils.Match>> findIniMatches(IProject project, String oldName) throws CoreException {
        return NedRefactoringUtils.findSubmoduleInIniFiles(project, oldName, submodule);
    }

    @Override
    protected void addIniChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
        NedRefactoringUtils.addIniFileChanges(group, file, matches, oldName, newName);
    }

}
