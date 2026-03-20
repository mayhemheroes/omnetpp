/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.editor.text;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.omnetpp.common.util.StringUtils;

/**
 * @author levy
 */
public class TextDifferenceUtils {
    public interface ITextDifferenceApplier {
        void replace(int start, int end, String replacement);
    }

    public static void modifyTextEditorContentByApplyingDifferences(final IDocument document, String newText) {
        DocumentRewriteSession session = null;
        try {
            // use rewrite session: it joins replacements into a single undo/redo unit, and also brings huge performance improvement in the case of large documents
            if (document instanceof IDocumentExtension4)
                session = ((IDocumentExtension4)document).startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);

            applyTextDifferences(document.get(), newText, new ITextDifferenceApplier() {
                public void replace(int start, int end, String replacement) {
                    try {
                        int numberOfLines = document.getNumberOfLines();
                        int startOffset = document.getLineOffset(start);
                        int endOffset = end == numberOfLines ? document.getLength() : document.getLineOffset(end);

                        document.replace(startOffset, endOffset - startOffset, replacement);
                    }
                    catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        }
        finally {
            if (document instanceof IDocumentExtension4)
                ((IDocumentExtension4)document).stopRewriteSession(session);
        }

        Assert.isTrue(document.get().equals(newText));
    }

    public static void applyTextDifferences(String original, String target, ITextDifferenceApplier applier) {
        LineRangeComparator comparatorOriginal = new LineRangeComparator(original);
        LineRangeComparator comparatorTarget = new LineRangeComparator(target);
        RangeDifference[] differences = RangeDifferencer.findDifferences(comparatorOriginal, comparatorTarget);

        Arrays.sort(differences, 0, differences.length, new Comparator<RangeDifference>() {
            public int compare(RangeDifference o1, RangeDifference o2) {
                return o1.leftStart() - o2.leftStart();
            }}
        );

        int offset = 0;
        for (int i = 0 ; i < differences.length; i++) {
            RangeDifference difference = differences[i];
            //Debug.println(difference);

            int leftStart = difference.leftStart();
            int leftEnd = difference.leftEnd();
            int rightStart = difference.rightStart();
            int rightEnd = difference.rightEnd();

            String replacement = comparatorTarget.getLineRange(rightStart, rightEnd);
            applier.replace(offset + leftStart, offset + leftEnd, replacement);
            offset += difference.rightLength() - difference.leftLength();
        }
    }

    /**
     * Returns a version of oldSource where only non-whitespace-only diff chunks from
     * newSource are applied. Chunks where old and new lines differ only in whitespace are
     * left unchanged (oldSource lines are kept), preserving the user's original formatting.
     * Chunks with real content changes (e.g. renamed identifiers) are taken from newSource.
     */
    public static String filterWhitespaceOnlyDiffs(String oldSource, String newSource) {
        LineRangeComparator comparatorOld = new LineRangeComparator(oldSource);
        LineRangeComparator comparatorNew = new LineRangeComparator(newSource);
        RangeDifference[] differences = RangeDifferencer.findDifferences(comparatorOld, comparatorNew);

        Arrays.sort(differences, 0, differences.length, new Comparator<RangeDifference>() {
            public int compare(RangeDifference o1, RangeDifference o2) {
                return o1.leftStart() - o2.leftStart();
            }}
        );

        StringBuilder result = new StringBuilder();
        int oldPos = 0;  // next line index in oldSource to copy
        for (RangeDifference diff : differences) {
            int leftStart = diff.leftStart();
            int leftEnd   = diff.leftEnd();
            int rightStart = diff.rightStart();
            int rightEnd   = diff.rightEnd();

            // copy unchanged lines before this chunk
            result.append(comparatorOld.getLineRange(oldPos, leftStart));

            String oldChunk = comparatorOld.getLineRange(leftStart, leftEnd);
            String newChunk = comparatorNew.getLineRange(rightStart, rightEnd);

            if (oldChunk.replaceAll("\\s", "").equals(newChunk.replaceAll("\\s", "")))
                result.append(oldChunk);  // whitespace-only difference: keep old
            else
                result.append(newChunk);  // real change: take new

            oldPos = leftEnd;
        }
        // copy remaining unchanged lines
        result.append(comparatorOld.getLineRange(oldPos, comparatorOld.getRangeCount()));
        return result.toString();
    }

    /**
     * Returns a patch-like string for turning leftText into rightText.
     * Lines to be removed are prefixed with "-", lines to be added are prefixed with "+".
     */
    public static String computeDiff(String leftText, String rightText) {
        LineRangeComparator leftLines = new LineRangeComparator(leftText);
        LineRangeComparator rightLines = new LineRangeComparator(rightText);
        RangeDifference[] differences = RangeDifferencer.findDifferences(leftLines, rightLines);

        Arrays.sort(differences, 0, differences.length, new Comparator<RangeDifference>() {
            public int compare(RangeDifference o1, RangeDifference o2) {
                return o1.leftStart() - o2.leftStart();
            }}
        );

        StringBuilder result = new StringBuilder();
        for (RangeDifference difference : differences) {
            int leftStart = difference.leftStart();
            int leftEnd = difference.leftEnd();
            int rightStart = difference.rightStart();
            int rightEnd = difference.rightEnd();

            String linesRemoved = leftStart == leftEnd ? "" : StringUtils.indentLines(leftLines.getLineRange(leftStart, leftEnd), "- ");
            String linesAdded = rightStart == rightEnd ? "" : StringUtils.indentLines(rightLines.getLineRange(rightStart, rightEnd), "+ ");

            result.append(String.format("@@ -%d,%d +%d,%d @@\n", leftStart, leftEnd-leftStart, rightStart, rightEnd-rightStart));
            result.append(linesRemoved);
            result.append(linesAdded);
            result.append("\n");
        }
        return result.toString().trim() + "\n";
    }

}

class LineRangeComparator implements IRangeComparator {
    private String text;

    private String[] lines;

    public LineRangeComparator(String text) {
        this.text = text;
        lines = StringUtils.splitToLines(text);
    }

    public String getText() {
        return text;
    }

    public int getRangeCount() {
        return lines.length;
    }

    public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
        return getLineAt(thisIndex).equals(((LineRangeComparator)other).getLineAt(otherIndex));
    }

    public String getLineAt(int index) {
        return lines[index];
    }

    public String getLineRange(int start, int end) {
        return StringUtils.join(ArrayUtils.subarray(lines, start, end));
    }

    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
        return false;
    }
}

class StringDifferenceApplier implements TextDifferenceUtils.ITextDifferenceApplier {
    private String text;

    public StringDifferenceApplier(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void replace(int start, int end, String replacement) {
        String[] lines = StringUtils.splitToLines(text);
        text =
            StringUtils.join(ArrayUtils.subarray(lines, 0, start)) +
            replacement +
            StringUtils.join(ArrayUtils.subarray(lines, end, lines.length));
    }
}
