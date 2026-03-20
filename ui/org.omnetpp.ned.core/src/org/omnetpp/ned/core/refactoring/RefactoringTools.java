/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.core.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.ConnectionElementEx;
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.NedElementUtilEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.IChannelKindTypeElement;
import org.omnetpp.ned.model.interfaces.IInterfaceTypeElement;
import org.omnetpp.ned.model.interfaces.IModuleKindTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeLookupContext;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;
import org.omnetpp.ned.model.interfaces.ISubmoduleOrConnection;
import org.omnetpp.ned.model.pojo.ConditionElement;
import org.omnetpp.ned.model.pojo.ConnectionElement;
import org.omnetpp.ned.model.pojo.ConnectionGroupElement;
import org.omnetpp.ned.model.pojo.ConnectionsElement;
import org.omnetpp.ned.model.pojo.ExtendsElement;
import org.omnetpp.ned.model.pojo.GateElement;
import org.omnetpp.ned.model.pojo.GatesElement;
import org.omnetpp.ned.model.pojo.ImportElement;
import org.omnetpp.ned.model.pojo.InterfaceNameElement;
import org.omnetpp.ned.model.pojo.LoopElement;
import org.omnetpp.ned.model.pojo.NedElementTags;
import org.omnetpp.ned.model.pojo.ParametersElement;
import org.omnetpp.ned.model.pojo.SubmoduleElement;
import org.omnetpp.ned.model.pojo.SubmodulesElement;
import org.omnetpp.ned.model.pojo.TypesElement;

/**
 * Implementation of various NED refactoring operations.
 *
 * @author andras
 */
public class RefactoringTools {

    /**
     * Replaces package declaration with the expected package.
     */
    public static void fixupPackageDeclaration(NedFileElementEx nedFileElement) {
        if (nedFileElement.hasSyntaxError())
            return;

        INedTypeResolver resolver = nedFileElement.getResolver();
        IFile file = resolver.getNedFile(nedFileElement);

        String expectedPackage = resolver.getExpectedPackageFor(file);

        if (expectedPackage != null && !StringUtils.equals(nedFileElement.getPackage(), expectedPackage))
            nedFileElement.setPackage(expectedPackage);
    }

    /**
     * Organizes imports in the file. If there's an ambiguity, a selection
     * dialog is presented to the user.
     */
    //XXX factor out UI part (dialog), and pass it in as lambda?
    public static void organizeImports(final NedFileElementEx nedFileElement) {
        if (nedFileElement.hasSyntaxError())
            return;

        final INedTypeResolver resolver = nedFileElement.getResolver();
        IFile file = resolver.getNedFile(nedFileElement);
        final IProject contextProject = file.getProject();

        // resolve all imports
        final List<String> oldImports = nedFileElement.getImports();
        final HashSet<String> imports = new HashSet<String>();
        NedElementUtilEx.visitNedTree(nedFileElement, new NedElementUtilEx.INedElementVisitor() {
            public void visit(INedElement element) {
                if (element instanceof ISubmoduleOrConnection)
                    collect(element.getEnclosingLookupContext(), ((ISubmoduleOrConnection)element).getTypeOrLikeType());
                else if (element instanceof ExtendsElement)
                    collect(element.getEnclosingLookupContext(), ((ExtendsElement)element).getName());
                else if (element instanceof InterfaceNameElement)
                    collect(element.getEnclosingLookupContext(), ((InterfaceNameElement)element).getName());
            }

            private void collect(INedTypeLookupContext lookupContext, String typeName) {
                if (typeName != null && !typeName.contains("."))
                    resolveImport(lookupContext, contextProject, typeName, nedFileElement.getQNameAsPrefix(), oldImports, imports);
            }
        });

        // update model if imports have changed
        List<String> newImports = org.omnetpp.common.util.CollectionUtils.toSorted(imports);
        Collections.sort(oldImports);
        if (!newImports.equals(oldImports)) {
            nedFileElement.removeImports();
            for (String importSpec : newImports)
                nedFileElement.addImport(importSpec);
        }
    }

    /**
     * Find the fully qualified type for the given simple name, and add it to the imports list.
     */
    protected static void resolveImport(INedTypeLookupContext lookupContext, IProject contextProject, final String unqualifiedTypeName, String packagePrefix, List<String> oldImports, HashSet<String> imports) {
        INedTypeResolver resolver = lookupContext.getResolver();
        // name is in the same package as this file, no need to add an import
        if (resolver.getToplevelNedType(packagePrefix + unqualifiedTypeName, contextProject) != null)
            return;

        List<String> potentialMatches = new ArrayList<String>();
        // find local types
        potentialMatches.addAll(resolver.getLocalTypeNames(lookupContext, new INedTypeResolver.IPredicate() {
            public boolean matches(INedTypeInfo typeInfo) {
                return typeInfo.getFullyQualifiedName().endsWith("." + unqualifiedTypeName);
            }
        }));
        // local types silently win over toplevel types, and we don't need to import any of them
        if (potentialMatches.size() == 1)
            return;
        else
            potentialMatches.clear();

        // find all potential types
        for (String qualifiedName : resolver.getToplevelNedTypeQNames(contextProject))
            if (qualifiedName.endsWith("." + unqualifiedTypeName) || qualifiedName.equals(unqualifiedTypeName))
                potentialMatches.add(qualifiedName);

        // if there's zero or one match, we're done
        if (potentialMatches.size() == 0)
            return; // not found, sorry
        if (potentialMatches.size() == 1) {
            imports.add(potentialMatches.get(0));
            return;
        }

        // if there's an import for one of them already, use that
        for (String potentialMatch : potentialMatches) {
            if (oldImports.contains(potentialMatch)) {
                imports.add(potentialMatch);
                return;
            }
            // oldImports may contain wildcards, so try with regex as well
            for (String oldImport : oldImports)
                if (oldImport.contains("*"))
                    if (potentialMatch.matches(NedElementUtilEx.importToRegex(oldImport))) {
                        imports.add(potentialMatch);
                        return;
                    }
        }

        // ambiguous import: let the user choose
        String selectedType = chooseImport(potentialMatches);
        if (selectedType != null)
            imports.add(selectedType);
    }

    /**
     * Dialog to prompt a user to choose one from the listed imports.
     */
    //XXX move it out of this class
    protected static String chooseImport(List<String> importsList) {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        Shell shell = activeWorkbenchWindow == null ? null :activeWorkbenchWindow.getShell();
        ListDialog dialog = new ListDialog(shell);
        dialog.setInput(importsList);
        dialog.setContentProvider(new ArrayContentProvider());
        dialog.setLabelProvider(new LabelProvider());
        dialog.setTitle("Select Import");
        dialog.setMessage("Choose type to import:");
        if (dialog.open() == Window.OK)
            return (String) dialog.getResult()[0];
        return null;
    }

