/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.neddoc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.CommonPlugin;
import org.omnetpp.common.IConstants;
import org.omnetpp.common.editor.text.Keywords;
import org.omnetpp.common.editor.text.NedCommentFormatter;
import org.omnetpp.common.editor.text.NedCommentFormatter.INeddocProcessor;
import org.omnetpp.common.editor.text.SyntaxHighlightHelper;
import org.omnetpp.common.util.CollectionUtils;
import org.omnetpp.common.util.DisplayUtils;
import org.omnetpp.common.util.FileUtils;
import org.omnetpp.common.util.IPredicate;
import org.omnetpp.common.util.Pair;
import org.omnetpp.common.util.ProcessUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.common.util.StringUtils.IRegexpReplacementProvider;
import org.omnetpp.figures.SubmoduleFigure;
import org.omnetpp.msg.editor.highlight.MsgCodeColorizerScanner;
import org.omnetpp.msg.editor.highlight.MsgDocColorizerScanner;
import org.omnetpp.msg.editor.highlight.MsgPrivateDocColorizerScanner;
import org.omnetpp.msg.editor.highlight.MsgSyntaxHighlightPartitionScanner;
import org.omnetpp.ned.core.INedResources;
import org.omnetpp.ned.core.MsgResources;
import org.omnetpp.ned.core.NedResources;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.editor.export.ExportDiagramFilesOperation;
import org.omnetpp.ned.editor.export.NedFigureProvider;
import org.omnetpp.ned.editor.export.SVGDiagramExporter;
import org.omnetpp.ned.editor.graph.figures.CompoundModuleTypeFigure;
import org.omnetpp.ned.editor.graph.parts.CompoundModuleEditPart;
import org.omnetpp.ned.editor.graph.parts.NedEditPart;
import org.omnetpp.ned.editor.graph.parts.NedTypeEditPart;
import org.omnetpp.ned.editor.graph.parts.SubmoduleEditPart;
import org.omnetpp.ned.editor.text.highlight.NedCodeColorizerScanner;
import org.omnetpp.ned.editor.text.highlight.NedDocColorizerScanner;
import org.omnetpp.ned.editor.text.highlight.NedPrivateDocColorizerScanner;
import org.omnetpp.ned.editor.text.highlight.NedSyntaxHighlightPartitionScanner;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.ChannelElementEx;
import org.omnetpp.ned.model.ex.ChannelInterfaceElementEx;
import org.omnetpp.ned.model.ex.ClassElementEx;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.EnumElementEx;
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.MessageElementEx;
import org.omnetpp.ned.model.ex.ModuleInterfaceElementEx;
import org.omnetpp.ned.model.ex.MsgFileElementEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.ex.PacketElementEx;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.PropertyElementEx;
import org.omnetpp.ned.model.ex.SimpleModuleElementEx;
import org.omnetpp.ned.model.ex.StructElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.IInterfaceTypeElement;
import org.omnetpp.ned.model.interfaces.IModuleTypeElement;
import org.omnetpp.ned.model.interfaces.IMsgTypeElement;
import org.omnetpp.ned.model.interfaces.INedFileElement;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.ITypeElement;
import org.omnetpp.ned.model.pojo.FieldElement;
import org.omnetpp.ned.model.pojo.LiteralElement;
import org.omnetpp.ned.model.pojo.PropertyKeyElement;
import org.omnetpp.neddoc.NeddocExtensions.ExtType;
import org.omnetpp.neddoc.properties.DocumentationGeneratorPropertyPage;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class generates documentation for a single OMNeT++ project. It calls
 * doxygen if requested, and generates documentation from NED and MSG files
 * found in the project. The result is a bunch of HTML and PNG files.
 *
 * The tool relies on the doxygen and graphviz dot executables which are
 * invoked through the runtime's exec facility. The documentation generation
 * takes place in a background job (thread) and a progress monitor is used to present
 * its state to the user. The whole process might take several minutes for
 * large projects such as the INET framework.
 *
 * The generated documentation consists of the following things:
 *  - doxygen documentation (several different kind of pages)
 *  - one page for each NED and MSG file showing its content and a list of declared types
 *  - one page for each type defined in NED and MSG files showing the type's figure,
 *    an inheritance and a usage diagram and various other tables.
 *  - several index pages each listing the declared types of a kind such as
 *    simple modules, compound modules, channels, etc.
 *  - other pages written by the user and extracted from NED and MSG file comments
 *  - separate full inheritance and usage diagrams for NED and MSG files
 *
 * @author levy
 */
@SuppressWarnings("restriction")
public class DocumentationGenerator {
    private static final boolean APPLY_CC = !IConstants.IS_COMMERCIAL;

    private static final Pattern pageSeparatorPattern = Pattern.compile("(?m)^//[ \t]*(@page|@titlepage)(.*?)$");

    // matches a @titlepage
    private static final Pattern titlePagePattern = Pattern.compile("(?m)^//[ \t]*@titlepage(.*?)$");

    // matches @externalpage with name and URL
    private static final Pattern externalPagePattern = Pattern.compile("(?m)^//[ \t]*@externalpage +([^,\n]+),? *(.*?)$");

    // matches @page with name and URL
    private static final Pattern pagePattern = Pattern.compile("(?m)^//[ \t]*@page +([^,\n]+),? *(.*?)$");

    // matches @include and extracts filename
    private final static Pattern includePattern = Pattern.compile("(?m)^//[ \t]*@include[ \t]+(.*?)$");

    // matches @debug and extracts its options
    private final static Pattern debugPattern = Pattern.compile("(?m)^//[ \t]*@debug[ \t]+(.*?)$");

    // matches optionally qualified NED type names
    private final static Pattern nedTypePattern = Pattern.compile(Keywords.NED_IDENT_REGEX + "(\\." + Keywords.NED_IDENT_REGEX + ")*");

    // matches optionally qualified C++ type names, leading "::" is optional
    private final static Pattern msgTypePattern = Pattern.compile("(::)?" + Keywords.MSG_IDENT_REGEX + "(::" + Keywords.MSG_IDENT_REGEX + ")*");

    // configuration flags
    protected boolean headless = false;
    protected boolean generateNedTypeFigures = true;
    protected boolean generatePerTypeUsageDiagrams = true;
    protected boolean generatePerTypeInheritanceDiagrams = true;
    protected boolean generateFullUsageDiagrams = false;
    protected boolean generateFullInheritanceDiagrams = false;
    protected boolean generateSourceListings = true;
    protected boolean generateMsgDefinitions = true;
    protected boolean generateFileListings = true;
    protected boolean automaticHyperlinking = true;  // false: tilde notation; true: autolinking
    protected boolean verboseMode = false;
    protected boolean generateDoxy = true;
    protected boolean generateCppSourceListings = false;
    protected String excludedDirsRegexPattern;

    // path vars
    protected String dotExecutablePath;
    protected String doxyExecutablePath;
    protected IPath documentationRootPath;
    protected IPath rootRelativeDoxyPath;
    protected IPath doxyRelativeRootPath;
    protected IPath absoluteDoxyConfigFilePath; //FIXME should be IFile! because error message about missing doxyfile should show workspace path not filesystem path!
    protected IPath rootRelativeNeddocPath;
    protected IPath neddocRelativeRootPath;
    protected IPath extensionFilePath;
    protected IPath customCssPath;
    protected IProject project;
    protected INedResources nedResources;
    protected MsgResources msgResources;
    protected IProgressMonitor monitor;

    protected FileOutputStream currentOutputStream;

    protected List<IFile> files = new ArrayList<IFile>();
    protected List<ITypeElement> typeElements = new ArrayList<ITypeElement>();
    protected Map<ITypeElement, ArrayList<ITypeElement>> subtypesMap = new LinkedHashMap<ITypeElement, ArrayList<ITypeElement>>();
    protected Map<INedTypeElement, ArrayList<INedTypeElement>> directImplementorsMap = new LinkedHashMap<INedTypeElement, ArrayList<INedTypeElement>>();
    protected Map<INedTypeElement, ArrayList<INedTypeElement>> allImplementorsMap = new LinkedHashMap<INedTypeElement, ArrayList<INedTypeElement>>();
    protected Map<ITypeElement, ArrayList<ITypeElement>> usersMap = new LinkedHashMap<ITypeElement, ArrayList<ITypeElement>>();
    protected Map<String, List<ITypeElement>> typeNamesMap = new LinkedHashMap<String, List<ITypeElement>>();
    protected Map<String, String> doxyMap = new LinkedHashMap<String, String>();
    protected Map<INedElement, String> commentCache = new LinkedHashMap<INedElement, String>();
    protected Map<File, String> includedFileCache = new LinkedHashMap<File,String>();
    protected Pattern possibleTypeReferencesPattern;

    protected ArrayList<String> packageNames;
    protected ArrayList<String> namespaceNames;

    protected int levelIndex[] = {0, 0, 0, 0};  // navigation level indexes for generating breadcrumbs (note that the 0th element is not used)
    protected int previousLevel = 0;
    protected TreeMap<String, ArrayList<Integer>> navigationItemIndex;

    protected INeddocRenderer renderer;
    protected NeddocExtensions neddocExtensions;

    static Image createImage(String base64Data) {
        return new Image(Display.getDefault(), new ImageData(new ByteArrayInputStream(Base64.decode(base64Data.getBytes()))));
    }

    public DocumentationGenerator(IProject project) {
        this.project = project;

        nedResources = NedResourcesPlugin.getNedResources();
        msgResources = NedResourcesPlugin.getMsgResources();

        IPreferenceStore store = CommonPlugin.getConfigurationPreferenceStore();
        dotExecutablePath = ProcessUtils.lookupExecutable(store.getString(IConstants.PREF_GRAPHVIZ_DOT_EXECUTABLE));
        doxyExecutablePath = ProcessUtils.lookupExecutable(store.getString(IConstants.PREF_DOXYGEN_EXECUTABLE));

        // Making sure it will be initialized by the time the documentation
        // generator workspace job is launched, so it doesn't deadlock.
        SyntaxHighlightHelper.init();
    }

    // specify an extension file using a full file system path to the XML doc-extension fragment file
    public void setExtensionFilePath(IPath extensionFilePath) {
        this.extensionFilePath = extensionFilePath;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public void setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
    }

    public void setGenerateNedTypeFigures(boolean generateNedTypeFigures) {
        this.generateNedTypeFigures = generateNedTypeFigures;
    }

    public void setGeneratePerTypeInheritanceDiagrams(boolean generatePerTypeInheritanceDiagrams) {
        this.generatePerTypeInheritanceDiagrams = generatePerTypeInheritanceDiagrams;
    }

    public void setGeneratePerTypeUsageDiagrams(boolean generatePerTypeUsageDiagrams) {
        this.generatePerTypeUsageDiagrams = generatePerTypeUsageDiagrams;
    }

    public void setGenerateFullUsageDiagrams(boolean generateFullUsageDiagrams) {
        this.generateFullUsageDiagrams = generateFullUsageDiagrams;
    }

    public void setGenerateFullInheritanceDiagrams(boolean generateFullInheritanceDiagrams) {
        this.generateFullInheritanceDiagrams = generateFullInheritanceDiagrams;
    }

    public void setGenerateSourceListings(boolean generateSourceListings) {
        this.generateSourceListings = generateSourceListings;
    }

    public void setGenerateMsgDefinitions(boolean generateMsgDefinitions) {
        this.generateMsgDefinitions = generateMsgDefinitions;
    }

    public void setGenerateFileListings(boolean generateFileListings) {
        this.generateFileListings = generateFileListings;
    }

    public void setAutomaticHyperlinking(boolean automaticHyperlinking) {
        this.automaticHyperlinking = automaticHyperlinking;
    }

    public void setGenerateDoxy(boolean generateDoxy) {
        this.generateDoxy = generateDoxy;
    }

    public void setExcludedDirs(String excludedDirs) {
        // accept both space and comma as separtor.
        // create a regex from the * and ** globs,
        // escape single . chars
        // use ¤ char as a temporary char instead of *
        // , is treated as a path separator
        this.excludedDirsRegexPattern =
                excludedDirs.replace(",", " ").replaceAll("\\s+", " ").trim()
                .replace(".", "\\.")
                .replace("**", ".¤?").replace("*", "[^/]*").replace(".¤?", ".*?")
                .replaceAll(" ", ".*?|");
        if (StringUtils.isNotBlank(excludedDirs))
            this.excludedDirsRegexPattern += ".*?";
    }

    public void setGenerateCppSourceListings(boolean generateCppSourceListings) {
        this.generateCppSourceListings = generateCppSourceListings;
    }

    public void setDocumentationRootPath(IPath documentationRootPath) {
        this.documentationRootPath = documentationRootPath;
    }

    public void setRootRelativeDoxyPath(IPath rootRelativeDoxyPath) {
        this.rootRelativeDoxyPath = rootRelativeDoxyPath;
        doxyRelativeRootPath = getReversePath(rootRelativeDoxyPath);
    }

