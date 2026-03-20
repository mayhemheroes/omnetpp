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
import org.omnetpp.ned.model.ex.ParamElementEx;

/**
 * LTK-based refactoring for renaming a parameter. Performs tree-level mutations
 * via {@link RefactoringTools#renameParam} and wraps the resulting file changes
 * as LTK {@link Change} objects, providing undo/redo and preview support.
 *
 * INI file matches are searched for the parameter name appearing as the last
 * dotted segment of a config key (e.g. {@code **.typename.paramName = value}).
 * Matches in key position are enabled by default; others are shown but disabled.
 *
 * @author andras
 */
public class RenameParamRefactoring extends AbstractNedMemberRenameRefactoring {

    private final ParamElementEx param;

    public RenameParamRefactoring(ParamElementEx param) {
        super(param.getEnclosingTypeElement());
        this.param = param;
    }

    public ParamElementEx getParam() {
        return param;
    }

    @Override
    public String getName() {
        return "Rename Parameter";
    }

    @Override
    public String getOldName() {
        return param.getName();
    }

    @Override
    protected String getElementKindLabel() {
        return "parameter";
    }

    @Override
    protected void checkAdditionalFinalConditions(RefactoringStatus status) throws CoreException {
        String fqn = declaringTypeInfo.getFullyQualifiedName();
        if (declaringTypeInfo.getParamDeclarations().containsKey(newName))
            status.addFatalError("A parameter named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getGateDeclarations().containsKey(newName))
            status.addFatalError("A gate named '" + newName + "' already exists in " + fqn + ".");
        else if (declaringTypeInfo.getSubmodules().containsKey(newName))
            status.addFatalError("A submodule named '" + newName + "' already exists in " + fqn + ".");
    }

    @Override
    protected Set<NedFileElementEx> performRename(String name) throws CoreException {
        return RefactoringTools.renameParam(param, name);
    }

    @Override
    protected Map<IFile, List<NedRefactoringUtils.Match>> findIniMatches(IProject project, String oldName) throws CoreException {
        return NedRefactoringUtils.findParamInIniFiles(project, oldName, param);
    }

    @Override
    protected void addIniChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
        NedRefactoringUtils.addIniFileChanges(group, file, matches, oldName, newName);
    }

    @Override
    protected Map<IFile, List<NedRefactoringUtils.Match>> findCppMatches(IProject project, String oldName) throws CoreException {
        return NedRefactoringUtils.findParamInCppFiles(project, oldName);
    }

    @Override
    protected void addCppChanges(CompositeChange group, IFile file,
            List<NedRefactoringUtils.Match> matches, String oldName, String newName) throws CoreException {
        NedRefactoringUtils.addTextFileChanges(group, file, matches, oldName, newName);
    }

}
