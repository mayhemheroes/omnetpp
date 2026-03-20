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
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;

/**
 * LTK-based refactoring for renaming a gate. Performs tree-level mutations
 * via {@link RefactoringTools#renameGate} and wraps the resulting file changes
 * as LTK {@link Change} objects, providing undo/redo and preview support.
 *
 * @author andras
 */
public class RenameGateRefactoring extends AbstractNedMemberRenameRefactoring {

    private final GateElementEx gate;

    public RenameGateRefactoring(GateElementEx gate) {
        super(gate.getEnclosingTypeElement());
        this.gate = gate;
    }

    public GateElementEx getGate() {
        return gate;
    }

    @Override
    public String getName() {
        return "Rename Gate";
    }

    @Override
    public String getOldName() {
        return gate.getName();
    }

    @Override
    protected String getElementKindLabel() {
        return "gate";
    }

    @Override
    protected void checkAdditionalFinalConditions(RefactoringStatus status) throws CoreException {
        String fqn = declaringTypeInfo.getFullyQualifiedName();
        if (declaringTypeInfo.getGateDeclarations().containsKey(newName))
            status.addFatalError("A gate named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getParamDeclarations().containsKey(newName))
            status.addFatalError("A parameter named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getSubmodules().containsKey(newName))
            status.addFatalError("A submodule named '" + newName + "' already exists in " + fqn + ".");
    }

    @Override
    protected Set<NedFileElementEx> performRename(String name) throws CoreException {
        return RefactoringTools.renameGate(gate, name);
    }

    @Override
    protected Map<IFile, List<NedRefactoringUtils.Match>> findIniMatches(IProject project, String oldName) throws CoreException {
        return NedRefactoringUtils.findNameInIniFiles(project, oldName);
    }

    @Override
    protected void addIniChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
        NedRefactoringUtils.addIniFileChanges(group, file, matches, oldName, newName);
    }

}