    public void setAbsoluteDoxyConfigFilePath(IPath absoluteDoxyConfigFilePath) {
        this.absoluteDoxyConfigFilePath = absoluteDoxyConfigFilePath;
    }

    public void setRootRelativeNeddocPath(IPath rootRelativeNeddocPath) {
        this.rootRelativeNeddocPath = rootRelativeNeddocPath;
        neddocRelativeRootPath = getReversePath(rootRelativeNeddocPath);
    }

    public void setCustomCssPath(IPath customCssPath) {
        this.customCssPath = customCssPath;
    }

    public IStatus generate(IProgressMonitor monitor) {
        try {
            if (verboseMode)
                System.out.println("Generating NED documentation for '" + project.getName() + "'.");

            DocumentationGenerator.this.monitor = monitor;
            renderer = new HtmlRenderer(documentationRootPath.append(rootRelativeNeddocPath));

            ensureEmptyNeddoc();
            collectCaches();
            generateDoxy();
            collectDoxyMap();
            renderer.copyStaticResources(customCssPath);
            generateNavTreeData();
            generateNavTreeIndex();
            generateNedTypeFigures();
            generatePackagesPage();
            if (generateMsgDefinitions)
                generateNamespacesPage();
            if (generateFileListings)
                generateFilePages();
            generateTypePages();
            generateFullDiagrams();
            generateNedTagFile();
            if (generateMsgDefinitions)
                generateMsgTagFile();

            return Status.OK_STATUS;
        }
        catch (CancellationException e) {
            return Status.CANCEL_STATUS;
        }
        catch (NeddocException e) {
            NeddocPlugin.logError(e);
            return NeddocPlugin.getErrorStatus("Error during generating NED documentation", e);
        }
        catch (IOException e) {
            NeddocPlugin.logError(e);
            return NeddocPlugin.getErrorStatus("Error during generating NED documentation", e);
        }
        catch (CoreException e) {
            NeddocPlugin.logError(e);
            return NeddocPlugin.getErrorStatus("Error during generating NED documentation", e);
        }
        catch (Exception e) {
            NeddocPlugin.logError(e);
            return NeddocPlugin.getErrorStatus("Internal error during generating NED documentation", e);
        }
        finally {
            if (verboseMode)
                System.out.println("");
            try {
                refreshFolder(getFullDoxyPath());
                refreshFolder(getFullNeddocPath());
            }
            catch (Exception e) {
                NeddocPlugin.logError(e);
            }

            if (monitor != null)
                monitor.done();
        }
    }

    protected void ensureEmptyNeddoc() throws CoreException {
        IPath neddocPath = getFullNeddocPath();
        File neddocFile = neddocPath.toFile();

        if (!neddocFile.exists())
            neddocFile.mkdirs();

        File[] files = neddocFile.listFiles();

        if (files != null) {
            try {
                monitor.beginTask("Emptying neddoc...", files.length);

                if (files != null) {
                    for (File file : files) {
                        monitor.subTask(file.toString());
                        file.delete();
                        monitor.worked(1);
                    }
                }
            }
            finally {
                monitor.done();
            }
        }

        refreshFolder(neddocPath);
    }

