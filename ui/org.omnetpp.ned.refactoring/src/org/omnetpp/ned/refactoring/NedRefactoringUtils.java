/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.refactoring;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.omnetpp.inifile.core.model.ConfigRegistry;
import org.omnetpp.inifile.core.model.IReadonlyInifileDocument;
import org.omnetpp.inifile.core.model.InifileDocument;
import org.omnetpp.inifile.core.model.InifileParser;
import org.omnetpp.inifile.core.model.InifileUtils;
import org.omnetpp.inifile.core.model.KeyType;
import org.omnetpp.inifile.core.model.ParamCollector;
import org.omnetpp.inifile.core.model.ParamResolution;
import org.omnetpp.inifile.core.model.ParamResolutionStatus;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;
import org.omnetpp.ned.model.interfaces.ISubmoduleOrConnection;

/**
 * Shared utilities for NED LTK refactoring classes: INI file search using
 * {@link InifileParser} for structurally-aware matching, and LTK change generation.
 *
 * <p>The parser-based approach eliminates false matches in comments, section headings,
 * and directives, and provides exact key/value distinction via the parser callback.
 * Key classification ({@link KeyType}) and segment extraction ({@link InifileUtils#findLastDot})
 * give further precision.
 */
final class NedRefactoringUtils {

    private NedRefactoringUtils() {}