    /**
     * This method removes unnecessary elements from the NED tree, such as empty
     * channel spec, empty "parameters", "gates", "submodule" etc elements.
     */
    public static void cleanupTree(INedElement element) {
        // filter the child nodes first
        for (INedElement child : element)
            cleanupTree(child);

        // check for empty types, parameters, gates, submodules, connections node
        if ((element instanceof TypesElement
                || element instanceof ParametersElement
                || element instanceof GatesElement
                || element instanceof SubmodulesElement
                || element instanceof ConnectionsElement && !((ConnectionsElement)element).getAllowUnconnected())
                                && !element.hasChildren()) {
            element.removeFromParent();
        }
    }

    /**
     * Renames a toplevel NED type and updates all references across all NED files
     * in the project. The type declaration, extends/like references, submodule and
     * connection type references, and import statements are all updated.
     * Uses the NED infrastructure's resolved type info for identity checks
     * to avoid false matches when different types share the same simple name.
     *
     * @param typeElement the NED type element to rename
     * @param newSimpleName the new simple (unqualified) name
     * @return the set of NedFileElementEx objects that were modified (excluding the file containing the declaration)
     */
    public static Set<NedFileElementEx> renameNedType(INedTypeElement typeElement, String newSimpleName) {
        INedTypeInfo typeInfo = typeElement.getNedTypeInfo();
        INedTypeResolver resolver = typeElement.getResolver();
        String oldQName = typeInfo.getFullyQualifiedName();
        String namePrefix = typeInfo.getNamePrefix();
        String newQName = namePrefix + newSimpleName;

        NedFileElementEx declarationFile = typeElement.getContainingNedFileElement();
        Set<NedFileElementEx> modifiedFiles = new HashSet<>();

        // IMPORTANT: update references BEFORE renaming the declaration, because
        // the resolver's lookup cache is invalidated on every model change, and
        // resolved type references (getSuperType(), getNedTypeInfo(), etc.) would
        // no longer find the old type after the declaration is renamed.

        // update extends/like/submodule/connection references using resolved type identity
        for (INedTypeInfo ti : resolver.getToplevelNedTypes(typeInfo.getProject())) {
            boolean modified = updateTypeReferences(ti, typeElement, newSimpleName, newQName);
            if (modified) {
                NedFileElementEx file = ti.getNedElement().getContainingNedFileElement();
                if (file != declarationFile)
                    modifiedFiles.add(file);
            }
        }

        // update import statements (imports are always fully qualified, so FQN string match is correct)
        for (IFile file : resolver.getNedFiles()) {
            NedFileElementEx nedFileElement = resolver.getNedFileElement(file);
            if (nedFileElement.hasSyntaxError())
                continue;
            if (updateImportReferences(nedFileElement, oldQName, newQName) && nedFileElement != declarationFile)
                modifiedFiles.add(nedFileElement);
        }

        // rename the declaration itself last
        typeElement.setName(newSimpleName);

        return modifiedFiles;
    }

    /**
     * Updates extends, interface, submodule, and connection type references within a single
     * NED type, using resolved type identity to avoid false matches.
     * Returns true if any modifications were made.
     */
    private static boolean updateTypeReferences(INedTypeInfo typeInfo, INedTypeElement targetType, String newSimpleName, String newQName) {
        boolean modified = false;
        INedTypeElement typeElement = typeInfo.getNedElement();
        INedTypeResolver resolver = typeElement.getResolver();
        boolean sameKindFamily = (targetType instanceof IModuleKindTypeElement) == (typeElement instanceof IModuleKindTypeElement);

        // check extends reference (modules can only extend modules, channels can only extend channels)
        if (sameKindFamily && typeInfo.getSuperType() == targetType) {
            ExtendsElement ext = (ExtendsElement) typeElement.getFirstChildWithTag(NedElementTags.NED_EXTENDS);
            if (ext != null) {
                ext.setName(ext.getName().contains(".") ? newQName : newSimpleName);
                modified = true;
            }
        }

        // check interface (like) references and interface-extends references
        // (only interface types appear in getLocalInterfaces(), and only within the same kind family)
        if (targetType instanceof IInterfaceTypeElement && sameKindFamily && typeInfo.getLocalInterfaces().contains(targetType)) {
            INedTypeLookupContext lookupContext = typeElement.getParentLookupContext();
            for (INedElement child : typeElement) {

                if (child instanceof InterfaceNameElement) {
                    // modules/channels implement interfaces via "like" clause
                    InterfaceNameElement iface = (InterfaceNameElement) child;
                    INedTypeInfo resolved = resolver.lookupNedType(iface.getName(), lookupContext);
                    if (resolved != null && resolved.getNedElement() == targetType) {
                        iface.setName(iface.getName().contains(".") ? newQName : newSimpleName);
                        modified = true;
                    }
                }
                else if (child instanceof ExtendsElement && typeInfo.getSuperType() != targetType) {
                    // interface types extend other interfaces; but skip if already handled by the extends check above
                    ExtendsElement ext = (ExtendsElement) child;
                    INedTypeInfo resolved = resolver.lookupNedType(ext.getName(), lookupContext);
                    if (resolved != null && resolved.getNedElement() == targetType) {
                        ext.setName(ext.getName().contains(".") ? newQName : newSimpleName);
                        modified = true;
                    }
                }
            }
        }

        // check submodule and connection type references
        if (typeElement instanceof CompoundModuleElementEx) {
            CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;

            // submodules (only module-kind types can be submodule types)
            INedElement submodules = targetType instanceof IModuleKindTypeElement ? compound.getFirstChildWithTag(NedElementTags.NED_SUBMODULES) : null;
            if (submodules != null) {
                for (INedElement node : submodules) {
                    if (node instanceof SubmoduleElementEx) {
                        SubmoduleElementEx sub = (SubmoduleElementEx) node;
                        INedTypeInfo resolvedType = sub.getNedTypeInfo();
                        if (resolvedType != null && resolvedType.getNedElement() == targetType) {
                            modified |= updateSubmoduleOrConnectionType(sub, newSimpleName, newQName);
                        }
                    }
                }
            }

            // connections (only channel-kind types can be connection types)
            INedElement connections = targetType instanceof IChannelKindTypeElement ? compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS) : null;
            if (connections != null) {
                for (INedElement node : connections) {
                    if (node instanceof ConnectionElementEx) {
                        ConnectionElementEx conn = (ConnectionElementEx) node;
                        INedTypeInfo resolvedType = conn.getNedTypeInfo();
                        if (resolvedType != null && resolvedType.getNedElement() == targetType) {
                            modified |= updateSubmoduleOrConnectionType(conn, newSimpleName, newQName);
                        }
                    }
                }
            }
        }

