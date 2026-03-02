/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.editor;

import static org.omnetpp.inifile.core.model.ConfigRegistry.CFGID_ABSTRACT;
import static org.omnetpp.inifile.core.model.ConfigRegistry.CFGID_REPEAT;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.omnetpp.common.util.UIUtils;
import org.omnetpp.inifile.core.model.InifileAnalyzer;
import org.omnetpp.inifile.core.model.InifileUtils;
import org.omnetpp.inifile.core.model.IReadonlyInifileDocument;
import org.omnetpp.inifile.core.model.ITimeout;
import org.omnetpp.inifile.core.model.ParamResolution;
import org.omnetpp.inifile.core.model.ParamResolution.ParamResolutionType;
import org.omnetpp.inifile.core.model.ParamResolutionDisabledException;
import org.omnetpp.inifile.core.model.ParamResolutionTimeoutException;

/**
 * Image constants and image-returning utility methods for the inifile editor UI.
 * Extracted from InifileUtils during the core/editor plugin split.
 *
 * @author Andras
 */
public class InifileImages {
    // for getSectionImage():
    private static final String ICON_SECTION_PNG = "icons/full/obj16/section.png";
    private static final String ICON_SECTION_ABSTRACT_PNG = "icons/full/obj16/section_abstract.png";
    private static final String ICON_SECTION_NONEXISTENT_PNG = "icons/full/obj16/section_nonexistent.png";
    private static final String ICON_REPEAT_PNG = "icons/full/ovr16/section_repeat.png";
    private static final String ICON_ITER_PNG = "icons/full/ovr16/section_iter.png";
    private static final String ICON_WARNING_PNG = "icons/full/ovr16/warning.png";
    private static final String ICON_ERROR_PNG = "icons/full/ovr16/error.png";

    // for getKeyImage()
    public static final Image ICON_ERROR = UIUtils.ICON_ERROR;
    public static final Image ICON_INFO = UIUtils.ICON_INFO;
    public static final Image ICON_PAR_UNASSIGNED = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_unassigned.png");
    public static final Image ICON_PAR_NED = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_ned.png");
    public static final Image ICON_PAR_INIDEFAULT = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_neddefault.png");
    public static final Image ICON_PAR_INIASK = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_iniask.png");
    public static final Image ICON_PAR_INI = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_ini.png");
    public static final Image ICON_PAR_INIOVERRIDE = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_inioverride.png");
    public static final Image ICON_PAR_ININEDDEFAULT = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_inineddefault.png");
    public static final Image ICON_PAR_IMPLICITDEFAULT = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_implicitdefault.png");
    public static final Image ICON_PAR_GROUP = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_group.png");
    public static final Image ICON_PAR_UNKNOWN = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_unknown.png");

    public static final Image ICON_SIGNAL = InifileEditorPlugin.getCachedImage("icons/full/obj16/signal.png");
    public static final Image ICON_STATISTIC = InifileEditorPlugin.getCachedImage("icons/full/obj16/statistic.png");

    public static final Image ICON_PROPOSAL_MODULE = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_module.png");
    public static final Image ICON_PROPOSAL_PARAMETER = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_parameter.png");
    public static final Image ICON_PROPOSAL_GLOBALCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_globalconfig.png");
    public static final Image ICON_PROPOSAL_MODULECONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_moduleconfig.png");
    public static final Image ICON_PROPOSAL_PARAMETERCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_parameterconfig.png");
    public static final Image ICON_PROPOSAL_STATISTICCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_statisticconfig.png");
    public static final Image ICON_PROPOSAL_SCALARCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_scalarconfig.png");
    public static final Image ICON_PROPOSAL_VECTORCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_vectorconfig.png");
    public static final Image ICON_PROPOSAL_OTHERCONFIG = InifileEditorPlugin.getCachedImage("icons/full/obj16/proposal_otherconfig.png");

    public static final Image ICON_KEY_EQUALS_DEFAULT = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_equals_default.png");
    public static final Image ICON_KEY_EQUALS_ASK = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_equals_ask.png");

    public static final Image ICON_INIPARMISC = InifileEditorPlugin.getCachedImage("icons/full/obj16/par_inimisc.png");

    /**
     * Returns an image for the given section, complete with error/warning markers etc.
     */
    public static Image getSectionImage(String sectionName, InifileAnalyzer analyzer) {
        IReadonlyInifileDocument doc = analyzer.getDocument();
        boolean exists = doc.containsSection(sectionName);
        boolean containsIteration = exists ? analyzer.containsIteration(sectionName) : false;
        boolean containsRepeat = exists ? InifileUtils.lookupConfig(sectionName, CFGID_REPEAT.getName(), doc) != null : false;
        boolean isAbstract = exists ? "true".equals(doc.getValue(sectionName, CFGID_ABSTRACT.getName())) : false;
        IMarker[] markers = InifileUtils.getProblemMarkersForWholeSection(sectionName, doc);
        int maxProblemSeverity = InifileUtils.getMaximumSeverity(markers);
        boolean hasError =  maxProblemSeverity == IMarker.SEVERITY_ERROR;
        boolean hasWarning = maxProblemSeverity == IMarker.SEVERITY_WARNING;

        return InifileEditorPlugin.getCachedDecoratedImage(
                !exists ? ICON_SECTION_NONEXISTENT_PNG : isAbstract ? ICON_SECTION_ABSTRACT_PNG : ICON_SECTION_PNG,
                new String[] {
                    containsIteration ? ICON_ITER_PNG : null, // TOP_LEFT
                    containsRepeat ? ICON_REPEAT_PNG : null,  // TOP_RIGHT
                    hasError ? ICON_ERROR_PNG : hasWarning ? ICON_WARNING_PNG : null, // BOTTOM_LEFT
                    null  // BOTTOM_RIGHT
                });
    }

    /**
     * Returns an image for a given inifile key, suitable for displaying in a table or tree.
     */
    public static Image getKeyImage(String section, String key, InifileAnalyzer analyzer, ITimeout timeout) {
        // return an icon based on ParamResolutions
        try {
            ParamResolution[] paramResolutions = analyzer.getParamResolutionsForKey(section, key, timeout);
            if (paramResolutions == null || paramResolutions.length == 0)
                return ICON_PAR_INI;
            if (paramResolutions.length == 1)
                return suggestImage(paramResolutions[0].type);

            // there are more than one ParamResolutions -- collect their types
            Set<ParamResolutionType> types = new HashSet<ParamResolutionType>();
            for (ParamResolution p : paramResolutions)
                types.add(p.type);
            if (types.size() == 1)
                return suggestImage(paramResolutions[0].type);
            return ICON_INIPARMISC;
        } catch (ParamResolutionDisabledException e) {
            return ICON_PAR_UNKNOWN;
        } catch (ParamResolutionTimeoutException e) {
            return ICON_PAR_UNKNOWN;
        }
    }

    /**
     * Helper function: suggests an icon for a table or tree entry.
     */
    public static Image suggestImage(ParamResolutionType type) {
        switch (type) {
            case UNASSIGNED: return ICON_PAR_UNASSIGNED;
            case NED: return ICON_PAR_NED;
            case INI: return ICON_PAR_INI;
            case INI_DEFAULT: return ICON_PAR_INIDEFAULT;
            case INI_ASK: return ICON_PAR_INIASK;
            case INI_OVERRIDE:  return ICON_PAR_INIOVERRIDE;
            case INI_NEDDEFAULT: return ICON_PAR_ININEDDEFAULT;
            case IMPLICITDEFAULT: return ICON_PAR_IMPLICITDEFAULT;
        }
        return null;
    }
}