    /**
     * Reads the full text content of the given file from disk, respecting the file's charset.
     * Returns null if the file cannot be read.
     */
    static String readFileContent(IFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(), file.getCharset()))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1)
                sb.append(buf, 0, n);
            return sb.toString();
        } catch (Exception e) {
            NedResourcesPlugin.logError("Error reading file content: " + file.getFullPath(), e);
            return null;
        }
    }

    /**
     * Represents a text match found in an INI file, with structural classification
     * and optional semantic confirmation from {@link InifileAnalyzer}.
     */
    static class Match {
        final int offset;
        final int length;
        final boolean inKey;    // true = match is in key part, false = in value part
        final String section;   // INI section name (for analyzer lookup); may be null
        final String key;       // INI key (for analyzer lookup); may be null
        Boolean confirmed;      // null = not checked, true/false = analyzer result

        Match(int offset, int length, boolean inKey, String section, String key) {
            this.offset = offset;
            this.length = length;
            this.inKey = inKey;
            this.section = section;
            this.key = key;
        }

        /**
         * Returns whether this match should be enabled in the refactoring preview.
         * If semantic confirmation was performed, uses that; otherwise falls back
         * to the structural {@code inKey} flag.
         */
        boolean isEnabled() {
            return confirmed != null ? confirmed : inKey;
        }
    }

    // ---- File collection -------------------------------------------------------

    /**
     * Collects all {@code .ini} files under {@code project}, skipping directories
     * whose name starts with '_'.
     */
    private static List<IFile> collectIniFiles(IProject project) throws CoreException {
        List<IFile> iniFiles = new ArrayList<>();
        if (project == null)
            return iniFiles;
        project.accept(resource -> {
            if (resource.getType() == IResource.FOLDER && resource.getName().startsWith("_"))
                return false;
            if (resource instanceof IFile && resource.getName().endsWith(".ini"))
                iniFiles.add((IFile) resource);
            return true;
        });
        return iniFiles;
    }

    /**
     * Collects all {@code .cc} and {@code .h} files under {@code project}, skipping
     * directories whose name starts with '_'.
     */
    private static List<IFile> collectCppFiles(IProject project) throws CoreException {
        List<IFile> cppFiles = new ArrayList<>();
        if (project == null)
            return cppFiles;
        project.accept(resource -> {
            if (resource.getType() == IResource.FOLDER && resource.getName().startsWith("_"))
                return false;
            if (resource instanceof IFile) {
                String name = resource.getName();
                if (name.endsWith(".cc") || name.endsWith(".h"))
                    cppFiles.add((IFile) resource);
            }
            return true;
        });
        return cppFiles;
    }

    // ---- Line-offset mapping ---------------------------------------------------

    /**
     * Pre-computes a mapping from 1-based line numbers to character offsets.
     * {@code lineOffsets[i]} is the character offset of line {@code i+1}.
     */
    private static int[] computeLineOffsets(String source) {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n')
                offsets.add(i + 1);
        }
        return offsets.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---- Segment matching helpers ----------------------------------------------

    /**
     * Finds all positions where {@code segment} appears as a complete dot-separated
     * segment in {@code key}. A segment boundary is a dot, {@code [}, or the
     * start/end of the string.
     */
    private static List<Integer> findSegmentPositions(String key, String segment) {
        List<Integer> positions = new ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int pos = key.indexOf(segment, searchFrom);
            if (pos < 0)
                break;
            boolean validStart = (pos == 0) || key.charAt(pos - 1) == '.';
            int endPos = pos + segment.length();
            boolean validEnd = (endPos == key.length()) || key.charAt(endPos) == '.' || key.charAt(endPos) == '[';
            if (validStart && validEnd)
                positions.add(pos);
            searchFrom = pos + 1;
        }
        return positions;
    }

    /**
     * Finds all whole-word occurrences of {@code name} within {@code text},
     * returning their start positions relative to the beginning of {@code text}.
     */
    private static List<Integer> findWholeWordPositions(String text, String name) {
        List<Integer> positions = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find())
            positions.add(matcher.start());
        return positions;
    }

    // ---- Parser-based search: parameter rename ---------------------------------

    /**
     * Finds parameter name occurrences in all INI files under {@code project}.
     * Matches the name as the last dotted segment of {@link KeyType#PARAM PARAM}-type keys.
     * If {@code targetParam} is non-null, uses {@link InifileAnalyzer} to semantically
     * confirm whether each match actually resolves to the given NED parameter.
     */
    static Map<IFile, List<Match>> findParamInIniFiles(IProject project, String paramName, ParamElementEx targetParam) throws CoreException {
        Map<IFile, List<Match>> result = new HashMap<>();
        for (IFile file : collectIniFiles(project)) {
            List<Match> matches = findParamInIniFile(file, paramName, targetParam);
            if (!matches.isEmpty())
                result.put(file, matches);
        }
        return result;
    }

    private static List<Match> findParamInIniFile(IFile file, String paramName, ParamElementEx targetParam) {
        List<Match> matches = new ArrayList<>();
        String source = readFileContent(file);
        if (source == null)
            return matches;

        int[] lineOffsets = computeLineOffsets(source);

        try {
            new InifileParser().parse(new StringReader(source), new InifileParser.ParserAdapter() {
                String currentSection = ConfigRegistry.GENERAL;

                @Override
                public void sectionHeadingLine(int lineNumber, int numLines, String rawLine, String sectionName, String rawComment) {
                    currentSection = sectionName;
                }

                @Override
                public void keyValueLine(int lineNumber, int numLines, String rawLine, String key, String rawValue, String rawComment) {
                    if (KeyType.getKeyType(key) != KeyType.PARAM)
                        return;

                    int lastDot = InifileUtils.findLastDot(key);
                    if (lastDot < 0)
                        return;
                    String lastSegment = key.substring(lastDot + 1);
                    if (!lastSegment.equals(paramName))
                        return;

                    int lineOffset = lineOffsets[lineNumber - 1];
                    int keyPosInLine = rawLine.indexOf(key);
                    if (keyPosInLine < 0)
                        return;
                    int offset = lineOffset + keyPosInLine + lastDot + 1;
                    matches.add(new Match(offset, paramName.length(), true, currentSection, key));
                }
            });
        } catch (CoreException e) {
            NedResourcesPlugin.logError("Error parsing INI file: " + file.getFullPath(), e);
        }

        // Semantic confirmation: use InifileAnalyzer to verify each match
        // actually resolves to the target NED parameter
        if (targetParam != null && !matches.isEmpty()) {
            confirmParamMatches(file, source, matches, targetParam);
        }

        return matches;
    }

    // ---- Semantic confirmation via ParamCollector --------------------------------

    /**
     * Uses {@link ParamCollector} to synchronously resolve parameters for each
     * INI section, then checks whether each match's key actually resolves to
     * {@code targetParam}. Sets {@code match.confirmed} to {@code true} or
     * {@code false} accordingly. On failure, leaves {@code confirmed} as
     * {@code null} (structural fallback).
     */
    private static void confirmParamMatches(IFile file, String source, List<Match> matches, ParamElementEx targetParam) {
        try {
            Document textDoc = new Document(source);
            InifileDocument iniDoc = new InifileDocument(textDoc, file, true);
            iniDoc.parseIfChanged();

            IReadonlyInifileDocument docCopy = iniDoc.getImmutableCopy();
            INedTypeResolver nedResolver = NedResourcesPlugin.getNedResources().getImmutableCopy();

            // Default all matches to "not confirmed"
            for (Match match : matches)
                if (match.section != null && match.key != null)
                    match.confirmed = false;

            // Collect param resolutions per section and cross-reference with matches
            for (String section : docCopy.getSectionNames()) {
                ParamResolutionStatus.Entry entry = ParamCollector.collectParametersAndProperties(
                        docCopy, nedResolver, section, new NullProgressMonitor());

                for (ParamResolution res : entry.paramResolutions) {
                    if (res.key == null)
                        continue;
                    if (!paramDeclarationMatches(res.paramDeclaration, targetParam))
                        continue;
                    // This resolution confirms a match — find the corresponding Match object.
                    // res.section is the section where the key physically exists (may differ
                    // from the activeSection due to section inheritance).
                    for (Match match : matches) {
                        if (res.section.equals(match.section) && res.key.equals(match.key)) {
                            match.confirmed = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // leave all matches with confirmed=null (structural fallback)
            NedResourcesPlugin.logError("Error during semantic INI analysis: " + file.getFullPath(), e);
        }
    }

    /**
     * Value-based comparison of two {@link ParamElementEx} instances.
     * Returns true if they have the same parameter name AND their enclosing
     * types are related by inheritance (one is an ancestor of the other,
     * or they are the same type). This is needed because (1) the analyzer
     * works on deep-copied NED trees, so identity comparison ({@code ==})
     * does not work, and (2) inherited parameters may be reported with
     * a different enclosing type than the original declaration.
     */
    private static boolean paramDeclarationMatches(ParamElementEx a, ParamElementEx b) {
        if (a == null || b == null)
            return false;
        if (a == b)
            return true;
        if (!a.getName().equals(b.getName()))
            return false;
        INedTypeElement aType = a.getEnclosingTypeElement();
        INedTypeElement bType = b.getEnclosingTypeElement();
        if (aType == null || bType == null)
            return false;
        INedTypeInfo aInfo = aType.getNedTypeInfo();
        INedTypeInfo bInfo = bType.getNedTypeInfo();
        if (aInfo == null || bInfo == null)
            return false;
        String aFqn = aInfo.getFullyQualifiedName();
        String bFqn = bInfo.getFullyQualifiedName();
        if (aFqn.equals(bFqn))
            return true;
        // Check if one type is an ancestor of the other (handles inherited params)
        for (INedTypeInfo ancestor : aInfo.getInheritanceChain())
            if (ancestor.getFullyQualifiedName().equals(bFqn))
                return true;
        for (INedTypeInfo ancestor : bInfo.getInheritanceChain())
            if (ancestor.getFullyQualifiedName().equals(aFqn))
                return true;
        return false;
    }

    // ---- Parser-based search: gate / submodule rename --------------------------

    /**
     * Finds name occurrences as dot-separated path segments in all INI file keys
     * under {@code project}. Used for gate renames (no semantic confirmation).
     * Only keys of type {@link KeyType#PARAM PARAM} or
     * {@link KeyType#PER_OBJECT_CONFIG PER_OBJECT_CONFIG} are searched
     * (CONFIG keys have no module path).
     */
    static Map<IFile, List<Match>> findNameInIniFiles(IProject project, String name) throws CoreException {
        Map<IFile, List<Match>> result = new HashMap<>();
        for (IFile file : collectIniFiles(project)) {
            List<Match> matches = findNameInIniFile(file, name, null);
            if (!matches.isEmpty())
                result.put(file, matches);
        }
        return result;
    }

    /**
     * Finds submodule name occurrences as dot-separated path segments in all INI
     * file keys under {@code project}. If {@code targetSubmodule} is non-null, uses
     * {@link ParamCollector} to semantically confirm whether each match's key
     * actually traverses the given NED submodule.
     */
    static Map<IFile, List<Match>> findSubmoduleInIniFiles(IProject project, String name, SubmoduleElementEx targetSubmodule) throws CoreException {
        Map<IFile, List<Match>> result = new HashMap<>();
        for (IFile file : collectIniFiles(project)) {
            List<Match> matches = findNameInIniFile(file, name, targetSubmodule);
            if (!matches.isEmpty())
                result.put(file, matches);
        }
        return result;
    }

    private static List<Match> findNameInIniFile(IFile file, String name, SubmoduleElementEx targetSubmodule) {
        List<Match> matches = new ArrayList<>();
        String source = readFileContent(file);
        if (source == null)
            return matches;

        int[] lineOffsets = computeLineOffsets(source);

        try {
            new InifileParser().parse(new StringReader(source), new InifileParser.ParserAdapter() {
                String currentSection = ConfigRegistry.GENERAL;

                @Override
                public void sectionHeadingLine(int lineNumber, int numLines, String rawLine, String sectionName, String rawComment) {
                    currentSection = sectionName;
                }

                @Override
                public void keyValueLine(int lineNumber, int numLines, String rawLine, String key, String rawValue, String rawComment) {
                    KeyType keyType = KeyType.getKeyType(key);
                    if (keyType == KeyType.CONFIG)
                        return;

                    int lineOffset = lineOffsets[lineNumber - 1];
                    int keyPosInLine = rawLine.indexOf(key);
                    if (keyPosInLine < 0)
                        return;

                    // Search for name as a dotted segment in the path portion of the key only.
                    // For PARAM keys the last segment is the parameter name, for PER_OBJECT_CONFIG
                    // the last segment is the config option name — neither is a module path element.
                    int lastDot = InifileUtils.findLastDot(key);
                    String pathPortion = (lastDot >= 0) ? key.substring(0, lastDot) : key;
                    for (int posInKey : findSegmentPositions(pathPortion, name)) {
                        int offset = lineOffset + keyPosInLine + posInKey;
                        matches.add(new Match(offset, name.length(), true, currentSection, key));
                    }

                    // Also search in value part (whole-word)
                    String valueNoComment = InifileParser.stripComments(rawValue);
                    if (valueNoComment != null && !valueNoComment.isEmpty()) {
                        int eqPos = rawLine.indexOf('=');
                        if (eqPos >= 0) {
                            String afterEq = rawLine.substring(eqPos + 1);
                            String strippedAfterEq = InifileParser.stripComments(afterEq);
                            for (int posInValue : findWholeWordPositions(strippedAfterEq, name)) {
                                int offset = lineOffset + eqPos + 1 + posInValue;
                                matches.add(new Match(offset, name.length(), false, currentSection, key));
                            }
                        }
                    }
                }
            });
        } catch (CoreException e) {
            NedResourcesPlugin.logError("Error parsing INI file: " + file.getFullPath(), e);
        }

        // Semantic confirmation: use ParamCollector to verify each match's key
        // actually traverses the target NED submodule
        if (targetSubmodule != null && !matches.isEmpty()) {
            confirmSubmoduleMatches(file, source, matches, targetSubmodule);
        }

        return matches;
    }

    /**
     * Uses {@link ParamCollector} to semantically confirm submodule matches.
     * For each {@link ParamResolution} whose {@code elementPath} contains a
     * submodule matching {@code targetSubmodule}, the corresponding {@link Match}
     * (by section + key) is confirmed.
     */
    private static void confirmSubmoduleMatches(IFile file, String source, List<Match> matches, SubmoduleElementEx targetSubmodule) {
        try {
            Document textDoc = new Document(source);
            InifileDocument iniDoc = new InifileDocument(textDoc, file, true);
            iniDoc.parseIfChanged();

            IReadonlyInifileDocument docCopy = iniDoc.getImmutableCopy();
            INedTypeResolver nedResolver = NedResourcesPlugin.getNedResources().getImmutableCopy();

            // Default all in-key matches to "not confirmed"
            for (Match match : matches)
                if (match.inKey && match.section != null && match.key != null)
                    match.confirmed = false;

            // Collect param resolutions per section and cross-reference with matches
            for (String section : docCopy.getSectionNames()) {
                ParamResolutionStatus.Entry entry = ParamCollector.collectParametersAndProperties(
                        docCopy, nedResolver, section, new NullProgressMonitor());

                for (ParamResolution res : entry.paramResolutions) {
                    if (res.key == null)
                        continue;
                    if (!elementPathContainsSubmodule(res.elementPath, targetSubmodule))
                        continue;
                    for (Match match : matches) {
                        if (match.inKey && res.section.equals(match.section) && res.key.equals(match.key)) {
                            match.confirmed = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // leave all matches with confirmed=null (structural fallback)
            NedResourcesPlugin.logError("Error during semantic INI analysis: " + file.getFullPath(), e);
        }
    }

    /**
     * Checks whether any element in {@code elementPath} is a submodule matching
     * {@code target} by name and enclosing compound module FQN.
     */
    private static boolean elementPathContainsSubmodule(ISubmoduleOrConnection[] elementPath, SubmoduleElementEx target) {
        if (elementPath == null || target == null)
            return false;
        for (ISubmoduleOrConnection element : elementPath) {
            if (element != null && submoduleMatches(element, target))
                return true;
        }
        return false;
    }

    /**
     * Value-based comparison of a submodule-or-connection from the immutable
     * NED copy with a live-model {@link SubmoduleElementEx}. Checks that both
     * have the same name and reside in the same compound module (by FQN,
     * with inheritance).
     */
    private static boolean submoduleMatches(ISubmoduleOrConnection a, SubmoduleElementEx b) {
        if (a == b)
            return true;
        if (!(a instanceof SubmoduleElementEx))
            return false;
        if (!a.getName().equals(b.getName()))
            return false;
        INedTypeElement aCompound = ((SubmoduleElementEx) a).getEnclosingTypeElement();
        INedTypeElement bCompound = b.getEnclosingTypeElement();
        if (aCompound == null || bCompound == null)
            return false;
        INedTypeInfo aInfo = aCompound.getNedTypeInfo();
        INedTypeInfo bInfo = bCompound.getNedTypeInfo();
        if (aInfo == null || bInfo == null)
            return false;
        String aFqn = aInfo.getFullyQualifiedName();
        String bFqn = bInfo.getFullyQualifiedName();
        if (aFqn.equals(bFqn))
            return true;
        // Check inheritance (compound module may be an inherited submodule)
        for (INedTypeInfo ancestor : aInfo.getInheritanceChain())
            if (ancestor.getFullyQualifiedName().equals(bFqn))
                return true;
        for (INedTypeInfo ancestor : bInfo.getInheritanceChain())
            if (ancestor.getFullyQualifiedName().equals(aFqn))
                return true;
        return false;
    }

    // ---- Parser-based search: NED type rename ----------------------------------

    /**
     * Finds NED type name occurrences in all INI files under {@code project}.
     * Searches for the simple name and fully-qualified name in values of all
     * key-value lines (e.g. {@code network=}, {@code **.typename=}, and other values).
     * Qualified-name matches subsume simple-name matches at the same position.
     */
    static Map<IFile, List<Match>> findTypeInIniFiles(IProject project, String simpleName, String qName) throws CoreException {
        Map<IFile, List<Match>> result = new HashMap<>();
        for (IFile file : collectIniFiles(project)) {
            List<Match> matches = findTypeInIniFile(file, simpleName, qName);
            if (!matches.isEmpty())
                result.put(file, matches);
        }
        return result;
    }

    private static List<Match> findTypeInIniFile(IFile file, String simpleName, String qName) {
        List<Match> matches = new ArrayList<>();
        String source = readFileContent(file);
        if (source == null)
            return matches;

        int[] lineOffsets = computeLineOffsets(source);

        try {
            new InifileParser().parse(new StringReader(source), new InifileParser.ParserAdapter() {
                String currentSection = ConfigRegistry.GENERAL;

                @Override
                public void sectionHeadingLine(int lineNumber, int numLines, String rawLine, String sectionName, String rawComment) {
                    currentSection = sectionName;
                }

                @Override
                public void keyValueLine(int lineNumber, int numLines, String rawLine, String key, String rawValue, String rawComment) {
                    int lineOffset = lineOffsets[lineNumber - 1];
                    int eqPos = rawLine.indexOf('=');
                    if (eqPos < 0)
                        return;

                    String afterEq = rawLine.substring(eqPos + 1);
                    String strippedAfterEq = InifileParser.stripComments(afterEq);

                    // Search for qName first, then simpleName; qName subsumes simpleName at same position
                    Pattern pattern = Pattern.compile("\\b(" + Pattern.quote(qName) + "|" + Pattern.quote(simpleName) + ")\\b");
                    Matcher matcher = pattern.matcher(strippedAfterEq);
                    while (matcher.find()) {
                        int offset = lineOffset + eqPos + 1 + matcher.start();
                        int length = matcher.group().length();

                        // Check for duplicate (qName match subsuming simpleName at same position)
                        boolean isDuplicate = false;
                        for (Match m : matches) {
                            if (offset >= m.offset && offset < m.offset + m.length) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate)
                            matches.add(new Match(offset, length, false, currentSection, key));
                    }

                    // Also search in key part for type name as a dotted segment
                    // (e.g. in section name references within keys, though rare)
                    int keyPosInLine = rawLine.indexOf(key);
                    if (keyPosInLine >= 0) {
                        for (int posInKey : findSegmentPositions(key, simpleName)) {
                            int offset = lineOffset + keyPosInLine + posInKey;
                            boolean isDuplicate = false;
                            for (Match m : matches) {
                                if (offset == m.offset) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            if (!isDuplicate)
                                matches.add(new Match(offset, simpleName.length(), true, currentSection, key));
                        }
                    }
                }
            });
        } catch (CoreException e) {
            NedResourcesPlugin.logError("Error parsing INI file: " + file.getFullPath(), e);
        }

        return matches;
    }

    // ---- C++ file search: parameter rename -------------------------------------

    /**
     * Finds occurrences of a parameter name in C++ string literals passed to
     * {@code par()}, {@code hasPar()}, or {@code findPar()} in all {@code .cc}
     * and {@code .h} files under {@code project}.
     */
    static Map<IFile, List<Match>> findParamInCppFiles(IProject project, String paramName) throws CoreException {
        Map<IFile, List<Match>> result = new HashMap<>();
        Pattern pattern = Pattern.compile(
                "(par|hasPar|findPar)\\s*\\(\\s*\"" + Pattern.quote(paramName) + "\"");
        for (IFile file : collectCppFiles(project)) {
            List<Match> matches = findParamInCppFile(file, paramName, pattern);
            if (!matches.isEmpty())
                result.put(file, matches);
        }
        return result;
    }

    private static List<Match> findParamInCppFile(IFile file, String paramName, Pattern pattern) {
        List<Match> matches = new ArrayList<>();
        String source = readFileContent(file);
        if (source == null)
            return matches;

        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            // The match covers e.g. 'par("paramName"' — compute the offset of
            // just the parameter name inside the quotes.
            int nameOffset = matcher.end() - paramName.length() - 1; // -1 for trailing quote
            Match match = new Match(nameOffset, paramName.length(), true, null, null);
            match.confirmed = true;
            matches.add(match);
        }
        return matches;
    }

    // ---- LTK Change generation -------------------------------------------------

    /**
     * Adds a {@link TextFileChange} to {@code composite} for all matches in {@code file},
     * replacing {@code oldName} with {@code newName}. Whether each edit is enabled depends
     * on {@link Match#isEnabled()}: semantic confirmation (if available) takes precedence
     * over the structural {@code inKey} flag.
     */
    static void addIniFileChanges(CompositeChange composite, IFile file, List<Match> matches,
            String oldName, String newName) throws CoreException {
        addTextFileChanges(composite, file, matches, oldName, newName);
    }

    /**
     * Adds a {@link TextFileChange} to {@code composite} for all matches in {@code file},
     * replacing {@code oldName} with {@code newName}. Whether each edit is enabled depends
     * on {@link Match#isEnabled()}.
     */
    static void addTextFileChanges(CompositeChange composite, IFile file, List<Match> matches,
            String oldName, String newName) throws CoreException {
        TextFileChange change = new TextFileChange(file.getName(), file);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setEdit(rootEdit);

        for (Match match : matches) {
            ReplaceEdit edit = new ReplaceEdit(match.offset, match.length, newName);
            rootEdit.addChild(edit);

            TextEditBasedChangeGroup group = new TextEditBasedChangeGroup(change,
                    new TextEditGroup("Replace '" + oldName + "'", edit));
            group.setEnabled(match.isEnabled());
            change.addChangeGroup(group);
        }

        if (rootEdit.getChildrenSize() > 0) {
            change.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
            composite.add(change);
        }
    }

    /**
     * Adds a {@link TextFileChange} to {@code composite} for all matches in {@code file},
     * replacing either the qualified or simple name as appropriate.
     * Used by {@link RenameNedTypeRefactoring}.
     */
    static void addIniFileChangesForType(CompositeChange composite, IFile file, List<Match> matches,
            String source, String oldSimple, String oldQName, String newSimple, String newQName) throws CoreException {
        TextFileChange change = new TextFileChange(file.getName(), file);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setEdit(rootEdit);

        for (Match match : matches) {
            String oldText = source.substring(match.offset, match.offset + match.length);
            String newText = oldText.equals(oldQName) ? newQName : newSimple;
            ReplaceEdit edit = new ReplaceEdit(match.offset, match.length, newText);
            rootEdit.addChild(edit);

            TextEditBasedChangeGroup group = new TextEditBasedChangeGroup(change,
                    new TextEditGroup("Replace '" + oldText + "'", edit));
            group.setEnabled(true);
            change.addChangeGroup(group);
        }

        if (rootEdit.getChildrenSize() > 0) {
            change.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
            composite.add(change);
        }
    }
}