        return modified;
    }

    /**
     * Updates the type or likeType attribute of a submodule or connection element,
     * preserving the reference style (simple name vs fully qualified).
     */
    private static boolean updateSubmoduleOrConnectionType(ISubmoduleOrConnection subOrConn, String newSimpleName, String newQName) {
        boolean modified = false;
        String type = subOrConn.getType();
        if (!StringUtils.isEmpty(type)) {
            subOrConn.setType(type.contains(".") ? newQName : newSimpleName);
            modified = true;
        }
        String likeType = subOrConn.getLikeType();
        if (!StringUtils.isEmpty(likeType)) {
            subOrConn.setLikeType(likeType.contains(".") ? newQName : newSimpleName);
            modified = true;
        }
        return modified;
    }

    /**
     * Updates import statements in a NED file that reference the old fully qualified name.
     * Returns true if any modifications were made.
     */
    private static boolean updateImportReferences(NedFileElementEx nedFileElement, String oldQName, String newQName) {
        boolean modified = false;
        for (INedElement child : nedFileElement) {
            if (child instanceof ImportElement) {
                ImportElement imp = (ImportElement) child;
                if (oldQName.equals(imp.getImportSpec())) {
                    imp.setImportSpec(newQName);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Renames a submodule declaration and updates all references across all NED files
     * in the project. Connections that reference the submodule, and deep parameter
     * assignments (e.g., {@code submod.param = value}) are updated in the declaring
     * compound module and all its subtypes.
     *
     * @param subDecl the submodule declaration element to rename
     * @param newName the new submodule name
     * @return the set of NedFileElementEx objects that were modified (excluding the declaration file)
     */
    public static Set<NedFileElementEx> renameSubmodule(SubmoduleElementEx subDecl, String newName) {
        INedTypeElement compoundModule = subDecl.getEnclosingTypeElement();
        String oldName = subDecl.getName();
        INedTypeResolver resolver = compoundModule.getResolver();
        INedTypeInfo compoundTypeInfo = compoundModule.getNedTypeInfo();
        IProject project = compoundTypeInfo.getProject();
        NedFileElementEx declarationFile = compoundModule.getContainingNedFileElement();
        Set<NedFileElementEx> modifiedFiles = new HashSet<>();

        // Snapshot all types before making changes
        List<INedTypeInfo> allTypes = new ArrayList<>(resolver.getToplevelNedTypes(project));

        // Find all compound modules whose inheritance chain includes the declaring compound module
        Set<INedTypeElement> affectedCompoundModules = computeAffectedTypes(allTypes, compoundModule);

        String oldPrefix = oldName + ".";
        String newPrefix = newName + ".";

        // Update connections and deep parameter assignments in affected compound modules
        // (done BEFORE renaming the submodule, to preserve resolved references)
        for (INedTypeElement cm : affectedCompoundModules) {
            if (cm instanceof CompoundModuleElementEx) {
                CompoundModuleElementEx compound = (CompoundModuleElementEx) cm;
                boolean modified = false;

                // Update connection references
                INedElement connectionsNode = compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS);
                if (connectionsNode != null)
                    modified |= updateSubmoduleRefsInConnections(connectionsNode, oldName, newName);

                // Update deep parameter assignments (e.g., "udp.icmpModule = ...")
                INedElement paramsNode = compound.getFirstChildWithTag(NedElementTags.NED_PARAMETERS);
                if (paramsNode != null)
                    modified |= updateDeepParamAssignments(paramsNode, oldPrefix, newPrefix);

                // Update expressions referencing the submodule name (e.g., sizeof(oldName))
                modified |= replaceNameInExpressions(compound, oldName, newName);

                if (modified) {
                    NedFileElementEx file = compound.getContainingNedFileElement();
                    if (file != declarationFile)
                        modifiedFiles.add(file);
                }
            }
        }

        // Rename the submodule declaration itself last
        subDecl.setName(newName);

        return modifiedFiles;
    }

    /**
     * Updates deep parameter assignments whose name starts with the given prefix.
     * E.g., if renaming submodule "udp" to "udpStack", then "udp.icmpModule"
     * becomes "udpStack.icmpModule".
     */
    private static boolean updateDeepParamAssignments(INedElement paramsNode, String oldPrefix, String newPrefix) {
        boolean modified = false;
        for (INedElement child : paramsNode) {
            if (child instanceof ParamElementEx) {
                ParamElementEx param = (ParamElementEx) child;
                String name = param.getName();
                if (name != null && name.startsWith(oldPrefix)) {
                    param.setName(newPrefix + name.substring(oldPrefix.length()));
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Computes the set of types whose inheritance chain includes the given declaring type.
     * This includes the declaring type itself and all its subtypes.
     */
    private static Set<INedTypeElement> computeAffectedTypes(List<INedTypeInfo> allTypes, INedTypeElement declaringType) {
        Set<INedTypeElement> affectedTypes = new HashSet<>();
        for (INedTypeInfo ti : allTypes) {
            for (INedTypeInfo ancestor : ti.getInheritanceChain()) {
                if (ancestor.getNedElement() == declaringType) {
                    affectedTypes.add(ti.getNedElement());
                    break;
                }
            }
        }
        return affectedTypes;
    }

    /**
     * Predicate used to test whether a NED interface type locally declares a particular
     * named member (parameter or gate). Used to seed the interface family computation.
     */
    private interface InterfaceMemberChecker {
        boolean hasMember(INedTypeInfo ifaceTypeInfo, String memberName);
    }

    /**
     * Like {@link #computeAffectedTypes}, but also spans across the moduleinterface/"like"
     * family for the given parameter name. Because a param declared in a moduleinterface
     * is the same logical parameter as the same-named param in any module that implements
     * that interface (directly or transitively), all such types must be renamed together.
     */
    private static Set<INedTypeElement> computeAffectedTypesForParam(List<INedTypeInfo> allTypes,
            INedTypeElement declaringType, String paramName) {
        return computeAffectedTypesForMember(allTypes, declaringType, paramName,
                new InterfaceMemberChecker() {
                    public boolean hasMember(INedTypeInfo ti, String name) {
                        return ti.getLocalParamDeclarations().containsKey(name)
                                || ti.getLocalParams().containsKey(name);
                    }
                });
    }

    /**
     * Like {@link #computeAffectedTypes}, but also spans across the moduleinterface/"like"
     * family for the given gate name. Because a gate declared in a moduleinterface
     * is the same logical gate as the same-named gate in any module that implements
     * that interface (directly or transitively), all such types must be renamed together.
     */
    private static Set<INedTypeElement> computeAffectedTypesForGate(List<INedTypeInfo> allTypes,
            INedTypeElement declaringType, String gateName) {
        return computeAffectedTypesForMember(allTypes, declaringType, gateName,
                new InterfaceMemberChecker() {
                    public boolean hasMember(INedTypeInfo ti, String name) {
                        return ti.getLocalGateDeclarations().containsKey(name);
                    }
                });
    }

    /**
     * Shared implementation for {@link #computeAffectedTypesForParam} and
     * {@link #computeAffectedTypesForGate}. Expands the affected-types set across the
     * moduleinterface/"like" family for the given member name.
     *
     * <p>Algorithm:
     * 1. Find the set of interfaces that "own" memberName (the interface family).
     * 2. Find every concrete type (module/channel) that implements any of those interfaces.
     * 3. Return the union of computeAffectedTypes() for the declaring type, for each
     *    interface in the family, and for each concrete implementor.
     */
    private static Set<INedTypeElement> computeAffectedTypesForMember(List<INedTypeInfo> allTypes,
            INedTypeElement declaringType, String memberName, InterfaceMemberChecker checker) {
        // Step 1: compute interface family
        Set<INedTypeElement> interfaceFamily = computeInterfaceFamily(allTypes, declaringType, memberName, checker);

        // Step 2 & 3: union of computeAffectedTypes for each seed
        Set<INedTypeElement> affectedTypes = computeAffectedTypes(allTypes, declaringType);

        // Include the interface types themselves (their own declarations must be renamed)
        affectedTypes.addAll(interfaceFamily);

        // For each concrete type that implements any interface in the family, expand via extends chain
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (typeElement instanceof IInterfaceTypeElement)
                continue; // already handled above
            for (INedTypeElement iface : ti.getInterfaces()) {
                if (interfaceFamily.contains(iface)) {
                    affectedTypes.addAll(computeAffectedTypes(allTypes, typeElement));
                    break;
                }
            }
        }

        return affectedTypes;
    }

    /**
     * Computes the set of interfaces related to memberName starting from declaringType.
     *
     * <p>If declaringType is itself an interface, the family starts with declaringType
     * and all interfaces reachable by transitively following the interface's
     * "extends" links (via getLocalInterfaces()).
     *
     * <p>If declaringType is a concrete type (module/channel), the family is seeded
     * with any of its interfaces that own memberName (as determined by checker), again
     * expanded transitively.
     *
     * <p>In both cases the family is further expanded by adding any interface in allTypes
     * whose own "extends" chain includes a family member (upward closure).
     */
    private static Set<INedTypeElement> computeInterfaceFamily(List<INedTypeInfo> allTypes,
            INedTypeElement declaringType, String memberName, InterfaceMemberChecker checker) {
        Set<INedTypeElement> family = new HashSet<>();

        // Seed the family
        if (declaringType instanceof IInterfaceTypeElement) {
            // declaringType is an interface — add it and all interfaces it transitively extends
            addInterfaceAndTransitiveExtends(declaringType, family);
        } else {
            // declaringType is a concrete type — seed from interfaces that own memberName
            for (INedTypeElement iface : declaringType.getNedTypeInfo().getInterfaces()) {
                if (checker.hasMember(iface.getNedTypeInfo(), memberName)) {
                    addInterfaceAndTransitiveExtends(iface, family);
                }
            }
        }

        // Upward closure: if any interface in allTypes has a local interface (extends) that
        // is already in the family, that interface also belongs to the family.
        // Repeat until stable.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (INedTypeInfo ti : allTypes) {
                INedTypeElement typeElement = ti.getNedElement();
                if (!(typeElement instanceof IInterfaceTypeElement))
                    continue;
                if (family.contains(typeElement))
                    continue;
                for (INedTypeElement localIface : ti.getLocalInterfaces()) {
                    if (family.contains(localIface)) {
                        family.add(typeElement);
                        changed = true;
                        break;
                    }
                }
            }
        }

        return family;
    }

    /**
     * Adds the given interface element and all interfaces it transitively extends
     * (via getLocalInterfaces()) to the given set.
     */
    private static void addInterfaceAndTransitiveExtends(INedTypeElement iface, Set<INedTypeElement> result) {
        if (result.contains(iface))
            return;
        result.add(iface);
        for (INedTypeElement extended : iface.getNedTypeInfo().getLocalInterfaces())
            if (extended instanceof IInterfaceTypeElement)
                addInterfaceAndTransitiveExtends(extended, result);
    }

    /**
     * Updates submodule references in connections within a connections block.
     * Returns true if any modifications were made.
     */
    private static boolean updateSubmoduleRefsInConnections(INedElement connectionsNode, String oldName, String newName) {
        boolean modified = false;
        for (INedElement child : connectionsNode) {
            if (child instanceof ConnectionElementEx) {
                ConnectionElementEx conn = (ConnectionElementEx) child;

                // Update source module name
                if (oldName.equals(conn.getSrcModule())) {
                    conn.setSrcModule(newName);
                    modified = true;
                }

                // Update destination module name
                if (oldName.equals(conn.getDestModule())) {
                    conn.setDestModule(newName);
                    modified = true;
                }
            }
            else if (child instanceof ConnectionGroupElement) {
                modified |= updateSubmoduleRefsInConnections(child, oldName, newName);
            }
        }
        return modified;
    }

    /**
     * Renames a gate declaration and updates all references across all NED files
     * in the project. Connection references (srcGate/destGate) in the declaring
     * type and all its subtypes are updated, as well as in compound modules that
     * instantiate the declaring type as a submodule.
     *
     * @param gateDecl the gate declaration element to rename
     * @param newName the new gate name
     * @return the set of NedFileElementEx objects that were modified (excluding the declaration file)
     */
    public static Set<NedFileElementEx> renameGate(GateElementEx gateDecl, String newName) {
        INedTypeElement declaringType = gateDecl.getEnclosingTypeElement();
        String oldName = gateDecl.getName();
        INedTypeResolver resolver = declaringType.getResolver();
        INedTypeInfo declaringTypeInfo = declaringType.getNedTypeInfo();
        IProject project = declaringTypeInfo.getProject();
        NedFileElementEx declarationFile = declaringType.getContainingNedFileElement();
        Set<NedFileElementEx> modifiedFiles = new HashSet<>();

        // Snapshot all types before making changes (including inner types from types: blocks)
        List<INedTypeInfo> allTypes = new ArrayList<>(resolver.getToplevelNedTypes(project));
        collectInnerTypes(allTypes);

        // Find all types whose inheritance chain includes the declaring type,
        // expanded across the moduleinterface/like family for the given gate name.
        Set<INedTypeElement> affectedTypes = computeAffectedTypesForGate(allTypes, declaringType, oldName);

        // Pre-resolve submodule lists and their types BEFORE any tree mutations.
        // This avoids repeated NedResources lock acquisitions and expensive rehashes
        // during the post-mutation loop (tree mutations invalidate the type hash
        // tables, and subsequent type lookups would trigger full rehashes under the
        // lock, blocking the UI thread — potential deadlock).
        Map<CompoundModuleElementEx, List<SubmoduleElementEx>> compoundSubsCache = new HashMap<>();
        Map<SubmoduleElementEx, INedTypeInfo> subTypeCache = new HashMap<>();
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!(typeElement instanceof CompoundModuleElementEx))
                continue;
            CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;
            List<SubmoduleElementEx> subs = compound.getSubmodules();
            compoundSubsCache.put(compound, subs);
            for (SubmoduleElementEx sub : subs) {
                INedTypeInfo subTypeInfo = sub.getNedTypeInfo();
                if (subTypeInfo != null)
                    subTypeCache.put(sub, subTypeInfo);
            }
        }

        // Update gate declarations and expressions in the declaring type and its subtypes
        // (done BEFORE renaming the gate declaration, to preserve resolved references)
        for (INedTypeElement affectedType : affectedTypes) {
            boolean modified = updateGateDeclaration(affectedType, declaringTypeInfo, oldName, newName);
            // Update expressions referencing the gate name (e.g., sizeof(oldName))
            modified |= replaceNameInExpressions(affectedType, oldName, newName);
            if (modified) {
                NedFileElementEx file = affectedType.getContainingNedFileElement();
                if (file != declarationFile)
                    modifiedFiles.add(file);
            }
        }

        // Update connection references in compound modules that use the declaring type
        // either directly (as the compound module itself) or as a submodule type
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!(typeElement instanceof CompoundModuleElementEx))
                continue;
            CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;

            boolean modified = false;

            // Case 1: the compound module itself is (or extends) the declaring type —
            // connections to the parent module's own gates use an empty module name
            if (affectedTypes.contains(compound)) {
                INedElement connectionsNode = compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS);
                if (connectionsNode != null)
                    modified |= updateGateRefsInConnections(connectionsNode, "", oldName, newName);
            }

            // Case 2: the compound module has submodules of the declaring type —
            // connections to those submodules' gates use the submodule name
            // (uses pre-cached submodule lists and types to avoid rehash under lock)
            for (SubmoduleElementEx sub : compoundSubsCache.getOrDefault(compound, Collections.emptyList())) {
                INedTypeInfo subTypeInfo = subTypeCache.get(sub);
                if (subTypeInfo != null && affectedTypes.contains(subTypeInfo.getNedElement())) {
                    // Submodule inline gate size specs: "sub: Type { gates: ethg[2]; }"
                    for (GateElementEx ownGate : sub.getOwnGates()) {
                        if (oldName.equals(ownGate.getName())) {
                            ownGate.setName(newName);
                            modified = true;
                        }
                    }
                    INedElement connectionsNode = compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS);
                    if (connectionsNode != null)
                        modified |= updateGateRefsInConnections(connectionsNode, sub.getName(), oldName, newName);
                    // Update expressions referencing the gate name (e.g., sizeof(sub.oldGate))
                    if (!affectedTypes.contains(compound))
                        modified |= replaceNameInExpressions(compound, oldName, newName);
                }
            }

            if (modified) {
                NedFileElementEx file = compound.getContainingNedFileElement();
                if (file != declarationFile)
                    modifiedFiles.add(file);
            }
        }

        // Rename the gate declaration itself last
        gateDecl.setName(newName);

        return modifiedFiles;
    }

    /**
     * Updates a gate declaration with the given old name inside a type that is part of
     * the affected inheritance hierarchy. Only updates the gate if it was inherited from
     * (or declared in) the original declaring type.
     * Returns true if any modification was made.
     */
    private static boolean updateGateDeclaration(INedTypeElement typeElement, INedTypeInfo declaringTypeInfo, String oldName, String newName) {
        // Only update the gate if this type directly declares or overrides it
        INedElement gatesNode = typeElement.getFirstChildWithTag(NedElementTags.NED_GATES);
        if (gatesNode == null)
            return false;
        boolean modified = false;
        for (INedElement child : gatesNode) {
            if (child instanceof GateElementEx) {
                GateElementEx gate = (GateElementEx) child;
                if (oldName.equals(gate.getName())) {
                    gate.setName(newName);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Updates connection srcGate/destGate references for a specific module name
     * within a connections block. An empty moduleName matches parent-module gate refs
     * (i.e., connections where srcModule or destModule is empty).
     * Returns true if any modifications were made.
     */
    private static boolean updateGateRefsInConnections(INedElement connectionsNode, String moduleName, String oldGateName, String newGateName) {
        boolean modified = false;
        for (INedElement child : connectionsNode) {
            if (child instanceof ConnectionElementEx) {
                ConnectionElementEx conn = (ConnectionElementEx) child;

                if (moduleName.equals(conn.getSrcModule()) && oldGateName.equals(conn.getSrcGate())) {
                    conn.setSrcGate(newGateName);
                    modified = true;
                }
                if (moduleName.equals(conn.getDestModule()) && oldGateName.equals(conn.getDestGate())) {
                    conn.setDestGate(newGateName);
                    modified = true;
                }
            }
            else if (child instanceof ConnectionGroupElement) {
                modified |= updateGateRefsInConnections(child, moduleName, oldGateName, newGateName);
            }
        }
        return modified;
    }

    /**
     * Renames a parameter declaration and updates all references across all NED files
     * in the project. This covers:
     * <ul>
     *   <li>The parameter declaration and same-name assignments in the declaring type
     *       and all subtypes (inheritance hierarchy).</li>
     *   <li>Submodule inline parameter assignments: {@code submod: Type { parameters: param = val; }}</li>
     *   <li>Connection inline channel parameter assignments: {@code A.out --> { delay = val; } --> B.in}</li>
     *   <li>Compound module top-level deep assignments: {@code submod.param = val},
     *       {@code submod.sub2.param = val}, and pattern forms like {@code **.param = val}.</li>
     *   <li>Parameter value expressions that reference the old name as an identifier,
     *       e.g. {@code otherParam = oldParam + 1} or {@code submod.p = parent.oldParam}.</li>
     * </ul>
     *
     * @param paramDecl the parameter declaration element to rename
     * @param newName the new parameter name
     * @return the set of NedFileElementEx objects that were modified (excluding the declaration file)
     */
    public static Set<NedFileElementEx> renameParam(ParamElementEx paramDecl, String newName) {
        INedTypeElement declaringType = paramDecl.getEnclosingTypeElement();
        String oldName = paramDecl.getName();
        INedTypeResolver resolver = declaringType.getResolver();
        INedTypeInfo declaringTypeInfo = declaringType.getNedTypeInfo();
        IProject project = declaringTypeInfo.getProject();
        NedFileElementEx declarationFile = declaringType.getContainingNedFileElement();
        Set<NedFileElementEx> modifiedFiles = new HashSet<>();

        // Snapshot all types before making changes (including inner types from types: blocks)
        List<INedTypeInfo> allTypes = new ArrayList<>(resolver.getToplevelNedTypes(project));
        collectInnerTypes(allTypes);

        // Find all types whose inheritance chain includes the declaring type,
        // expanded across the moduleinterface/like family for the given param name.
        Set<INedTypeElement> affectedTypes = computeAffectedTypesForParam(allTypes, declaringType, oldName);

        // Compute the set of compound modules that transitively use an affected type
        // (i.e. have an affected type anywhere in their submodule hierarchy, at any depth).
        // This is needed to correctly handle deep assignments like "ipv4.arp.param = val"
        // in a compound module that doesn't directly contain an affected-type submodule,
        // but contains a submodule whose type does.
        Set<INedTypeElement> compoundsTransitivelyUsingAffected = new HashSet<>();
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!(typeElement instanceof CompoundModuleElementEx))
                continue;
            // getUsedTypes() returns all types used as submodules or connections, including
            // inherited ones, but NOT transitively (i.e. not the submodules of submodules).
            // We need transitive closure, so we do a BFS/DFS over the submodule type graph.
            if (transitivelyUsesAffectedType(ti, affectedTypes, new HashSet<>()))
                compoundsTransitivelyUsingAffected.add(typeElement);
        }

        // Pre-resolve submodule lists, submodule types, and connection types
        // BEFORE any tree mutations. This avoids repeated NedResources lock
        // acquisitions and expensive rehashes during the mutation loops (tree
        // mutations invalidate the type hash tables, and subsequent type lookups
        // would trigger full rehashes under the lock, blocking the UI thread —
        // potential deadlock).
        Map<CompoundModuleElementEx, List<SubmoduleElementEx>> compoundSubsCache = new HashMap<>();
        Map<SubmoduleElementEx, INedTypeInfo> subTypeCache = new HashMap<>();
        Map<ConnectionElementEx, INedTypeInfo> connTypeCache = new HashMap<>();
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!(typeElement instanceof CompoundModuleElementEx))
                continue;
            CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;
            List<SubmoduleElementEx> subs = compound.getSubmodules();
            compoundSubsCache.put(compound, subs);
            for (SubmoduleElementEx sub : subs) {
                INedTypeInfo subTypeInfo = sub.getNedTypeInfo();
                if (subTypeInfo != null)
                    subTypeCache.put(sub, subTypeInfo);
            }
            INedElement connectionsNode = compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS);
            if (connectionsNode != null) {
                for (INedElement child : connectionsNode) {
                    if (child instanceof ConnectionElementEx) {
                        ConnectionElementEx conn = (ConnectionElementEx) child;
                        INedTypeInfo chanTypeInfo = conn.getNedTypeInfo();
                        if (chanTypeInfo != null)
                            connTypeCache.put(conn, chanTypeInfo);
                    }
                }
            }
        }

        // --- Pass 1: update all ParamElementEx NAME attributes ---
        //
        // Walk every type in the project. For each ParamElementEx, decide whether
        // its 'name' attribute refers to the renamed parameter and update it.

        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            NedFileElementEx file = typeElement.getContainingNedFileElement();
            boolean modified = false;

            if (affectedTypes.contains(typeElement)) {
                // Direct declarations and assignments in the affected type's own parameters block
                // (name == oldName exactly)
                modified |= renameParamInBlock(typeElement.getFirstChildWithTag(NedElementTags.NED_PARAMETERS),
                        oldName, newName, false);
            }

            if (typeElement instanceof CompoundModuleElementEx) {
                CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;
                INedElement compoundParams = compound.getFirstChildWithTag(NedElementTags.NED_PARAMETERS);

                // Submodule inline parameters blocks: "submod: Type { parameters: param = val; }"
                // Here the name is just the bare param name (not prefixed with the submodule name).
                // (uses pre-cached submodule lists and types to avoid rehash under lock)
                for (SubmoduleElementEx sub : compoundSubsCache.getOrDefault(compound, Collections.emptyList())) {
                    INedTypeInfo subTypeInfo = subTypeCache.get(sub);
                    if (subTypeInfo != null && affectedTypes.contains(subTypeInfo.getNedElement())) {
                        for (ParamElementEx ownParam : sub.getOwnParams()) {
                            if (oldName.equals(ownParam.getName())) {
                                ownParam.setName(newName);
                                modified = true;
                            }
                        }
                    }
                }

                // Connection inline channel parameters: "A.out --> { delay = val; } --> B.in"
                // (uses pre-cached connection types to avoid rehash under lock)
                INedElement connectionsNode = compound.getFirstChildWithTag(NedElementTags.NED_CONNECTIONS);
                if (connectionsNode != null) {
                    for (INedElement child : connectionsNode) {
                        if (child instanceof ConnectionElementEx) {
                            ConnectionElementEx conn = (ConnectionElementEx) child;
                            INedTypeInfo chanTypeInfo = connTypeCache.get(conn);
                            if (chanTypeInfo != null && affectedTypes.contains(chanTypeInfo.getNedElement())) {
                                for (ParamElementEx ownParam : conn.getOwnParams()) {
                                    if (oldName.equals(ownParam.getName())) {
                                        ownParam.setName(newName);
                                        modified = true;
                                    }
                                }
                            }
                        }
                    }
                }

                // Compound top-level params block: deep assignments like
                // "submod.param", "ipv4.arp.param", "submod.**.param", "**.param" etc.
                // Rename any dotted-path entry whose last segment equals oldName, for any
                // compound that transitively uses an affected type at any depth.
                // Bare names (no dot) are excluded here — those are handled above via
                // renameParamInBlock(false) for types in affectedTypes.
                if (compoundParams != null && compoundsTransitivelyUsingAffected.contains(compound))
                    modified |= renameParamInBlockDottedPaths(compoundParams, oldName, newName);
            }

            if (modified && file != declarationFile)
                modifiedFiles.add(file);
        }

        // --- Pass 2: update all ParamElementEx VALUE expressions ---
        //
        // A parameter value expression can reference the renamed parameter by name,
        // e.g. "otherParam = oldParam + 1" or "submod.p = parent.oldParam".
        // We do a whole-word substitution in the value string for all ParamElementEx
        // nodes in types where the old param name is in scope.
        //
        // Scope: the param is in scope in affected types themselves, and in compound
        // modules that use an affected type as a submodule (via "parent.paramName"
        // or direct reference inside the submodule's own block).

        // Build the set of compound modules that contain submodules of affected types
        // (uses pre-cached submodule lists and types to avoid rehash under lock)
        Set<INedTypeElement> compoundsWithAffectedSubmodules = new HashSet<>();
        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!(typeElement instanceof CompoundModuleElementEx))
                continue;
            CompoundModuleElementEx compound = (CompoundModuleElementEx) typeElement;
            for (SubmoduleElementEx sub : compoundSubsCache.getOrDefault(compound, Collections.emptyList())) {
                INedTypeInfo subTypeInfo = subTypeCache.get(sub);
                if (subTypeInfo != null && affectedTypes.contains(subTypeInfo.getNedElement())) {
                    compoundsWithAffectedSubmodules.add(compound);
                    break;
                }
            }
        }

        // Types where the param name is in scope for value expressions
        Set<INedTypeElement> valueScope = new HashSet<>();
        valueScope.addAll(affectedTypes);
        valueScope.addAll(compoundsWithAffectedSubmodules);

        for (INedTypeInfo ti : allTypes) {
            INedTypeElement typeElement = ti.getNedElement();
            if (!valueScope.contains(typeElement))
                continue;

            NedFileElementEx file = typeElement.getContainingNedFileElement();
            boolean modified = renameParamInValues(typeElement, oldName, newName);
            if (modified && file != declarationFile)
                modifiedFiles.add(file);
        }

        return modifiedFiles;
    }

    /**
     * Renames the parameter in the name attribute of ParamElementEx nodes within a
     * parameters block.
     *
     * When {@code dotted} is false, only exact matches of {@code oldName} are renamed
     * (for declarations and direct assignments).
     *
     * When {@code dotted} is true, the name is treated as a dotted path and the last
     * segment is matched against {@code oldName}. This covers deep assignments like
     * {@code submod.param}, {@code submod.sub2.param}, and pattern forms like
     * {@code **.param} or {@code submod.**.param}.
     *
     * Returns true if any modification was made.
     */
    private static boolean renameParamInBlock(INedElement paramsNode, String oldName, String newName, boolean dotted) {
        if (paramsNode == null)
            return false;
        boolean modified = false;
        for (INedElement child : paramsNode) {
            if (child instanceof ParamElementEx) {
                ParamElementEx param = (ParamElementEx) child;
                String name = param.getName();
                if (name == null)
                    continue;
                if (!dotted) {
                    // exact match
                    if (oldName.equals(name)) {
                        param.setName(newName);
                        modified = true;
                    }
                } else {
                    // dotted path: rename the last segment if it equals oldName
                    String renamed = renameLastSegment(name, oldName, newName);
                    if (renamed != null) {
                        param.setName(renamed);
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    /**
     * If the last dotted segment of {@code path} equals {@code oldSegment}, returns
     * the path with the last segment replaced by {@code newSegment}. Otherwise returns null.
     * Handles bare names (no dot), dotted paths, and wildcard patterns like {@code **}.
     */
    private static String renameLastSegment(String path, String oldSegment, String newSegment) {
        int lastDot = path.lastIndexOf('.');
        String lastSeg = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        if (!oldSegment.equals(lastSeg))
            return null;
        if (lastDot < 0)
            return newSegment;
        return path.substring(0, lastDot + 1) + newSegment;
    }

    /**
     * Recursively collects inner types (from {@code types:} blocks) of all compound
     * modules in {@code allTypes} and appends them to the list. This ensures that
     * local type definitions are also processed during rename operations.
     */
    private static void collectInnerTypes(List<INedTypeInfo> allTypes) {
        List<INedTypeInfo> innerTypes = new ArrayList<>();
        for (INedTypeInfo ti : allTypes)
            collectInnerTypesRecursive(ti.getNedElement(), innerTypes);
        allTypes.addAll(innerTypes);
    }

    private static void collectInnerTypesRecursive(INedTypeElement typeElement, List<INedTypeInfo> result) {
        if (typeElement instanceof CompoundModuleElementEx) {
            for (INedTypeElement inner : ((CompoundModuleElementEx) typeElement).getOwnInnerTypes()) {
                INedTypeInfo innerInfo = inner.getNedTypeInfo();
                if (innerInfo != null) {
                    result.add(innerInfo);
                    collectInnerTypesRecursive(inner, result);
                }
            }
        }
    }

    /**
     * Returns true if the given type transitively uses any type in {@code affectedTypes}
     * as a submodule or connection channel, at any depth of nesting.
     * Uses {@code visited} to break cycles.
     */
    private static boolean transitivelyUsesAffectedType(INedTypeInfo typeInfo,
            Set<INedTypeElement> affectedTypes, Set<INedTypeInfo> visited) {
        if (!visited.add(typeInfo))
            return false;
        for (INedTypeElement usedType : typeInfo.getUsedTypes()) {
            if (affectedTypes.contains(usedType))
                return true;
            INedTypeInfo usedTypeInfo = usedType.getNedTypeInfo();
            if (usedTypeInfo != null && transitivelyUsesAffectedType(usedTypeInfo, affectedTypes, visited))
                return true;
        }
        return false;
    }

    /**
     * Renames the parameter in the name attribute of ParamElementEx nodes within a
     * parameters block, but only for entries that contain a dot (i.e. are dotted paths
     * like {@code submod.param}, {@code ipv4.arp.param}, {@code submod.**.param}).
     * Bare names (no dot) are excluded — those are handled separately by
     * {@link #renameParamInBlock} with {@code dotted=false}.
     * Returns true if any modification was made.
     */
    private static boolean renameParamInBlockDottedPaths(INedElement paramsNode,
            String oldName, String newName) {
        if (paramsNode == null)
            return false;
        boolean modified = false;
        for (INedElement child : paramsNode) {
            if (child instanceof ParamElementEx) {
                ParamElementEx param = (ParamElementEx) child;
                String name = param.getName();
                if (name == null || !name.contains("."))
                    continue;  // bare names handled elsewhere
                String renamed = renameLastSegment(name, oldName, newName);
                if (renamed != null) {
                    param.setName(renamed);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Renames whole-word occurrences of {@code oldName} in the value expressions of
     * all ParamElementEx nodes within the given type element (including submodule and
     * connection inline parameter blocks).
     * Returns true if any modification was made.
     */
    private static boolean renameParamInValues(INedTypeElement typeElement, String oldName, String newName) {
        boolean modified = false;
        // Use a visitor to walk the entire subtree of the type element
        for (INedElement node : collectParamElements(typeElement)) {
            ParamElementEx param = (ParamElementEx) node;
            String value = param.getValue();
            if (value != null && !value.isEmpty()) {
                String updated = replaceWholeWord(value, oldName, newName);
                if (!updated.equals(value)) {
                    param.setValue(updated);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Collects all ParamElementEx nodes in the subtree rooted at the given element.
     */
    private static List<ParamElementEx> collectParamElements(INedElement root) {
        List<ParamElementEx> result = new ArrayList<>();
        collectParamElementsRecursive(root, result);
        return result;
    }

    private static void collectParamElementsRecursive(INedElement node, List<ParamElementEx> result) {
        if (node instanceof ParamElementEx)
            result.add((ParamElementEx) node);
        for (INedElement child : node)
            collectParamElementsRecursive(child, result);
    }

    /**
     * Recursively walks the subtree rooted at {@code element} and replaces whole-word
     * occurrences of {@code oldName} with {@code newName} in all NED expression attributes:
     * vector sizes, index expressions, loop bounds, conditions, like-expressions, and
     * parameter values. This handles e.g. {@code sizeof(oldName)} references.
     * Returns true if any modification was made.
     */
    private static boolean replaceNameInExpressions(INedElement element, String oldName, String newName) {
        boolean modified = false;
        if (element instanceof SubmoduleElement) {
            SubmoduleElement sub = (SubmoduleElement) element;
            modified |= replaceExprAttr(sub, sub.getVectorSize(), newVal -> sub.setVectorSize(newVal), oldName, newName);
            modified |= replaceExprAttr(sub, sub.getLikeExpr(), newVal -> sub.setLikeExpr(newVal), oldName, newName);
        }
        else if (element instanceof GateElement) {
            GateElement gate = (GateElement) element;
            modified |= replaceExprAttr(gate, gate.getVectorSize(), newVal -> gate.setVectorSize(newVal), oldName, newName);
        }
        else if (element instanceof ConnectionElement) {
            ConnectionElement conn = (ConnectionElement) element;
            modified |= replaceExprAttr(conn, conn.getSrcModuleIndex(), newVal -> conn.setSrcModuleIndex(newVal), oldName, newName);
            modified |= replaceExprAttr(conn, conn.getSrcGateIndex(), newVal -> conn.setSrcGateIndex(newVal), oldName, newName);
            modified |= replaceExprAttr(conn, conn.getDestModuleIndex(), newVal -> conn.setDestModuleIndex(newVal), oldName, newName);
            modified |= replaceExprAttr(conn, conn.getDestGateIndex(), newVal -> conn.setDestGateIndex(newVal), oldName, newName);
            modified |= replaceExprAttr(conn, conn.getLikeExpr(), newVal -> conn.setLikeExpr(newVal), oldName, newName);
        }
        else if (element instanceof LoopElement) {
            LoopElement loop = (LoopElement) element;
            modified |= replaceExprAttr(loop, loop.getFromValue(), newVal -> loop.setFromValue(newVal), oldName, newName);
            modified |= replaceExprAttr(loop, loop.getToValue(), newVal -> loop.setToValue(newVal), oldName, newName);
        }
        else if (element instanceof ConditionElement) {
            ConditionElement cond = (ConditionElement) element;
            modified |= replaceExprAttr(cond, cond.getCondition(), newVal -> cond.setCondition(newVal), oldName, newName);
        }
        else if (element instanceof ParamElementEx) {
            ParamElementEx param = (ParamElementEx) element;
            modified |= replaceExprAttr(param, param.getValue(), newVal -> param.setValue(newVal), oldName, newName);
        }
        for (INedElement child : element)
            modified |= replaceNameInExpressions(child, oldName, newName);
        return modified;
    }

    /**
     * Helper: replaces whole-word occurrences of {@code oldName} in the given attribute
     * value. If a replacement is made, calls the setter with the new value.
     * Returns true if a replacement was made.
     */
    private static boolean replaceExprAttr(INedElement element, String value,
            java.util.function.Consumer<String> setter, String oldName, String newName) {
        if (value != null && !value.isEmpty()) {
            String updated = replaceWholeWord(value, oldName, newName);
            if (!updated.equals(value)) {
                setter.accept(updated);
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces all whole-word occurrences of {@code oldWord} with {@code newWord} in
     * {@code text}. Uses regex word boundaries so that e.g. "param" does not match
     * inside "myParam" or "paramCount".
     */
    private static String replaceWholeWord(String text, String oldWord, String newWord) {
        return text.replaceAll("(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(oldWord) + "(?![A-Za-z0-9_])", newWord);
    }

    public static Collection<AddGateLabels> inferAllGateLabels(INedTypeElement element, boolean forward) {
        Collection<AddGateLabels> result = new HashSet<AddGateLabels>();

        // TODO: why do we need to copy it? some notification seems to cause a recalculation of gate declarations during inference
        for (GateElementEx gate : new ArrayList<GateElementEx>(element.getNedTypeInfo().getGateDeclarations().values()))
            inferGateLabels(gate, forward, result);

        return result;
    }

    public static void inferGateLabels(GateElementEx gate, boolean forward, Collection<AddGateLabels> result) {
        INedTypeElement typeElement = gate.getEnclosingTypeElement();

        for (INedTypeInfo typeInfo : NedResourcesPlugin.getNedResources().getToplevelNedTypesFromAllProjects()) {
            INedTypeElement element = typeInfo.getNedElement();

            if (element instanceof CompoundModuleElementEx) {
                CompoundModuleElementEx compoundModule = (CompoundModuleElementEx)element;

                for (SubmoduleElementEx submoduleElement : compoundModule.getSubmodules()) {
                    if (submoduleElement.getTypeOrLikeTypeRef() == typeElement) {
                        inferLabelsOnConnections(compoundModule.getSrcConnectionsFor(submoduleElement.getName()), gate, forward, result);
                        inferLabelsOnConnections(compoundModule.getDestConnectionsFor(submoduleElement.getName()), gate, forward, result);
                    }
                }

                inferLabelsOnConnections(compoundModule.getSrcConnections(), gate, forward, result);
                inferLabelsOnConnections(compoundModule.getDestConnections(), gate, forward, result);
            }
        }
    }

    private static void inferLabelsOnConnections(List<ConnectionElementEx> connections, GateElementEx gate1, boolean forward, Collection<AddGateLabels> result) {
        INedTypeInfo typeInfo = gate1.getEnclosingTypeElement().getNedTypeInfo();
        for (ConnectionElementEx connection : connections) {
            GateElementEx gate2 = null;
            INedTypeInfo srcTypeInfo = connection.getSrcModuleRef().getNedTypeInfo();
            INedTypeInfo destTypeInfo = connection.getDestModuleRef().getNedTypeInfo();

            if (connection.getDestGate().equals(gate1.getName()) && destTypeInfo == typeInfo)
                gate2 = srcTypeInfo.getGateDeclarations().get(connection.getSrcGate());
            else if (connection.getSrcGate().equals(gate1.getName()) && srcTypeInfo == typeInfo)
                gate2 = destTypeInfo.getGateDeclarations().get(connection.getDestGate());

            if (gate2 != null) {
                if (forward)
                    inferGateLabels(gate1, gate2, result);
                else
                    inferGateLabels(gate2, gate1, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void inferGateLabels(GateElementEx fromGate, GateElementEx toGate, Collection<AddGateLabels> result) {
        ArrayList<String> fromLabels = NedElementUtilEx.getLabels(fromGate);
        ArrayList<String> toLabels = NedElementUtilEx.getLabels(toGate);
        Collection<String> addedLabels = CollectionUtils.subtract(fromLabels, toLabels);

        if (!addedLabels.isEmpty())
            result.add(new AddGateLabels(toGate, addedLabels));
    }

    public static class AddGateLabels implements Runnable {
        public GateElementEx gate;
        public Collection<String> labels;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((gate == null) ? 0 : gate.hashCode());
            result = prime * result + ((labels == null) ? 0 : labels.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AddGateLabels other = (AddGateLabels) obj;
            if (gate == null) {
                if (other.gate != null)
                    return false;
            }
            else if (!gate.equals(other.gate))
                return false;
            if (labels == null) {
                if (other.labels != null)
                    return false;
            }
            else if (!labels.equals(other.labels))
                return false;
            return true;
        }

        public AddGateLabels(GateElementEx gate, Collection<String> labels) {
            this.gate = gate;
            this.labels = labels;
        }

        public void run() {
            //Debug.println("*** Adding labels: " + labels + " to gate: " + gate.getEnclosingTypeElement().getName() + "." + gate.getName());
            NedElementUtilEx.addLabels(gate, labels);
        }
    }
}