    protected void refreshFolder(IPath path) throws CoreException {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        if (path.toString().startsWith(workspaceRoot.getLocation().toString())) {
            IFolder folder = workspaceRoot.getFolder(path.removeFirstSegments(workspaceRoot.getLocation().segmentCount()));
            folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    protected void collectCaches() throws Exception {
        try {
            monitor.beginTask("Collecting data...", 6);
            NedResources.getInstance().readMissingNedFiles(); // ensure that all ned files are loaded already

            navigationItemIndex = new TreeMap<String, ArrayList<Integer>>();
            for (int i = 0; i<levelIndex.length; i++)
                levelIndex[i] = 0;
            collectFiles();
            collectTypes();
            collectPackageNames();
            if (generateMsgDefinitions)
                collectNamespaceNames();
            collectTypeNames();
            collectSubtypesMap();
            collectImplementorsMap();
            collectUsersMap();
            collectExtensions();
        }
        finally {
            monitor.done();
        }
    }

    protected void collectFiles() throws CoreException {
        monitor.subTask("Collecting NED and MSG files");
        files = new ArrayList<IFile>();
        files.addAll(nedResources.getNedFiles(project));
        files.addAll(msgResources.getMsgFiles(project));
        files = files.stream().filter(f -> {
            String path = f.getFullPath().removeLastSegments(1).addTrailingSeparator().toString();
            return !path.matches(excludedDirsRegexPattern);

        }).collect(Collectors.toList());
        Collections.sort(files, (IFile o1, IFile o2)-> o1.toString().compareToIgnoreCase(o2.toString()));
        monitor.worked(1);
    }

    protected void collectTypes() throws CoreException, IOException {
        monitor.subTask("Collecting types");
        for (INedTypeInfo nedTypeInfo : nedResources.getBuiltInDeclarations())
            typeElements.add(nedTypeInfo.getNedElement());
        for (IFile file : files) {
            if (nedResources.isNedFile(file))
                typeElements.addAll(nedResources.getNedFileElement(file).getTopLevelTypeNodes());
            else if (msgResources.isMsgFile(file))
                typeElements.addAll(msgResources.getMsgFileElement(file).getTopLevelTypeNodes());
        }
        Collections.sort(typeElements, (ITypeElement o1, ITypeElement o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        monitor.worked(1);
    }

    protected String getPackageName(INedTypeElement typeElement) {
        String packageName = typeElement.getNedTypeInfo().getPackageName();
        return StringUtils.defaultIfEmpty(packageName, "default");
    }

    protected String getNamespaceName(IMsgTypeElement typeElement) {
        String namespaceName = typeElement.getMsgTypeInfo().getNamespaceName();
        return StringUtils.defaultIfEmpty(namespaceName, "default");
    }

    protected String getFullyQualifiedName(ITypeElement typeElement) {
        if (typeElement instanceof INedTypeElement)
            return ((INedTypeElement)typeElement).getNedTypeInfo().getFullyQualifiedName();
        else if (typeElement instanceof IMsgTypeElement)
            return ((IMsgTypeElement)typeElement).getMsgTypeInfo().getFullyQualifiedCppClassName();
        else
            return typeElement.getName();  // return unqualified name by default
    }

    protected void collectPackageNames() {
        monitor.subTask("Collecting package names");
        Set<String> packageNames = new LinkedHashSet<String>();

        for (ITypeElement typeElement : typeElements)
            if (typeElement instanceof INedTypeElement)
                packageNames.add(getPackageName((INedTypeElement)typeElement));

        this.packageNames = (ArrayList<String>)CollectionUtils.toSorted(new ArrayList<String>(packageNames));
    }

    protected void collectNamespaceNames() {
        monitor.subTask("Collecting namespace names");
        Set<String> namespaceNames = new LinkedHashSet<String>();

        for (ITypeElement typeElement : typeElements)
            if (typeElement instanceof IMsgTypeElement)
                namespaceNames.add(getNamespaceName((IMsgTypeElement)typeElement));

        this.namespaceNames = (ArrayList<String>)CollectionUtils.toSorted(new ArrayList<String>(namespaceNames));
    }

    protected void collectTypeNames() {
        monitor.subTask("Collecting type names");
        for (ITypeElement typeElement : typeElements) {
            if (typeElement instanceof INedTypeElement) {
                // add simple and fully qualified NED names
                String name = typeElement.getName();
                String qname = ((INedTypeElement)typeElement).getNedTypeInfo().getFullyQualifiedName();
                addToTypeNamesMap(name, typeElement);
                if (!qname.equals(name))
                    addToTypeNamesMap(qname, typeElement);
            }

            if (typeElement instanceof IMsgTypeElement) {
                // add simple, fully and all partially qualified C++ names.
                // (we want inet::foo::Foo to be found as any of: inet::foo::Foo, foo::Foo, Foo)
                String qname = ((IMsgTypeElement)typeElement).getMsgTypeInfo().getFullyQualifiedCppClassName();
                addToTypeNamesMap("::" + qname, typeElement);
                while (qname.contains("::")) {
                    addToTypeNamesMap(qname, typeElement);
                    qname = StringUtils.substringAfter(qname, "::");
                }
                String name = typeElement.getName(); // should be same as qname remainder
                addToTypeNamesMap(name, typeElement);
            }
        }

        final String QCHAR = "[a-zA-Z\\u0080-\\uffff_0-9.:]"; // char that may occur in fully qualified NED or C++ type names
        final String NCHAR = "[a-zA-Z\\u0080-\\uffff_0-9]";   // char that may occur in NED or C++ simple type names

        if (!automaticHyperlinking) {
            // Tilde syntax: we match fully/partially/un qualified NED and C++ names prefixed with a tilde.
            // Include all preceding tildes into the match (because a double tilde means one literal tilde in the output)
            //
            // Note 1: We can't just join known type names with "|", because we want invalid references to be
            // marked as errors (colored red), not just be ignored.
            //
            // Note 2: This is a very plain regex, but we don't need a more sophisticated one. False positives
            // will be filtered out in the replacement phase.
            //
            possibleTypeReferencesPattern = Pattern.compile("(~+)(" + QCHAR + "*" + NCHAR + ")");

            // rudimentary test
            Assert.isTrue(possibleTypeReferencesPattern.matcher("~Foo").matches());
            Assert.isTrue(possibleTypeReferencesPattern.matcher("~foo.Bar").matches());
            Assert.isTrue(possibleTypeReferencesPattern.matcher("~foo::Bar").matches());
            Assert.isTrue(possibleTypeReferencesPattern.matcher("~::foo::Bar").matches());
            Assert.isTrue(possibleTypeReferencesPattern.matcher("~~~~Foo").matches());
            Assert.isTrue(!possibleTypeReferencesPattern.matcher("~").matches()); // do not match tildes not in front of candidates
            Assert.isTrue(!possibleTypeReferencesPattern.matcher("foo").matches());
        }
        else {
            // Autolinking: Match recognized names, optionally prefixed with a backslash (or more backslashes).
            // Include all preceding backslashes into the match (because a double backslash means one literal backslash in the output)
            String typeNamesPattern = StringUtils.join(typeNamesMap.keySet(), "|").replace(".", "\\.");

            // we need to match this too, in order to be able to turn remove the backslash from in front of unrecognized type names ("\NoSuchType" --> "NoSuchType"):
            final String ESCAPEDNAME = "(\\\\+" + QCHAR + "+)";

            // note: "(?<!" is a lookbehind assertion to sure "Bar" won't match "FooBar" ("\b" is not good because it won't work for "::Foo", ":" doesn't make a word boundary)
            possibleTypeReferencesPattern = Pattern.compile("(\\\\*)(?<!" + QCHAR + ")(" + typeNamesPattern + "|" + ESCAPEDNAME + ")\\b");
        }
        monitor.worked(1);
    }

    private void addToTypeNamesMap(String name, ITypeElement typeElement) {
        if (!typeNamesMap.containsKey(name))
            typeNamesMap.put(name, new ArrayList<ITypeElement>());
        typeNamesMap.get(name).add(typeElement);
    }

    protected void collectSubtypesMap() {
        monitor.subTask("Collecting subtypes");
        for (ITypeElement subtype : typeElements) {
            if (subtype instanceof IInterfaceTypeElement)
                for (INedTypeElement supertype : ((IInterfaceTypeElement)subtype).getNedTypeInfo().getLocalInterfaces())
                    addToListInMap(subtypesMap, supertype, subtype);
            else {
                ITypeElement supertype = subtype.getSuperType();
                if (supertype != null)
                    addToListInMap(subtypesMap, supertype, subtype);
            }
        }
        monitor.worked(1);
    }

    private void addToListInMap(Map<ITypeElement, ArrayList<ITypeElement>> map, ITypeElement key, ITypeElement itemToAdd) {
        ArrayList<ITypeElement> list = map.get(key);
        if (list == null)
            list = new ArrayList<ITypeElement>();
        list.add(itemToAdd);
        map.put(key, list);
    }

    private void addToListInMap(Map<INedTypeElement, ArrayList<INedTypeElement>> map, INedTypeElement key, INedTypeElement itemToAdd) {
        ArrayList<INedTypeElement> list = map.get(key);
        if (list == null)
            list = new ArrayList<INedTypeElement>();
        list.add(itemToAdd);
        map.put(key, list);
    }

    protected void collectImplementorsMap() {
        monitor.subTask("Collecting implementors");
        for (ITypeElement typeElement : typeElements) {
            if (typeElement instanceof INedTypeElement && !(typeElement instanceof IInterfaceTypeElement)) {
                INedTypeElement implementor = (INedTypeElement)typeElement;

                for (INedTypeElement interfaze : implementor.getNedTypeInfo().getLocalInterfaces())
                    addToListInMap(directImplementorsMap, interfaze, implementor);

                for (INedTypeElement interfaze : implementor.getNedTypeInfo().getInterfaces())
                    addToListInMap(allImplementorsMap, interfaze, implementor);
            }
        }

        monitor.worked(1);
    }

    protected void collectUsersMap() {
        monitor.subTask("Collecting uses");
        for (ITypeElement userType : typeElements)
            for (ITypeElement usedType : userType.getLocalUsedTypes())
                addToListInMap(usersMap, usedType, userType);
        monitor.worked(1);
    }

    protected void collectDoxyMap() throws Exception {
        File doxyTagsFile = getFullDoxyPath().append("doxytags.xml").toFile();

        if (doxyTagsFile.exists()) {
            FileInputStream stream = new FileInputStream(doxyTagsFile);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(stream);
            NodeList nodes = (NodeList)XPathFactory.newInstance().newXPath().compile("//compound[@kind='class']/filename")
                .evaluate(document, XPathConstants.NODESET);

            try {
                monitor.beginTask("Collecting doxygen...", nodes.getLength());

                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    String fileName = node.getTextContent();
                    Node nameNode = node.getParentNode().getFirstChild().getNextSibling();
                    Assert.isTrue(nameNode.getNodeName().equals("name"));
                    String className = nameNode.getTextContent();
                    doxyMap.put(className, fileName);
                    monitor.worked(1);
                }
            }
            finally {
                monitor.done();
            }
        }
    }

    protected void collectExtensions() {
        if (extensionFilePath != null)
            neddocExtensions = new NeddocExtensions(project.getFile(extensionFilePath));
    }

    private boolean isCppProject(IProject project) throws CoreException {
        return project.getNature("org.eclipse.cdt.core.ccnature") != null;
    }

    protected void generateDoxy() throws Exception {
        if (generateDoxy && isCppProject(project)) {
            if (doxyExecutablePath == null || !new File(doxyExecutablePath).exists())
                throw new IllegalStateException("The Doxygen executable path is invalid, set it using Window/Preferences...\nThe currently set path is: " + doxyExecutablePath);

            try {
                monitor.beginTask("Running doxygen...", IProgressMonitor.UNKNOWN);
                monitor.subTask("Waiting for doxygen to complete"); // needed, otherwise Eclipse progress dialog will show last subtask of previous task
                File doxyConfigFile = absoluteDoxyConfigFilePath.toFile();

                if (!doxyConfigFile.exists()) {
                    boolean[] ok = new boolean[1];
                    if (!headless) {
                        DisplayUtils.runNowOrSyncInUIThread(() -> {
                            ok[0] = MessageDialog.openQuestion(null, "Missing configuration file", "Doxygen configuration file does not exist: " + absoluteDoxyConfigFilePath + "\n\nDo you want to create a default one?");
                        });
                    } else {
                        ok[0] = true;
                    }
                    if (ok[0])
                        DocumentationGeneratorPropertyPage.generateDefaultDoxyConfigurationFile(project, doxyExecutablePath, absoluteDoxyConfigFilePath.toString());
                }

                if (doxyConfigFile.exists()) {
                    String content = FileUtils.readTextFile(doxyConfigFile, null);
                    // these options must be always overwritten
                    IPath fullPath = getFullDoxyPath().makeRelativeTo(new Path(project.getLocation().toString()));
                    content = DocumentationGeneratorPropertyPage.replaceDoxygenConfigurationEntry(content, "OUTPUT_DIRECTORY", fullPath.toString());
                    content = DocumentationGeneratorPropertyPage.replaceDoxygenConfigurationEntry(content, "HTML_HEADER", fullPath.append("../../doxy-header.tmpl").toString());
                    content = DocumentationGeneratorPropertyPage.replaceDoxygenConfigurationEntry(content, "GENERATE_TAGFILE", fullPath.append("doxytags.xml").toString());
                    content = DocumentationGeneratorPropertyPage.replaceDoxygenConfigurationEntry(content, "SOURCE_BROWSER", (generateCppSourceListings ? "YES" : "NO"));
                    File modifiedDoxyConfigFile = documentationRootPath.append("temp-doxy.cfg").toFile();
                    File headerTemplateFile = documentationRootPath.append("doxy-header.tmpl").toFile();

                    try {
                        String header = readTextFromResource("doxy-header.tmpl");
                        FileUtils.writeTextFile(headerTemplateFile, header, null);

                        FileUtils.writeTextFile(modifiedDoxyConfigFile, content, null);
                        ProcessUtils.exec(doxyExecutablePath, new String[] {modifiedDoxyConfigFile.toString()}, project.getLocation().toString(), monitor);
                    }
                    finally {
                        headerTemplateFile.delete();
                        modifiedDoxyConfigFile.delete();
                    }
                }
                else
                    throw new IllegalStateException("Doxygen configuration file not found: " + absoluteDoxyConfigFilePath);
            }
            finally {
                monitor.done();
            }
        }
    }

    protected String readTextFromResource(final String resourcePath) throws Exception {
        InputStream stream = getClass().getResourceAsStream("templates/" + renderer.getTemplateName() + "/" + resourcePath);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + resourcePath);
        return  new String(FileUtils.readBinaryFile(stream));
    }

    protected void generatePage(String pageName, String title, final String content) throws Exception {
        generatePage(pageName, title, () -> {
                out(content);
            }
        );
    }

    protected void generatePage(String pageName, String title, Runnable content) throws Exception {
        FileOutputStream oldCurrentOutputStream = currentOutputStream;

        File file = getOutputFile(renderer.appendFilenameExtension(pageName));
        currentOutputStream = new FileOutputStream(file);

        String splitPage[] = readTextFromResource("page.tmpl").replace("@title@", title).split("@content@");
        if (splitPage.length != 2)
            throw new RuntimeException("page.tmpl must contain exactly one @content@ placeholder ");

        out(splitPage[0]);
        content.run();

        if (APPLY_CC)
            out(renderer.copyrightNotice());

        out(splitPage[1]);

        currentOutputStream.close();
        currentOutputStream = oldCurrentOutputStream;
    }

    protected String processHTMLContent(String clazz, String comment) {
        Set<String> debugOptions = new HashSet<>();
        comment = processDebugOptions(comment, debugOptions);
        boolean debugLinks = debugOptions.contains("links");

        return NedCommentFormatter.makeHtmlDocu(comment, clazz.equals("briefcomment"), !automaticHyperlinking, new INeddocProcessor() {
            public String process(String comment) {
                return replaceTypeReferences(comment, debugLinks);
            }});
    }

    private static int countPrefixChars(String str) {
        int i = 0;
        while (i < str.length() && (str.charAt(i) == '~' || str.charAt(i) == '\\'))
            i++;
        return i;
    }

    protected String replaceTypeReferences(String comment, boolean debug) {
        // note: Chinese characters in the comment must be unescaped at this point (not as numeric entities),
        // otherwise we our regex won't find them and Chinese identifiers won't be hyperlinked
        return StringUtils.replaceMatches(comment, possibleTypeReferencesPattern, new IRegexpReplacementProvider() {
            public String getReplacement(Matcher matcher) {
                String match = matcher.group();
                int prefixLen = countPrefixChars(match);
                String prefix = match.substring(0, prefixLen);
                String identifier = match.substring(prefixLen);

                // This match may be a false positive, because the original regex is not specific enough.
                // To check, we try match it as NED type name and as C++ type name, and take the longer match.
                // Note that matching with the regex "<nedTypePatter>|<msgTypePattern>" won't work, because
                // "|" is not greedy enough and doesn't take the longer of the two alternatives but the first one.
                // Illustration: the regex "a|apple" will NEVER match "apple" only its first "a"!
                // This is why we need separate match operations and manually take the longer one.
                Matcher nedTypeMatcher = nedTypePattern.matcher(identifier);
                Matcher msgTypeMatcher = msgTypePattern.matcher(identifier);
                int nedTypeLen = nedTypeMatcher.lookingAt() ? nedTypeMatcher.end() : 0;
                int msgTypeLen = msgTypeMatcher.lookingAt() ? msgTypeMatcher.end() : 0;
                int len = Math.max(nedTypeLen, msgTypeLen); // the longer of the two will be the real one
                if (len == 0)
                    return null; // leave "as is"

                // If the match contains some trailing garbage (e.g. "::100"), it should be chopped off and added to "suffix"
                String suffix = "";
                if (len < identifier.length()) {
                    suffix = identifier.substring(len);
                    identifier = identifier.substring(0, len);
                }

                List<ITypeElement> typeElements = typeNamesMap.get(identifier);
                if (typeElements == null && identifier.contains("::")) {
                    // identifier might be an enum member, e.g. "State::IDLE" -- try again without the last segment
                    int pos = identifier.lastIndexOf("::");
                    typeElements = typeNamesMap.get(identifier.substring(0, pos));
                    // if successful, go with it
                    if (typeElements != null) {
                        suffix += identifier.substring(pos); // e.g. "::IDLE"
                        identifier = identifier.substring(0, pos);
                    }
                }

                // literal backslashes and tildes are doubled in the neddoc source when they are in front of an identifier
                boolean evenPrefixes = prefix.length() % 2 == 0;

                prefix = prefix.substring(0, prefix.length() / 2);

                if (debug) {
                    prefix = "[" + match + " ➜ " + prefix;
                    suffix = suffix + "]";
                }

                if ((!automaticHyperlinking && !evenPrefixes) || (automaticHyperlinking && evenPrefixes && typeElements != null)) {
                    if (typeElements == null) {
                        return prefix + renderer.styled(identifier, "error", null) + suffix;
                    }
                    else if (typeElements.size() == 1) {
                        ITypeElement typeElement = typeElements.get(0);
                        return prefix + typeReferenceString(typeElement) + suffix;
                    }
                    else {
                        // several types with the same simple name
                        String links = "";
                        int i = 1;
                        for (ITypeElement typeElement : typeElements)
                            links += typeReferenceString(typeElement, String.valueOf(i++)) + ",";
                        links = StringUtils.removeEnd(links, ",");
                        return prefix + typeElements.get(0).getName() + "(" + links + ")" + suffix;
                    }
                }
                else
                    return prefix + identifier + suffix;
            }
        });
    }

    protected void generateNavigationTree(String fileName, Runnable content) throws Exception {
        FileOutputStream oldCurrentOutputStream = currentOutputStream;

        File file = getOutputFile(fileName);
        currentOutputStream = new FileOutputStream(file);

        String splitNavigation[] = readTextFromResource("navtree.tmpl").split("@content@");
        if (splitNavigation.length != 2)
            throw new RuntimeException("navtree.tmpl must contain exactly one @content@ placeholder ");

        out(splitNavigation[0]);
        content.run();
        out(splitNavigation[1]);

        currentOutputStream.close();
        currentOutputStream = oldCurrentOutputStream;
    }

    protected void generateProjectIndexReference(IProject project) throws Exception {
        String projectName = project.getName();
        IPath indexPath = new Path("..").append(neddocRelativeRootPath).append(projectName).append(DocumentationGeneratorPropertyPage.getNeddocPath(project)).append(renderer.appendFilenameExtension("index"));
        generateNavigationMenuItem(1, projectName, indexPath.toPortableString(), null);
    }

    protected void generateProjectIndexReferences(String title, final IProject[] projects) throws Exception {
        if (projects.length != 0)
            generateNavigationMenuItem(1, title, null, () -> {
                    for (IProject project : projects)
                        if (project.isAccessible()) // generate only for accessible (existing, open) projects
                            generateProjectIndexReference(project);
                }
            );
    }

    protected void generateProjectIndex() throws Exception {
        generateProjectIndexReferences("Referenced Projects", project.getReferencedProjects());
        generateProjectIndexReferences("Referencing Projects", project.getReferencingProjects());
    }

    protected String packageReferenceString(String packageName) throws IOException {
        return "Package: " + renderer.link(packageName, renderer.appendFilenameExtension("packages") + "#" + packageName.replace('.', '_'), "reference-ned");
    }

    protected void generatePackageIndex() throws Exception {
        if (packageNames.size() != 0)
            generateNavigationMenuItem(1, "Packages", renderer.appendFilenameExtension("packages"), () -> {
                    for (String packageName : packageNames) {
                        generateTypeIndex(2, packageName, renderer.appendFilenameExtension("packages")+"#"+packageName.replace('.', '_'), (object) -> {
                            if (object instanceof INedTypeElement)
                                return getPackageName((INedTypeElement)object).equals(packageName);

                            return false;
                        });
                    }
                }
            );
    }

    protected void generateNamespaceIndex() throws Exception {
        if (packageNames.size() != 0)
            generateNavigationMenuItem(1, "Namespaces", renderer.appendFilenameExtension("namespaces"), () -> {
                    for (String namespaceName : namespaceNames) {
                        generateTypeIndex(2, namespaceName, renderer.appendFilenameExtension("namespaces")+"#"+namespaceName.replace("::", "_"), (object) -> {
                            if (object instanceof IMsgTypeElement)
                                return getNamespaceName((IMsgTypeElement)object).equals(namespaceName);

                            return false;
                        });
                    }
                }
            );
    }

    protected void generateFileIndex() throws Exception {
        if (files.size() != 0)
            generateNavigationMenuItem(1, "Files", null, () -> {
                    for (IFile file : files)
                        generateNavigationMenuItem(2, file.getProjectRelativePath().toPortableString(), renderer.appendFilenameExtension(getOutputBaseNameForFile(file)), null);
                }
            );
    }

    protected void generateNavTreeData() throws Exception {
        try {
            monitor.beginTask("Generating navigation tree...", IProgressMonitor.UNKNOWN);

            generateNavigationTree("navtreedata.js", () -> {
                    generateNavigationMenuItem(0, project.getName(), renderer.appendFilenameExtension("index"), () -> {

                        generateOverview();
                        generateSelectedTopics();

                        generateDiagrams();
                        generatePackageIndex();

                        generateTypeIndex(1, "Networks", null, object -> {
                                return object instanceof CompoundModuleElementEx && ((CompoundModuleElementEx)object).isNetwork();
                            });
                        generateTypeIndex(1, "Modules", null, object -> {
                            return object instanceof IModuleTypeElement && !((IModuleTypeElement)object).isNetwork();
                        });
                        generateTypeIndex(1, "Module Interfaces", ModuleInterfaceElementEx.class);
                        generateTypeIndex(1, "Channels", ChannelElementEx.class);
                        generateTypeIndex(1, "Channel Interfaces", ChannelInterfaceElementEx.class);
                        if (generateMsgDefinitions) {
                            generateNamespaceIndex();
                            generateMsgIndex();
                        }
                        if (generateFileListings)
                            generateFileIndex();
                        generateCppIndex();
                        generateProjectIndex();
                    });
                }
            );
        }
        finally {
            monitor.done();
        }
    }

    private void generateMsgIndex() throws Exception {
        generateNavigationMenuItem(1, "Message Definitions", null, () -> {
                generateTypeIndex(2, "Messages", MessageElementEx.class);
                generateTypeIndex(2, "Packets", PacketElementEx.class);
                generateTypeIndex(2, "Classes", ClassElementEx.class);
                generateTypeIndex(2, "Structs", StructElementEx.class);
                generateTypeIndex(2, "Enums", EnumElementEx.class);
        });
    }

    protected void generateNavTreeIndex() throws Exception {
        FileOutputStream oldCurrentOutputStream = currentOutputStream;

        File file = getOutputFile("navtreeindex0.js");
        currentOutputStream = new FileOutputStream(file);

        out("var NAVTREEINDEX0 = {\n");
        for (Map.Entry<String,ArrayList<Integer>> entry : navigationItemIndex.entrySet()) {
            out("'"+entry.getKey()+"':"+entry.getValue()+",\n");
        }
        out("};");

        currentOutputStream.close();
        currentOutputStream = oldCurrentOutputStream;
    }

    protected void generateNavigationMenuItem(int level, String title, String url, Runnable content) throws Exception {
        // reset the index for the current level if we are now deeper in the level hierarchy than last time
        if (previousLevel < level)
            levelIndex[level] = 0;

        // dump the menu item
        String indent = String.format("%1$"+(2+level*2)+"s", "");
        out("\n"+indent+"[`" + title + "`, " + (url==null ? "null" : "`"+url+"`") + ", ");
        if (content != null) {
            out("[");
            content.run();
            out("\n"+indent+"] ],");
        } else {
            out("null");
            out("],");
        }

        // add index entry with the current breadcrumb (if not already present)
        if (level >= 1 && url != null && !navigationItemIndex.containsKey(url)) {
            ArrayList<Integer> breadcrumb = new ArrayList<>();
            for (int i=1; i<=level; i++)
                breadcrumb.add(i-1, levelIndex[i]);
            navigationItemIndex.put(url, breadcrumb);
        }

        // increase the breadcrumb counters for the next item
        levelIndex[level]++;
        previousLevel = level;
    }

    protected void generateCppMenuItem(String title, String url) throws Exception {
        if (project.getFile(rootRelativeDoxyPath.append(url)).getLocation().toFile().exists())
            generateNavigationMenuItem(2, title, neddocRelativeRootPath.append(rootRelativeDoxyPath).append(url).toPortableString(), null);
    }

    private void generateCppIndex() throws Exception {
        if (isCppProject(project) && generateDoxy)
            generateNavigationMenuItem(1, "C++", null, () -> {
                    generateCppMenuItem("Main Page", renderer.appendFilenameExtension("main"));
                    generateCppMenuItem("File List", renderer.appendFilenameExtension("files"));
                    generateCppMenuItem("Class List", renderer.appendFilenameExtension("annotated"));
                    generateCppMenuItem("Class Hierarchy", renderer.appendFilenameExtension("hierarchy"));
                    generateCppMenuItem("Class Members", renderer.appendFilenameExtension("functions"));
                    generateCppMenuItem("Namespace List", renderer.appendFilenameExtension("namespaces"));
                    generateCppMenuItem("File Members", renderer.appendFilenameExtension("globals"));
                    generateCppMenuItem("Namespace Members", renderer.appendFilenameExtension("namespacemembers"));
                }
            );
    }

    protected void generateOverview() throws Exception {
        generateNavigationMenuItem(1, "Overview", renderer.appendFilenameExtension("index"), null);
        final boolean[] titlePageFound = new boolean[1];
        mapPageMatchers(new IPageMatcherVisitor() {
        public boolean visit(PageType pageType, String file, String title, String content) throws Exception {
            if (pageType == PageType.TITLE_PAGE && content.length() > 0) {
                    titlePageFound[0] = true;
                    generatePage("index", "Overview",
                        processHTMLContent("comment", content ));
                    return false;
                }
                else
                    return true;
            }
        });
        // generate default if @titlepage not found
        if (!titlePageFound[0])
            generatePage("index", "Overview", renderer.defaultOverviewString());
    }

    protected void generateSelectedTopics() throws Exception {
        final boolean[] pageFound = new boolean[1];
        // see if there's a @page at all
        mapPageMatchers(new IPageMatcherVisitor() {
            public boolean visit(PageType pageType, String file, String title, String content) throws Exception {
                if (pageType == PageType.PAGE || pageType == PageType.EXTERNAL_PAGE) {
                    pageFound[0] = true;
                    return false;
                }
                else
                    return true;
            }
        });
        if (pageFound[0]) {
            generateNavigationMenuItem(1, "Selected Topics", null, () -> {
                    mapPageMatchers(new IPageMatcherVisitor() {
                        public boolean visit(PageType pageType, String file, String title, String content) throws Exception {
                            if (pageType == PageType.PAGE) {
                                generateNavigationMenuItem(2, title, file, null);
                                generatePage(new Path(file).removeFileExtension().toPortableString(), title,
                                        renderer.sectionHeading(title, null) + processHTMLContent("comment", content));

                            }
                            else if (pageType == PageType.EXTERNAL_PAGE) {
                                generateNavigationMenuItem(2, title, file, null);
                            }
                            return true;
                        }
                    });
                }
            );
        }
    }

    protected enum PageType {
        PAGE,
        TITLE_PAGE,
        EXTERNAL_PAGE
    }

    protected static interface IPageMatcherVisitor {
        public boolean visit(PageType pageType, String file, String title, String content) throws Exception;
    }

    protected void mapPageMatchers(IPageMatcherVisitor visitor) throws Exception {
        for (IFile file : files) {
            String comment = null;

            if (nedResources.isNedFile(file))
                comment = getExpandedComment(nedResources.getNedFileElement(file));
            else if (msgResources.isMsgFile(file))
                comment = getExpandedComment(msgResources.getMsgFileElement(file));

            if (comment != null) {
                if (comment.contains("@page") || comment.contains("@titlepage") || comment.contains("@externalpage")) {

                    Matcher matcher = externalPagePattern.matcher(comment);
                    StringBuffer buffer = new StringBuffer();

                    while (matcher.find()) {
                        matcher.appendReplacement(buffer, "");
                        String fileName = matcher.group(1);
                        String title = StringUtils.isEmpty(matcher.group(2)) ? fileName : matcher.group(2);
                        if (!visitor.visit(PageType.EXTERNAL_PAGE, fileName, title, null))
                            return;
                    }

                    matcher.appendTail(buffer);
                    comment = buffer.toString();

                    List<String> pages = StringUtils.splitPreservingSeparators(comment, pageSeparatorPattern);

                    for (int i = 1; i < pages.size() - 1; i++) {
                        String header = pages.get(i);
                        String content = pages.get(i+1);

                        if (titlePagePattern.matcher(header).find()) {
                            if (!visitor.visit(PageType.TITLE_PAGE, null, null, content))
                                return;
                        }
                        else {
                            matcher = pagePattern.matcher(header);
                            if (matcher.find()) {
                                String fileName = matcher.group(1);
                                String title = StringUtils.isEmpty(matcher.group(2)) ? fileName : matcher.group(2);
                                fileName = sanitizeFileName(fileName, ".html");
                                if (!visitor.visit(PageType.PAGE, fileName, title, content))
                                    return;
                            }
                        }
                    }
                }
            }
        }
    }

    protected String sanitizeFileName(String fileName, String requiredSuffix) {
        // Replace characters that may cause trouble: control characters, characters that may
        // be invalid in a file name in some platform, may need quoting when put into a HTML
        // attribute (href), or may have different representations in different encodings
        // (i.e. non-ASCII chars). When changing this code, make sure that the page "Nasty-Filename"
        // in test/neddoc can be generated and also opened!
        fileName = fileName.trim().replaceAll("\\s+", "-").replace('[', '(').replace(']', ')');
        fileName = fileName.replaceAll("[^-A-Za-z0-9!@$(){};+=.]", "_");

        // add suffix
        if (!fileName.endsWith(requiredSuffix))
            fileName += requiredSuffix;
        return fileName;
    }

    protected void generateDiagrams() throws Exception {
        boolean found = false;
        for (ITypeElement typeElement : typeElements) {
            if (typeElement instanceof INedTypeElement || typeElement instanceof IMsgTypeElement) {
                found = true;
                break;
            }
        }

        if (found && (generateFullUsageDiagrams || generateFullInheritanceDiagrams)) {
            generateNavigationMenuItem(1, "Diagrams", null, () -> {
                    for (ITypeElement typeElement : typeElements) {
                        if (typeElement instanceof INedTypeElement) {
                            if (generateFullUsageDiagrams)
                                generateNavigationMenuItem(2, "Full NED Usage Diagram", "full-ned-usage-diagram.html", null);

                            if (generateFullInheritanceDiagrams)
                                generateNavigationMenuItem(2, "Full NED Inheritance Diagram", "full-ned-inheritance-diagram.html", null);

                            break;
                        }
                    }

                    for (ITypeElement typeElement : typeElements) {
                        if (typeElement instanceof IMsgTypeElement) {
                            if (generateFullUsageDiagrams)
                                generateNavigationMenuItem(2, "Full MSG Usage Diagram", "full-msg-usage-diagram.html", null);

                            if (generateFullInheritanceDiagrams)
                                generateNavigationMenuItem(2, "Full MSG Inheritance Diagram", "full-msg-inheritance-diagram.html", null);

                            break;
                        }
                    }
                }
            );
        }
    }

    protected void generatePageReference(String fileName, String title) throws Exception {
        if (title == null || title.equals(""))
            title = fileName;

        generateNavigationMenuItem(2, title, fileName, null);
    }

    protected void generateTypeIndexEntry(int level, ITypeElement typeElement) throws Exception {
        generateNavigationMenuItem(level, typeElement.getName(), renderer.appendFilenameExtension(getOutputBaseFileName(typeElement)), null);
    }

    protected void generateTypeIndex(int level, String title, final Class<?> clazz) throws Exception {
        generateTypeIndex(level, title, null, object -> {
                return clazz.isInstance(object);
            }
        );
    }

    protected void generateTypeIndex(int level, String title, String url, IPredicate predicate) throws Exception {
        final ArrayList<ITypeElement> selectedElements = new ArrayList<ITypeElement>();

        for (ITypeElement typeElement : typeElements)
            if (predicate.matches(typeElement))
                selectedElements.add(typeElement);

        if (selectedElements.size() != 0) {
            generateNavigationMenuItem(level, title, url, () -> {
                    for (ITypeElement typeElement : selectedElements)
                        generateTypeIndexEntry(level+1, typeElement);
                }
            );
        }
    }

    protected void generateNedTagFile() throws Exception {

        FileOutputStream oldCurrentOutputStream = currentOutputStream;
        currentOutputStream = new FileOutputStream(getOutputFile("nedtags.xml"));

        out("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n");
        out("<tagfile>\n");
        for (ITypeElement typeElement : typeElements)
            if (typeElement instanceof INedTypeElement){
                INedTypeElement e = (INedTypeElement)typeElement;
                out("  <compound kind=\"class\">\n");
                String pkgname = getPackageName(e);
                out("    <name>"+ (("default".equals(pkgname)) ? "" : pkgname+".") +e.getName()+"</name>\n");
                out("    <filename>"+renderer.appendFilenameExtension(getOutputBaseFileName(e))+"</filename>\n");
                out("  </compound>\n");
            }
        out("</tagfile>\n");

        currentOutputStream.close();
        currentOutputStream = oldCurrentOutputStream;
    }

    protected void generateMsgTagFile() throws Exception {

        FileOutputStream oldCurrentOutputStream = currentOutputStream;
        currentOutputStream = new FileOutputStream(getOutputFile("msgtags.xml"));

        out("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n");
        out("<tagfile>\n");
        for (ITypeElement typeElement : typeElements)
            if (typeElement instanceof IMsgTypeElement){
                out("  <compound kind=\"class\">\n");
                out("    <name>"+ typeElement.getName()+"</name>\n");
                out("    <filename>"+renderer.appendFilenameExtension(getOutputBaseFileName(typeElement))+"</filename>\n");
                out("  </compound>\n");
            }
        out("</tagfile>\n");

        currentOutputStream.close();
        currentOutputStream = oldCurrentOutputStream;
    }

    protected void generatePackagesPage() throws Exception {
        generatePage("packages", "Packages", () -> {
            out(renderer.sectionHeading("Packages", "comptitle"));
            for (final String packageName : packageNames) {
                        out(renderer.anchor(packageName.replace('.', '_')));
                        out(renderer.sectionHeading(packageName, "subtitle"));
                        generateExtensionFragment(ExtType.NED, packageName, "top");

                        out(renderer.beginTable("typestable"));
                        generateTypesTableHead();

                        for (ITypeElement typeElement : typeElements)
                            if (typeElement instanceof INedTypeElement)
                                if (packageName.equals(getPackageName((INedTypeElement)typeElement)))
                                    generateTypeReferenceLine(typeElement);

                        out(renderer.endTable());
                        generateExtensionFragment(ExtType.NED, packageName, "bottom");
                    }
            }
        );
    }

    protected void generateNamespacesPage() throws Exception {
        generatePage("namespaces", "Namespaces", () -> {
            out(renderer.sectionHeading("Namespaces", "comptitle"));
            for (final String namespaceName : namespaceNames) {
                        out(renderer.anchor(namespaceName.replace("::", "_")));
                        out(renderer.sectionHeading(namespaceName, "subtitle"));
                        generateExtensionFragment(ExtType.MSG, namespaceName, "top");

                        out(renderer.beginTable("typestable"));
                        generateTypesTableHead();

                        for (ITypeElement typeElement : typeElements)
                            if (typeElement instanceof IMsgTypeElement)
                                if (namespaceName.equals(getNamespaceName((IMsgTypeElement)typeElement)))
                                    generateTypeReferenceLine(typeElement);

                        out(renderer.endTable());
                        generateExtensionFragment(ExtType.MSG, namespaceName, "bottom");
                    }
            }
        );
    }

    protected void generateFilePages() throws Exception {
        try {
            monitor.beginTask("Generating file pages...", files.size());

            for (final IFile file : files) {
                generatePage(getOutputBaseNameForFile(file), file.getName(), () -> {
                        monitor.subTask(file.getFullPath().toString());
                        String fileType = nedResources.isNedFile(file) ? "NED" : msgResources.isMsgFile(file) ? "Msg" : "";
                        String fragmentKey = file.getProjectRelativePath().toString();

                        out(renderer.sectionHeading(fileType + " File " + file.getProjectRelativePath(), "comptitle"));
                        generateExtensionFragment(ExtType.FILE, fragmentKey, "top");

                        INedFileElement fileElement = msgResources.isMsgFile(file) ?
                                msgResources.getMsgFileElement(file) : nedResources.getNedFileElement(file);
                        List<? extends ITypeElement> typeElements = fileElement.getTopLevelTypeNodes();

                        if (!typeElements.isEmpty()) {
                            out(renderer.beginTable("typestable"));
                            generateTypesTableHead();

                            for (ITypeElement typeElement : typeElements)
                                generateTypeReferenceLine(typeElement);

                            out(renderer.endTable());
                        }
                        generateExtensionFragment(ExtType.FILE, fragmentKey, "after-types");

                        generateSourceContent(file);
                        generateExtensionFragment(ExtType.FILE, fragmentKey, "bottom");
                        monitor.worked(1);
                    }
                );
            }
        }
        finally {
            monitor.done();
        }
    }

    protected void generateTypePages() throws Exception {
        try {
            monitor.beginTask("Generating NED type pages...", typeElements.size());

            for (ITypeElement typeElement : typeElements) {
                if (verboseMode)
                    System.out.append('.').flush();
                generateTypePage(typeElement);
            }
        }
        finally {
            monitor.done();
        }
    }

    protected void generateTypePage(ITypeElement typeElement) throws Exception {
        generatePage(getOutputBaseFileName(typeElement), typeElement.getName(), () -> {
                String fragmentKey = getFullyQualifiedName(typeElement);

                boolean isNedTypeElement = typeElement instanceof INedTypeElement;
                boolean isMsgTypeElement = typeElement instanceof IMsgTypeElement;

                if (isNedTypeElement) generateExtensionFragment(ExtType.NED, fragmentKey, "top");
                if (isMsgTypeElement) generateExtensionFragment(ExtType.MSG, fragmentKey, "top");

                if (isNedTypeElement)
                    out(renderer.paragraph(packageReferenceString(getPackageName((INedTypeElement)typeElement))));

                if (isMsgTypeElement) {
                    String namespaceName = getNamespaceName((IMsgTypeElement)typeElement);
                    out(renderer.paragraph("Namespace " + renderer.code(namespaceName, null)));
                }

                out(renderer.typeSectionHeading(typeElement));

                if (typeElement instanceof SimpleModuleElementEx || typeElement instanceof IMsgTypeElement)
                    generateNedTypeCppDefinitionReference(typeElement);

                String comment = getExpandedComment(typeElement);
                if (comment == null)
                    out(renderer.paragraph("(no description)"));
                else
                    out(processHTMLContent("comment", comment));
                if (isNedTypeElement) generateExtensionFragment(ExtType.NED, fragmentKey, "after-description");
                if (isMsgTypeElement) generateExtensionFragment(ExtType.MSG, fragmentKey, "after-description");

                if (isNedTypeElement) {
                    INedTypeElement nedTypeElement = (INedTypeElement)typeElement;
                    monitor.subTask(nedTypeElement.getReadableTagName() + ": " + nedTypeElement.getNedTypeInfo().getFullyQualifiedName());

                    generateTypeDiagram(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-image");

                    generateUsageDiagram(nedTypeElement);
                    generateInheritanceDiagram(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-diagrams");

                    if (typeElement instanceof IInterfaceTypeElement)
                        generateImplementorsTable(nedTypeElement);
                    generateUsedInTables(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-usage");

                    generateKnownSubtypesTable(nedTypeElement);
                    generateExtendsTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-inheritance");

                    generateParametersTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-parameters");

                    generatePropertiesTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-properties");

                    if (typeElement instanceof IModuleTypeElement)
                        generateGatesTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-gates");

                    generateSignalsTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-signals");

                    generateStatisticsTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-statistics");

                    if (typeElement instanceof CompoundModuleElementEx)
                        generateUnassignedParametersTable(nedTypeElement);
                    generateExtensionFragment(ExtType.NED, fragmentKey, "after-unassigned-parameters");
                }
                else if (isMsgTypeElement) {
                    IMsgTypeElement msgTypeElement = (IMsgTypeElement)typeElement;
                    monitor.subTask(msgTypeElement.getReadableTagName() + ": " + msgTypeElement.getName());

                    generateUsageDiagram(msgTypeElement);
                    generateInheritanceDiagram(msgTypeElement);
                    generateExtensionFragment(ExtType.MSG, fragmentKey, "after-diagrams");

                    generateExtendsTable(msgTypeElement);
                    generateKnownSubtypesTable(msgTypeElement);
                    generateExtensionFragment(ExtType.MSG, fragmentKey, "after-inheritance");

                    generateFieldsTable(msgTypeElement);
                    generateExtensionFragment(ExtType.MSG, fragmentKey, "after-fields");

                    generatePropertiesTable(msgTypeElement);
                    generateExtensionFragment(ExtType.MSG, fragmentKey, "after-properties");
                }

                if (generateSourceListings) {
                    generateSourceContent(typeElement);

                    if (typeElement instanceof INedTypeElement && nedResources.isBuiltInDeclaration(((INedTypeElement)typeElement).getNedTypeInfo()))
                        generateBuiltinTypeReference();
                    else
                        generateFileReference(getNedOrMsgFile(typeElement));
                }
                if (isNedTypeElement) generateExtensionFragment(ExtType.NED, fragmentKey, "bottom");
                if (isMsgTypeElement) generateExtensionFragment(ExtType.MSG, fragmentKey, "bottom");

                monitor.worked(1);
            }
        );
    }

    protected void generateExtensionFragment(ExtType extensionType, String key, String anchor) throws IOException {
        if (neddocExtensions != null) {
            String fragment = neddocExtensions.getFragment(extensionType, key, anchor);
            if (fragment != null)
                out(processHTMLContent("fragment", fragment));
        }
    }

    protected void generateFieldsTable(IMsgTypeElement msgTypeElement) throws IOException {
        Map<String, FieldElement> fields = msgTypeElement.getMsgTypeInfo().getFields();
        Map<String, FieldElement> localFields = msgTypeElement.getMsgTypeInfo().getLocalFields();
        if (fields.size() != 0) {
            out(renderer.subsectionHeading("Fields", "subtitle") +
                renderer.beginTable("fieldstable") +
                renderer.tableHeading("Name","name", "Type","type", "Description","description"));

            for (String name : fields.keySet())
            {
                FieldElement field = fields.get(name);
                String dataType = field.getDataType();
                IMsgTypeElement fieldTypeElement = msgResources.lookupMsgType(dataType);

                String trClass = localFields.containsKey(name) ? "local" : "inherited";
                String type = (fieldTypeElement != null) ? typeReferenceString(fieldTypeElement) : dataType;
                if (field.getIsVector())
                    type += "[" + field.getVectorSize() + "]"; // note: no whitespace between type name and "["

                out(renderer.tableRow(trClass, name, type, tableCommentString(getExpandedComment(field))));
            }
            out(renderer.endTable());
        }
    }

    protected void generatePropertiesTable(ITypeElement typeElement) throws IOException {
        Map<String, Map<String, PropertyElementEx>> properties = typeElement.getProperties();

        if (properties.size() != 0) {
            out(renderer.subsectionHeading("Properties", "subtitle")+
                renderer.beginTable("propertiestable") +
                renderer.tableHeading("Name","name", "Value","value", "Description","description"));

            for (String name : properties.keySet())
            {
                PropertyElementEx property = properties.get(name).get(PropertyElementEx.DEFAULT_PROPERTY_INDEX);

                if (property != null)
                    out(renderer.tableRow(null, name, propertyLiteralValuesString(property), tableCommentString(getExpandedComment(property))));
            }

            out(renderer.endTable());
        }
    }

    protected String propertyLiteralValuesString(PropertyElementEx property) {
        String result = "";
        for (PropertyKeyElement key = property.getFirstPropertyKeyChild(); key != null; key = key.getNextPropertyKeySibling()) {
            result += key.getFirstLiteralChild().getValue();

            if (key.getNextPropertyKeySibling() != null)
                result += ",";
        }
        return result;
    }

    protected void generateUsedInTables(INedTypeElement typeElement) throws IOException {
        if (usersMap.containsKey(typeElement)) {
            ArrayList<CompoundModuleElementEx> compoundModules = new ArrayList<CompoundModuleElementEx>();
            ArrayList<CompoundModuleElementEx> networks = new ArrayList<CompoundModuleElementEx>();

            for (ITypeElement userElement : usersMap.get(typeElement)) {
                if (userElement instanceof CompoundModuleElementEx) {
                    CompoundModuleElementEx compoundModule = (CompoundModuleElementEx)userElement;

                    if (compoundModule.isNetwork())
                        networks.add(compoundModule);
                    else
                        compoundModules.add(compoundModule);
                }
            }

            if (compoundModules.size() != 0) {
                out(renderer.subsectionHeading("Used in compound modules", "subtitle"));

                out(renderer.beginTable("typestable"));
                generateTypesTableHead();

                for (INedTypeElement userElement : compoundModules)
                    generateTypeReferenceLine(userElement);

                out(renderer.endTable());
            }

            if (networks.size() != 0) {
                out(renderer.subsectionHeading("Used in", "subtitle"));

                out(renderer.beginTable("typestable"));
                generateTypesTableHead();

                for (INedTypeElement userElement : networks)
                    generateTypeReferenceLine(userElement);

                out(renderer.endTable());
            }
        }
    }

    protected void generateParametersTable(INedTypeElement typeElement) throws IOException {
        Map<String, ParamElementEx> localParamsDeclarations = typeElement.getNedTypeInfo().getLocalParamDeclarations();
        Map<String, ParamElementEx> paramsDeclarations = typeElement.getParamDeclarations();
        Map<String, ParamElementEx> paramsAssignments = typeElement.getParamAssignments();

        if (!paramsDeclarations.isEmpty()) {
            out(renderer.subsectionHeading("Parameters", "subtitle")+
                renderer.beginTable("paramstable") +
                renderer.tableHeading("Name","name", "Type","type", "Default value","defaultvalue", "Description","description"));

            for (String name : paramsDeclarations.keySet()) {
                ParamElementEx paramDeclaration = paramsDeclarations.get(name);
                ParamElementEx paramAssignment = paramsAssignments.get(name);
                String trClass = localParamsDeclarations.containsKey(name) ? "local" : "inherited";

                out(renderer.tableRow(trClass,
                        name,
                        getParamTypeAsString(paramDeclaration),
                        (paramAssignment == null ? "" : paramAssignment.getValue()),
                        tableCommentString(getExpandedComment(paramDeclaration))));
            }

            out(renderer.endTable());
        }
    }

    protected void generateSignalsTable(INedTypeElement typeElement) throws IOException {
        Map<String, PropertyElementEx> propertyMap = typeElement.getProperties().get("signal");
        if (propertyMap != null) {
            out(renderer.subsectionHeading("Signals", "subtitle")+
                renderer.beginTable("signalstable") +
                renderer.tableHeading("Name","name", "Type","type", "Unit","unit"));
            for (String name : propertyMap.keySet()) {
                PropertyElementEx propertyElement = propertyMap.get(name);
                String cppType = getPropertyElementValue(propertyElement, "type");
                out(renderer.tableRow("local",
                        name,
                        cppReferenceString(cppType, cppType),
                        getPropertyElementValue(propertyElement, "unit")));
            }
            out(renderer.endTable());
        }
    }

    protected void generateStatisticsTable(INedTypeElement typeElement) throws IOException {
        Map<String, PropertyElementEx> propertyMap = typeElement.getProperties().get("statistic");
        if (propertyMap != null) {
            out(renderer.subsectionHeading("Statistics", "subtitle")+
                renderer.beginTable("statisticstable") +
                renderer.tableHeading("Name","name", "Title","title", "Source","source",
                            "Record","record", "Unit","unit", "Interpolation Mode","interpolationmode"));

            for (String name : propertyMap.keySet()) {
                PropertyElementEx propertyElement = propertyMap.get(name);
                out(renderer.tableRow("local", name,
                        getPropertyElementValue(propertyElement, "title"),
                        getPropertyElementValue(propertyElement, "source"),
                        getPropertyElementValue(propertyElement, "record"),
                        getPropertyElementValue(propertyElement, "unit"),
                        getPropertyElementValue(propertyElement, "interpolationmode")));

            }
            out(renderer.endTable());
        }
    }

    protected void generateUnassignedParametersTable(INedTypeElement typeElement) throws IOException {
        ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();
        collectUnassignedParameters(null, typeElement.getNedTypeInfo().getSubmodules(), params);

        if (params.size() != 0) {
            out(renderer.subsectionHeading("Unassigned submodule parameters", "subtitle")+
                renderer.beginTable("deepparamstable") +
                renderer.tableHeading("Name","name", "Type","type", "Default value","defaultvalue", "Description","description"));

            for (ArrayList<Object> tuple : params) {
                ParamElementEx paramDeclaration = (ParamElementEx)tuple.get(1);
                ParamElementEx paramAssignment = (ParamElementEx)tuple.get(2);

                out(renderer.tableRow(null, (String)tuple.get(0),
                        getParamTypeAsString(paramDeclaration),
                        (paramAssignment == null ? "" : paramAssignment.getValue()),
                        tableCommentString(getExpandedComment(paramDeclaration))));
            }
            out(renderer.endTable());
        }
    }

    protected void collectUnassignedParameters(String prefix, Map<String, SubmoduleElementEx> typeElementMap, ArrayList<ArrayList<Object>> params) throws IOException {
        for (SubmoduleElementEx submodule : typeElementMap.values()) {
            INedTypeElement typeElement = submodule.getEffectiveTypeRef();

            if (typeElement != null) {
                String newPrefix = (prefix == null ? "" : prefix + ".") + typeReferenceString(typeElement, submodule.getName());

                if (typeElement instanceof CompoundModuleElementEx)
                    collectUnassignedParameters(newPrefix, typeElement.getNedTypeInfo().getSubmodules(), params);
                else {
                    INedTypeInfo typeInfo = typeElement.getNedTypeInfo();
                    Map<String, ParamElementEx> declarations = typeInfo.getParamDeclarations();
                    Map<String, ParamElementEx> assigments = submodule.getParamAssignments();

                    for (String name : declarations.keySet()) {
                        ParamElementEx paramDeclaration = declarations.get(name);
                        ParamElementEx paramAssignment = assigments.get(name);

                        if (paramAssignment == null || paramAssignment.getIsDefault()) {
                            ArrayList<Object> tuple = new ArrayList<Object>();
                            tuple.add(newPrefix + "." + name);
                            tuple.add(paramDeclaration);
                            tuple.add(paramAssignment);
                            params.add(tuple);
                        }
                    }
                }
            }
        }
    }

    protected void generateGatesTable(INedTypeElement typeElement) throws IOException {
        IModuleTypeElement module = (IModuleTypeElement)typeElement;

        Map<String, GateElementEx> localGateDeclarations = module.getNedTypeInfo().getLocalGateDeclarations();
        Map<String, GateElementEx> gateDeclarations = module.getGateDeclarations();
        Map<String, GateElementEx> gatesSizes = module.getGateSizes();

        if (!gateDeclarations.isEmpty()) {
            out(renderer.subsectionHeading("Gates", "subtitle")+
                renderer.beginTable("gatestable") +
                renderer.tableHeading("Name","name", "Direction","type", "Size","gatesize", "Description","description"));

            for (String name : gateDeclarations.keySet()) {
                GateElementEx gateDeclaration = gateDeclarations.get(name);
                GateElementEx gateSize = gatesSizes.get(name);
                String trClass = localGateDeclarations.containsKey(name) ? "local" : "inherited";

                out(renderer.tableRow(trClass,
                        name + (gateDeclaration.getIsVector() ? " [ ]" : ""),
                        gateDeclaration.getAttribute(GateElementEx.ATT_TYPE),
                        (gateSize != null && gateSize.getIsVector() ? gateSize.getVectorSize() : ""),
                        tableCommentString(getExpandedComment(gateDeclaration))));
            }

            out(renderer.endTable());
        }
    }

    protected String tableCommentString(String comment) {
        return (comment != null) ? processHTMLContent("comment", comment) : "";
    }

    protected void generateTypesTableHead() throws IOException {
        out(renderer.tableHeading("Name","name", "Type","type", "Description","description"));
    }

    protected void generateTypeReferenceLine(ITypeElement typeElement) throws IOException {
        out(renderer.tableRow(null,
                typeReferenceString(typeElement),
                typeTypeString(typeElement),
                typeCommentString(typeElement)));
    }

    protected String typeReferenceString(ITypeElement typeElement) {
        return typeReferenceString(typeElement, typeElement.getName());
    }

    protected String typeReferenceString(ITypeElement typeElement, String label) {
        // note: label might also be "1", "2", "3",etc (for ambiguous type references), not only name/qname
        String qname = getFullyQualifiedName(typeElement);
        String tooltip = label.equals(qname) ? null : qname;
        return renderer.link(label, renderer.appendFilenameExtension(getOutputBaseFileName(typeElement)), null, tooltip);
    }

    protected String typeTypeString(ITypeElement typeElement) {
        return renderer.italic(typeElement.getReadableTagName().replaceAll(" ", "&nbsp;"));
    }

    protected String typeCommentString(ITypeElement typeElement) throws IOException {
        String comment = getExpandedComment(typeElement);
        if (comment != null)
            return processHTMLContent("briefcomment", comment);
        else
            return renderer.italic("(no description)");
    }

    protected void generateUnresolvedTypeReferenceLine(String name) throws IOException {
        out(renderer.tableRow(null,
                name, renderer.italic("(unknown -- not in documented files)"),""));
    }

    protected void generateNedTypeCppDefinitionReference(ITypeElement typeElement) throws IOException {
        String cppClassName;

        if (typeElement instanceof INedTypeElement)
            cppClassName = ((INedTypeElement)typeElement).getNedTypeInfo().getFullyQualifiedCppClassName();
        else if (typeElement instanceof IMsgTypeElement)
            cppClassName = ((IMsgTypeElement)typeElement).getMsgTypeInfo().getFullyQualifiedCppClassName();
        else
            throw new RuntimeException("Unknown type element: " + typeElement);

        if (doxyMap.containsKey(cppClassName))
            out(renderer.paragraph(cppReferenceString(cppClassName, "C++ definition")));
    }

    protected String cppReferenceString(String cppClassName, String label) throws IOException {
        if (doxyMap.containsKey(cppClassName))
            return renderer.link(label, getRelativePath(rootRelativeNeddocPath, rootRelativeDoxyPath).append(doxyMap.get(cppClassName)).toString(), "reference-cpp");
        else
            return label;
    }

    protected void generateBuiltinTypeReference() throws IOException {
        out("File: Built-in type");
    }

    protected void generateFileReference(IFile file) throws IOException {
        if (generateFileListings)
            out("File: " + renderer.link(file.getProjectRelativePath().toString(), renderer.appendFilenameExtension(getOutputBaseNameForFile(file)), null));
        else
            out("File: " + file.getProjectRelativePath());
    }

    protected void generateKnownSubtypesTable(ITypeElement typeElement) throws IOException {
        if (subtypesMap.containsKey(typeElement)) {
            out(renderer.subsectionHeading("Known subclasses", "subtitle"));

            out(renderer.beginTable("typestable"));
            generateTypesTableHead();

            for (ITypeElement subtype : subtypesMap.get(typeElement))
                generateTypeReferenceLine(subtype);

            out(renderer.endTable());
        }
    }

    protected void generateImplementorsTable(ITypeElement typeElement) throws IOException {
        if (allImplementorsMap.containsKey(typeElement)) {
            out(renderer.subsectionHeading("Implemented by", "subtitle"));

            out(renderer.beginTable("implementorstable"));
            generateTypesTableHead();

            for (ITypeElement subtype : allImplementorsMap.get(typeElement))
                generateTypeReferenceLine(subtype);

            out(renderer.endTable());
        }
    }

    protected void generateExtendsTable(ITypeElement typeElement) throws IOException {
        if (typeElement.getFirstExtends() != null) {
            out(renderer.subsectionHeading("Extends", "subtitle"));

            out(renderer.beginTable("typestable"));
            generateTypesTableHead();

            if (typeElement instanceof IInterfaceTypeElement)
                for (INedTypeElement supertype : ((IInterfaceTypeElement)typeElement).getNedTypeInfo().getLocalInterfaces())
                    generateTypeReferenceLine(supertype);
            else {
                ITypeElement supertype = typeElement.getSuperType();

                if (supertype != null)
                    generateTypeReferenceLine(supertype);
                else
                    generateUnresolvedTypeReferenceLine(typeElement.getFirstExtends());
            }

            out(renderer.endTable());
        }
    }

    protected void generateNedTypeFigures() throws InterruptedException, CoreException {
        if (generateNedTypeFigures) {
            ArrayList<IFile> nedFiles = new ArrayList<IFile>(nedResources.getNedFiles(project));

            final ExportDiagramFilesOperation exportOperation =
                new ExportDiagramFilesOperation(nedFiles,
                    new SVGDiagramExporter() {
                        @Override
                        public String getName() {
                            return "NED Figure Provider";
                        }

                        @Override
                        public String getDescription() {
                            return "NED Figure Provider";
                        }
                }, true, null) {

                @Override
                protected void refreshContainer(IContainer container, IProgressMonitor monitor) {
                    if (monitor.isCanceled())
                        throw new CancellationException();
                }
            };
            exportOperation.setOverwriteMode(ExportDiagramFilesOperation.OverwriteMode.ALL);
            exportOperation.run(monitor);

            try {
                // KLUDGE: move generated images under neddocPath
                monitor.beginTask("Moving type diagrams...", nedFiles.size());

                for (IFile file : nedFiles) {
                    if (verboseMode)
                        System.out.append('.').flush();
                    monitor.subTask(file.getFullPath().toString());

                    List<INedTypeElement> typeElements = nedResources.getNedFileElement(file).getTopLevelTypeNodes();

                    for (INedTypeElement typeElement : typeElements) {
                        String fileName = file.getName().replaceAll(".ned", "");
                        String imageName = NedFigureProvider.getFigureName(typeElements, typeElement, fileName);
                        File sourceImageFile = file.getParent().getFile(new Path(imageName + ".svg")).getLocation().toFile();

                        if (sourceImageFile.exists()) {
                            IPath destinationImagePath = getFullNeddocPath().append(getOutputFileName(typeElement, "type", ".svg"));
                            sourceImageFile.renameTo(destinationImagePath.toFile());
                        }
                        else
                            throw new RuntimeException("Cannot generate image for " + typeElement.getNedTypeInfo().getFullyQualifiedName());
                    }

                    monitor.worked(1);
                }
            }
            finally {
                monitor.done();
            }
        }
    }

    protected void generateTypeDiagram(final INedTypeElement typeElement) throws IOException {
        if (generateNedTypeFigures && !nedResources.isBuiltInDeclaration(typeElement.getNedTypeInfo())) {
            out(renderer.typeImageTag(getOutputFileName(typeElement, "type", ".svg")));
            DisplayUtils.runNowOrSyncInUIThread(() -> {
                    try {
                        NedFileElementEx modelRoot = typeElement.getContainingNedFileElement();
                        ScrollingGraphicalViewer viewer = NedFigureProvider.createNedViewer(modelRoot);
                        NedEditPart editPart = (NedEditPart)viewer.getEditPartRegistry().get(typeElement);

                        out(renderer.typeImageMap());

                        if (editPart instanceof CompoundModuleEditPart) {
                            CompoundModuleEditPart compoundModuleEditPart = (CompoundModuleEditPart)editPart;

                            for (Object child : compoundModuleEditPart.getChildren()) {
                                if (child instanceof SubmoduleEditPart) {
                                    SubmoduleEditPart submoduleEditPart = (SubmoduleEditPart)child;
                                    SubmoduleElementEx submoduleElement = submoduleEditPart.getModel();
                                    outMapReference(submoduleElement.getEffectiveTypeRef(), submoduleEditPart.getFigure());
                                }
                            }
                        }
                        else if (editPart instanceof NedTypeEditPart) {
                            NedTypeEditPart nedTypeEditPart = (NedTypeEditPart)editPart;
                            outMapReference(nedTypeEditPart.getModel(), nedTypeEditPart.getFigure());
                        }

                        out(renderer.endTypeImageMap());
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }
    }

    protected void generateFullDiagrams() throws Exception {
        try {
            monitor.beginTask("Generating full diagrams...", (generateFullUsageDiagrams ? 2 : 0) + (generateFullInheritanceDiagrams ? 2 : 0));
            final ArrayList<INedTypeElement> nedTypeElements = new ArrayList<INedTypeElement>();
            final ArrayList<IMsgTypeElement> msgTypeElements = new ArrayList<IMsgTypeElement>();

            for (ITypeElement typeElement : this.typeElements) {
                if (typeElement instanceof INedTypeElement)
                    nedTypeElements.add((INedTypeElement)typeElement);
                else if (typeElement instanceof IMsgTypeElement)
                    msgTypeElements.add((IMsgTypeElement)typeElement);
            }

            if (generateFullUsageDiagrams) {
                generatePage("full-ned-usage-diagram", "Full NED Usage Diagram", () -> {
                        out(renderer.sectionHeading("Full NED Usage Diagram", "comptitle")+
                            renderer.paragraph("The following diagram shows usage relationships between simple and compound modules, module interfaces, networks, channels and channel interfaces.\n" +
                            "Unresolved types are missing from the diagram."));
                        generateUsageDiagram(nedTypeElements, "full-ned-usage-diagram.svg");
                    }
                );
                monitor.worked(1);
            }

            if (generateFullInheritanceDiagrams) {
                generatePage("full-ned-inheritance-diagram", "Full NED Inheritance Diagram", () -> {
                        out(renderer.sectionHeading("Full NED Inheritance Diagram", "comptitle")+
                            renderer.paragraph("The following diagram shows the inheritance hierarchy between simple and compound modules, module interfaces, networks, channels and channel interfaces.\n" +
                            "Unresolved types are missing from the diagram."));
                        generateInheritanceDiagram(nedTypeElements, "full-ned-inheritance-diagram.svg");
                    }
                );
                monitor.worked(1);
            }

            if (generateFullUsageDiagrams) {
                generatePage("full-msg-usage-diagram", "Full MSG Usage Diagram", () -> {
                        out(renderer.sectionHeading("Full MSG Usage Diagram", "comptitle")+
                            renderer.paragraph("The following diagram shows usage relationships between messages, packets, classes and structs.\n" +
                            "Unresolved types are missing from the diagram."));
                        generateUsageDiagram(msgTypeElements, "full-msg-usage-diagram.svg");
                    }
                );
                monitor.worked(1);
            }

            if (generateFullInheritanceDiagrams) {
                generatePage("full-msg-inheritance-diagram", "Full MSG Inheritance Diagram", () -> {
                        out(renderer.sectionHeading("Full MSG Inheritance Diagram", "comptitle")+
                            renderer.paragraph("The following diagram shows the inheritance hierarchy between messages, packets, classes and structs.\n" +
                            "Unresolved types are missing from the diagram."));
                        generateInheritanceDiagram(msgTypeElements, "full-msg-inheritance-diagram.svg");
                    }
                );
                monitor.worked(1);
            }
        }
        finally {
            monitor.done();
        }
    }

    protected void generateUsageDiagram(ITypeElement typeElement) throws IOException {
        if (generatePerTypeUsageDiagrams && typeElement.getLocalUsedTypes().size()>0) {
            ArrayList<ITypeElement> typeElements = new ArrayList<ITypeElement>();
            typeElements.add(typeElement);

            String diagramType = typeElement instanceof INedTypeElement ? "ned" : "msg";
            String fullRef = !generateFullUsageDiagrams ?
                    "" : " Click " + renderer.link("here", renderer.appendFilenameExtension("full-"+diagramType+"-usage-diagram"), null) + " to see the full picture.";

            out(renderer.subsectionHeading("Usage diagram", "subtitle")+
                renderer.paragraph("The following diagram shows usage relationships between types.\n" +
                "Unresolved types are missing from the diagram."+fullRef));

            generateUsageDiagram(typeElements, getOutputFileName(typeElement, "usage", ".svg"));
        }
    }

    protected void generateUsageDiagram(List<? extends ITypeElement> typeElements, String imageFileName) throws IOException {
        if (generatePerTypeUsageDiagrams || generateFullUsageDiagrams) {
            DotGraph dot = new DotGraph();

            dot.append("digraph opp {\n" +
                       "   graph [bgcolor=\"#ffffff00\"] " +
                       "   node [fontsize=10,fontname=Helvetica,shape=box,height=.25,style=filled];\n");

            for (ITypeElement typeElement : typeElements) {
                dot.appendNode(typeElement, typeElements.size() == 1);

                for (ITypeElement usedTypeElement : typeElement.getLocalUsedTypes()) {
                    dot.appendNode(usedTypeElement);
                    dot.appendEdge(typeElement, usedTypeElement);
                }

                ArrayList<ITypeElement> users = usersMap.get(typeElement);

                if  (users != null) {
                    for (ITypeElement user : users) {
                        dot.appendNode(user);
                        dot.appendEdge(user, typeElement);
                    }
                }
            }

            dot.append("}");

            generateDotOutput(dot, getOutputFile(imageFileName), "svg");

            out(renderer.svgImage(imageFileName));
        }
    }

    protected void generateInheritanceDiagram(ITypeElement typeElement) throws IOException {
        if (generatePerTypeInheritanceDiagrams) {
            ArrayList<ITypeElement> typeElements = new ArrayList<ITypeElement>();
            typeElements.add(typeElement);

            String imageFileName = getOutputFileName(typeElement, "inheritance", ".svg");
            DotGraph dot = generateInheritanceDotGraph(typeElements, imageFileName);

            if (dot.getNumNodes() > 1) { // has content
                String diagramType = typeElement instanceof INedTypeElement ? "ned" : "msg";
                String fullRef = !generateFullInheritanceDiagrams ?
                        "" : " Click "+ renderer.link("here", renderer.appendFilenameExtension("full-"+diagramType+"-inheritance-diagram"), null) + " to see the full picture.";

                out(renderer.subsectionHeading("Inheritance diagram", "subtitle") +
                    renderer.paragraph("The following diagram shows inheritance relationships for this type.\n" +
                    "Unresolved types are missing from the diagram."+fullRef));

                generateDotOutput(dot, getOutputFile(imageFileName), "svg");
                out(renderer.svgImage(imageFileName));
            }
        }
    }

    protected void generateInheritanceDiagram(List<? extends ITypeElement> typeElements, String imageFileName) throws IOException {
        DotGraph dot = generateInheritanceDotGraph(typeElements, imageFileName);
        generateDotOutput(dot, getOutputFile(imageFileName), "svg");
        out(renderer.svgImage(imageFileName));
    }

    protected DotGraph generateInheritanceDotGraph(List<? extends ITypeElement> typeElements, String imageFileName) throws IOException {
        DotGraph dot = new DotGraph();

        dot.append("digraph opp {\n" +
                   "   graph [bgcolor=\"#ffffff00\"] " +
                   "   node [fontsize=10,fontname=Helvetica,shape=box,height=.25,style=filled];\n" +
                   "   edge [arrowhead=none,arrowtail=empty];\n");

        for (ITypeElement typeElement : typeElements) {
            dot.appendNode(typeElement, typeElements.size() == 1);

            if (typeElement instanceof IInterfaceTypeElement) {
                for (INedTypeElement supertype : ((IInterfaceTypeElement)typeElement).getNedTypeInfo().getLocalInterfaces()) {
                    dot.appendNode(supertype);
                    dot.appendEdge(supertype, typeElement);
                }
            }
            else {
                if (typeElement.getSuperType() != null) {
                    ITypeElement extendz = typeElement.getSuperType();
                    dot.appendNode(extendz);
                    dot.appendEdge(extendz, typeElement);
                }
            }

            if (typeElement instanceof IInterfaceTypeElement) {
                ArrayList<INedTypeElement> implementors = directImplementorsMap.get(typeElement);

                if  (implementors != null) {
                    for (INedTypeElement implementor : implementors) {
                        dot.appendNode(implementor);
                        dot.appendEdge(typeElement, implementor, "style=dashed");
                    }
                }
            }
            else if (typeElement instanceof INedTypeElement) {
                INedTypeInfo typeInfo = ((INedTypeElement)typeElement).getNedTypeInfo();
                Set<INedTypeElement> interfaces = typeInfo.getLocalInterfaces();

                if  (interfaces != null) {
                    for (INedTypeElement interfaze : interfaces) {
                        dot.appendNode(interfaze);
                        dot.appendEdge(interfaze, typeElement, "style=dashed");
                    }
                }
            }

            ArrayList<ITypeElement> subtypes = subtypesMap.get(typeElement);

            if (subtypes != null) {
                for (ITypeElement subtype : subtypes) {
                    dot.appendNode(subtype);
                    dot.appendEdge(typeElement, subtype);
                }
            }
        }

        dot.append("}");
        return dot;
    }

    protected void generateSourceContent(IFile file) throws IOException, CoreException {
        generateSourceContent(FileUtils.readTextFile(file.getContents(), file.getCharset()), nedResources.isNedFile(file));
    }

    protected void generateSourceContent(ITypeElement typeElement) throws IOException {
        generateSourceContent(typeElement.getNedSource(), typeElement instanceof INedTypeElement);
    }

    protected void generateSourceContent(String source, boolean nedSource) throws IOException {
        out(renderer.subsectionHeading("Source code", "subtitle"));
        out(renderer.beginSource());

        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);
        ITokenScanner partitioner = nedSource ? new NedSyntaxHighlightPartitionScanner() : new MsgSyntaxHighlightPartitionScanner();
        partitioner.setRange(document, 0, document.getLength());

        StringBuffer buffer = new StringBuffer();
        Object bufferTokenData = null;
        ITokenScanner scanner = null;
        NedPrivateDocColorizerScanner nedPrivateDocColorizerScanner = new NedPrivateDocColorizerScanner();
        NedDocColorizerScanner nedDocColorizerScanner = new NedDocColorizerScanner();
        NedCodeColorizerScanner nedCodeColorizerScanner = new NedCodeColorizerScanner();
        MsgPrivateDocColorizerScanner msgPrivateDocColorizerScanner = new MsgPrivateDocColorizerScanner();
        MsgDocColorizerScanner msgDocColorizerScanner = new MsgDocColorizerScanner();
        MsgCodeColorizerScanner msgCodeColorizerScanner = new MsgCodeColorizerScanner();

        while (true) {
            IToken token = partitioner.nextToken();
            Object data = token.getData();

            if (token.isEOF()) {
                if (buffer.length() != 0)
                    generateSourcePartition(scanner, buffer.toString());

                break;
            }

            if (bufferTokenData != data && buffer.length() != 0) {
                generateSourcePartition(scanner, buffer.toString());
                buffer = new StringBuffer();
                bufferTokenData = null;
            }

            int offset = partitioner.getTokenOffset();
            int length = partitioner.getTokenLength();
            buffer.append(source.substring(offset, offset + length));
            bufferTokenData = data;

            if (nedSource) {
                if (NedSyntaxHighlightPartitionScanner.NED_PRIVATE_DOC.equals(data))
                    scanner = nedPrivateDocColorizerScanner;
                else if (NedSyntaxHighlightPartitionScanner.NED_DOC.equals(data))
                    scanner = nedDocColorizerScanner;
                else
                    scanner = nedCodeColorizerScanner;
            }
            else {
                if (MsgSyntaxHighlightPartitionScanner.MSG_PRIVATE_DOC.equals(data))
                    scanner = msgPrivateDocColorizerScanner;
                else if (MsgSyntaxHighlightPartitionScanner.MSG_DOC.equals(data))
                    scanner = msgDocColorizerScanner;
                else
                    scanner = msgCodeColorizerScanner;
            }
        }

        out(renderer.endSource());
    }

    protected void generateSourcePartition(ITokenScanner scanner, String source) throws IOException {
        org.eclipse.jface.text.Document partition = new org.eclipse.jface.text.Document(source);
        scanner.setRange(partition, 0, source.length());
        StringBuffer buffer = new StringBuffer();
        Object bufferTokenData = null;

        while (true) {
            IToken token = scanner.nextToken();

            if (token.isEOF()) {
                if (buffer.length() != 0)
                    out(renderer.styled(StringEscapeUtils.escapeHtml4(buffer.toString()), null, (TextAttribute)bufferTokenData));

                break;
            }

            Object data = token.getData();

            if (bufferTokenData != data && buffer.length() != 0) {
                out(renderer.styled(StringEscapeUtils.escapeHtml4(buffer.toString()), null, (TextAttribute)bufferTokenData));
                buffer = new StringBuffer();
                bufferTokenData = null;
            }

            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            buffer.append(source.substring(offset, offset + length));
            bufferTokenData = data;
        }
    }

    protected void generateDotOutput(DotGraph dot, File outputFile, String format) throws IOException {
        if (dotExecutablePath == null || !new File(dotExecutablePath).exists())
            throw new IllegalStateException("The GraphViz Dot executable path is invalid, set it using Window/Preferences...\nThe currently set path is: " + dotExecutablePath);

        // dot has a width/height limit of 32768 pixels, see bug #149.
        ProcessUtils.exec(dotExecutablePath, new String[] {"-T" + format, "-o", outputFile.toString()}, ".", dot.toString(), 10, monitor);
    }

    protected String getParamTypeAsString(ParamElementEx param) {
        String type = param.getAttribute(GateElementEx.ATT_TYPE);

        if (type == null)
            return "numeric";
        else
            return type;
    }

    protected String getPropertyElementValue(PropertyElementEx propertyElement, String key) {
        StringBuffer result = new StringBuffer();
        for (PropertyKeyElement propertyKey = propertyElement.getFirstPropertyKeyChild(); propertyKey != null; propertyKey = propertyKey.getNextPropertyKeySibling()) {
            if (propertyKey.getName().equals(key)) {
                for (LiteralElement literal = propertyKey.getFirstLiteralChild(); literal != null; literal = literal.getNextLiteralSibling()) {
                    if (result.length() != 0)
                        result.append(", ");
                    result.append(literal.getValue());
                }
            }
        }
        return result.toString();
    }

    /**
     * Returns the comment of the given NED element with '@include' directives expanded.
     */
    protected String getExpandedComment(INedElement element) throws IOException {
        if (commentCache.containsKey(element))
            return commentCache.get(element);

        String comment = element.getComment();
        if (comment != null && comment.contains("@include")) {
            Stack<File> parents = new Stack<File>();
            IFile file = getNedOrMsgFile(element);
            parents.push(file.getLocation().toFile().getCanonicalFile());
            comment = processIncludes(comment, parents);
        }
        commentCache.put(element, comment);
        return comment;
    }

    private String getFileContent(File file, Stack<File> parents) {
        try {
            if (includedFileCache.containsKey(file))
                return includedFileCache.get(file);

            int index = parents.indexOf(file);
            if (index >= 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Circular @includes: ");
                for (int i = index; i < parents.size(); ++i)
                    sb.append(parents.get(i).getPath()).append(" --> ");
                sb.append(file.getPath());

                throw new NeddocException(sb.toString());
            }

            parents.push(file);
            String content = FileUtils.readTextFile(file, null);
            // add '// ' to the beginning of lines
            content = content.replaceAll("(?m)^(.*)$", "// $1");
            content = processIncludes(content, parents);
            parents.pop();
            includedFileCache.put(file, content);
            return content;
        }
        catch (IOException e) {
            throw new NeddocException("Cannot include file: " + file.getAbsolutePath(), e);
        }
    }

    private String processIncludes(String comment, Stack<File> parents) throws IOException {
        if (comment.contains("@include")) {
            Matcher matcher = includePattern.matcher(comment);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                File dir = parents.peek().getParentFile();
                File file = new File(dir, matcher.group(1)).getCanonicalFile();
                String content = getFileContent(file, parents);
                matcher.appendReplacement(buffer, content.replace("\\", "\\\\").replace("$", "\\$"));
            }

            matcher.appendTail(buffer);
            comment = buffer.toString();
        }
        return comment;
    }

    private String processDebugOptions(String comment, Set<String> options) {
        if (comment.contains("@debug")) {
            Matcher matcher = debugPattern.matcher(comment);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                options.addAll(Arrays.asList(matcher.group(1).split("[ \t]")));
                matcher.appendReplacement(buffer, "");
            }
            matcher.appendTail(buffer);
            comment = buffer.toString();
        }
        return comment;
    }

    protected void out(String string) throws IOException {
        if (monitor.isCanceled())
            throw new CancellationException();

        currentOutputStream.write(string.getBytes("UTF-8"));
    }

    protected void out(byte[] data) throws IOException {
        if (monitor.isCanceled())
            throw new CancellationException();

        currentOutputStream.write(data);
    }

    protected void outMapReference(INedTypeElement model, IFigure figure) throws IOException {
        //FigureUtils.debugPrintFigureAncestors(figure, "");
        Rectangle bounds = new Rectangle(figure.getBounds());
        if (figure instanceof SubmoduleFigure) {
            // translate it to the compound module type figure's coordinate system
            IFigure f = figure;
            while (!(f instanceof CompoundModuleTypeFigure)) {
                f.translateToParent(bounds);
                f = f.getParent();
                Assert.isTrue(f != null);
            }
        }

        out(renderer.areaRef(model.getName(), renderer.appendFilenameExtension(getOutputBaseFileName(model)), bounds));
    }

    protected IPath getOutputFilePath(IFile file) {
        return new Path("");
    }

    protected IPath getOutputFilePath(ITypeElement typeElement) {
        IFile file = getNedOrMsgFile(typeElement);

        if (file != null) {
            IProject elementProject = file.getProject();

            if (!elementProject.equals(project))
                return new Path("../").append(neddocRelativeRootPath).append(elementProject.getName()).append(rootRelativeNeddocPath);
        }

        return new Path("");
    }

    protected String getOutputBaseNameForFile(IFile file) {
        return getOutputFilePath(file).append(file.getProjectRelativePath().toString().replace("/", "-")).toString();
    }

    protected String getOutputBaseFileName(ITypeElement typeElement) {
        return getOutputFileName(typeElement, null, "");
    }

    protected String getOutputFileName(ITypeElement typeElement, String discriminator, String extension) {
        String fileName = "";

        if (typeElement instanceof INedTypeElement)
            fileName += ((INedTypeElement)typeElement).getNedTypeInfo().getFullyQualifiedName();
        else if (typeElement instanceof IMsgTypeElement) {
            String cppName = ((IMsgTypeElement)typeElement).getMsgTypeInfo().getFullyQualifiedCppClassName();
            fileName += cppName.replace("::", "-");
        }

        if (discriminator != null)
            fileName += "-" + discriminator;

        if (extension != null)
            fileName += extension;

        return getOutputFilePath(typeElement).append(fileName).toString();
    }

    protected File getOutputFile(String relativePath) {
        return getFullNeddocPath().append(relativePath).toFile();
    }

    protected IFile getNedOrMsgFile(INedElement element) {
        NedFileElementEx nedFileElement = element.getContainingNedFileElement();

        if (nedFileElement != null)
            return nedResources.getNedFile(nedFileElement);

        MsgFileElementEx msgFileElement = element.getContainingMsgFileElement();

        if (msgFileElement != null)
            return msgResources.getMsgFile(msgFileElement);

        return null;
    }

    protected IPath getReversePath(IPath path) {
        return new Path(StringUtils.repeat("../", path.segmentCount()));
    }

    protected IPath getRelativePath(IPath sourcePath, IPath targetPath) {
        if (sourcePath.equals(targetPath))
            return new Path(".");
        else {
            int commonPrefixLength = targetPath.matchingFirstSegments(sourcePath);
            return new Path(StringUtils.removeEnd(StringUtils.repeat("../", commonPrefixLength), "/")).append(targetPath.removeFirstSegments(commonPrefixLength));
        }
    }

    protected IPath getFullNeddocPath() {
        return documentationRootPath.append(rootRelativeNeddocPath);
    }

    protected IPath getFullDoxyPath() {
        return documentationRootPath.append(rootRelativeDoxyPath);
    }

    protected interface Runnable {
        public void run() throws Exception;
    }

    protected class DotGraph {
        StringBuffer buffer = new StringBuffer();
        Set<ITypeElement> nodes = new LinkedHashSet<ITypeElement>();
        Set<Pair<ITypeElement, ITypeElement>> edges = new LinkedHashSet<Pair<ITypeElement, ITypeElement>>();

        public void append(String text) {
            buffer.append(text);
        }

        public void appendNode(ITypeElement typeElement) {
            appendNode(typeElement, false);
        }

        public void appendNode(ITypeElement typeElement, boolean highlight) {
            if (!nodes.contains(typeElement)) {
                nodes.add(typeElement);

                String name = typeElement.getName();
                append("\"" + name + "\" ");

                append("[URL=\"" + renderer.appendFilenameExtension(getOutputBaseFileName(typeElement)) + "\", target=\"_parent\"");

                String color = "#ff0000";
                if (typeElement instanceof CompoundModuleElementEx && ((CompoundModuleElementEx)typeElement).isNetwork())
                    color = highlight ? "#9090ff" : "#d0d0ff";
                else if (typeElement instanceof CompoundModuleElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof SimpleModuleElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof ModuleInterfaceElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof ChannelElementEx)
                    color = highlight ? "#90ff90" : "#d0ffd0";
                else if (typeElement instanceof ChannelInterfaceElementEx)
                    color = highlight ? "#90ff90" : "#d0ffd0";
                else if (typeElement instanceof MessageElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof PacketElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof ClassElementEx)
                    color = highlight ? "#fff700" : "#fffcaf";
                else if (typeElement instanceof StructElementEx)
                    color = highlight ? "#9090ff" : "#d0d0ff";
                else if (typeElement instanceof EnumElementEx)
                    color = highlight ? "#90ff90" : "#d0ffd0";
                append("fillcolor=\"" + color + "\",");

                if (typeElement instanceof IInterfaceTypeElement)
                    append("style=\"filled,dashed\",");

                append("tooltip=\"" + WordUtils.capitalize(typeElement.getReadableTagName()) + " " + name + "\"]");
                append(";\n");
            }
        }

        public void appendEdge(ITypeElement sourceTypeElement, ITypeElement targetTypeElement) {
            appendEdge(sourceTypeElement, targetTypeElement, "");
        }

        public void appendEdge(ITypeElement sourceTypeElement, ITypeElement targetTypeElement, String attributes) {
            Pair<ITypeElement, ITypeElement> pair = new Pair<ITypeElement, ITypeElement>(sourceTypeElement, targetTypeElement);

            if (!edges.contains(pair)) {
                edges.add(pair);
                append("\"" + sourceTypeElement.getName() + "\" -> \"" + targetTypeElement.getName() + "\" [" + attributes + "];\n");
            }
        }

        public int getNumNodes() {
            return nodes.size();
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }
}
