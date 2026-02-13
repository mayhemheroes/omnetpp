/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.omnetpp.common.ui.GenericTreeNode;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeLookupContext;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;
import org.omnetpp.ned.model.pojo.ExtendsElement;
import org.omnetpp.ned.model.pojo.InterfaceNameElement;

/**
 * Utility methods for building NED type inheritance trees.
 *
 * @author andras
 */
public class NedTypeHierarchyUtil {

    /**
     * Returns the direct subtypes of the given NED type by scanning all known
     * NED types and checking their "extends" and "like" clauses.
     */
    public static List<INedTypeInfo> getSubtypesOf(INedTypeInfo inputType) {
        List<INedTypeInfo> result = new ArrayList<>();
        INedTypeResolver res = inputType.getResolver();

        for (INedTypeInfo type : res.getToplevelNedTypesFromAllProjects()) {
            INedTypeLookupContext lookupContext = type.getNedElement().getParentLookupContext();
            for (INedElement child : type.getNedElement()) {
                String superName = null;
                if (child instanceof ExtendsElement)
                    superName = ((ExtendsElement) child).getName();
                else if (child instanceof InterfaceNameElement)
                    superName = ((InterfaceNameElement) child).getName();
                if (superName != null) {
                    INedTypeInfo superType = res.lookupNedType(superName, lookupContext);
                    if (superType == inputType)
                        result.add(type);
                }
            }
        }
        return result;
    }

    /**
     * Builds a GenericTreeNode tree representing the full type hierarchy of
     * the given NED type. The tree starts with the root of the inheritance chain
     * at the top, then descends through the supertypes to the focus type,
     * then continues with the subtypes recursively.
     *
     * @param focusType the NED type to build the hierarchy for
     * @return a result containing the root node and the node for the focus type
     */
    public static HierarchyResult buildInheritanceTree(INedTypeInfo focusType) {
        GenericTreeNode rootNode = new GenericTreeNode("root");
        GenericTreeNode currentNode = rootNode;

        List<INedTypeInfo> extendsChain = focusType.getInheritanceChain();
        for (INedTypeInfo nedType : extendsChain.reversed()) {
            if (nedType != focusType) {
                GenericTreeNode newNode = new GenericTreeNode(nedType.getNedElement());
                currentNode.addChild(newNode);
                currentNode = newNode;
            }
        }

        GenericTreeNode focusNode = buildSubtypeTree(focusType, currentNode, new HashSet<>());
        return new HierarchyResult(rootNode, focusNode);
    }

    /**
     * Result of buildInheritanceTree(), containing the root of the tree
     * and the node that represents the focus type.
     */
    public static class HierarchyResult {
        public final GenericTreeNode rootNode;
        public final GenericTreeNode focusNode;

        public HierarchyResult(GenericTreeNode rootNode, GenericTreeNode focusNode) {
            this.rootNode = rootNode;
            this.focusNode = focusNode;
        }
    }

    private static GenericTreeNode buildSubtypeTree(INedTypeInfo typeInfo,
            GenericTreeNode parentNode, Set<INedTypeInfo> visited) {
        GenericTreeNode node = new GenericTreeNode(typeInfo.getNedElement());
        parentNode.addChild(node);

        if (!visited.contains(typeInfo)) {
            visited.add(typeInfo);
            List<INedTypeInfo> subtypes = getSubtypesOf(typeInfo);
            subtypes.sort(Comparator.comparing(INedTypeInfo::getName));
            for (INedTypeInfo sub : subtypes)
                buildSubtypeTree(sub, node, visited);
        }
        return node;
    }
}
