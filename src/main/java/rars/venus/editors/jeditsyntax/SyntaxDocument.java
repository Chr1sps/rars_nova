/*
 * SyntaxDocument.java - Document that can be tokenized
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.venus.editors.jeditsyntax.tokenmarker.TokenMarker;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.undo.UndoableEdit;

/**
 * A document implementation that can be tokenized by the syntax highlighting
 * system.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxDocument.java,v 1.14 1999/12/13 03:40:30 sp Exp $
 */
public class SyntaxDocument extends PlainDocument {
    private static final Logger LOGGER = LogManager.getLogger();
    // protected members
    protected TokenMarker tokenMarker;

    /**
     * Returns the token marker that is to be used to split lines
     * of this document up into tokens. May return null if this
     * document is not to be colorized.
     *
     * @return a {@link TokenMarker} object
     */
    public TokenMarker getTokenMarker() {
        return this.tokenMarker;
    }

    /**
     * Sets the token marker that is to be used to split lines of
     * this document up into tokens. May throw an exception if
     * this is not supported for this type of document.
     *
     * @param tm The new token marker
     */
    public void setTokenMarker(final TokenMarker tm) {
        this.tokenMarker = tm;
        if (tm == null)
            return;
        this.tokenMarker.insertLines(0, this.getDefaultRootElement()
                .getElementCount());
        this.tokenizeLines();
    }

    /**
     * Reparses the document, by passing all lines to the token
     * marker. This should be called after the document is first
     * loaded.
     */
    public void tokenizeLines() {
        this.tokenizeLines(0, this.getDefaultRootElement().getElementCount());
    }

    /**
     * Reparses the document, by passing the specified lines to the
     * token marker. This should be called after a large quantity of
     * text is first inserted.
     *
     * @param start The first line to parse
     * @param len   The number of lines, after the first one to parse
     */
    public void tokenizeLines(final int start, int len) {
        if (this.tokenMarker == null || !TokenMarker.supportsMultilineTokens())
            return;

        final Segment lineSegment = new Segment();
        final Element map = this.getDefaultRootElement();

        len += start;

        try {
            for (int i = start; i < len; i++) {
                final Element lineElement = map.getElement(i);
                final int lineStart = lineElement.getStartOffset();
                this.getText(lineStart, lineElement.getEndOffset()
                        - lineStart - 1, lineSegment);
                this.tokenMarker.markTokens(lineSegment, i);
            }
        } catch (final BadLocationException bl) {
            SyntaxDocument.LOGGER.error("Error when tokenizing lines.", bl);
        }
    }

    /**
     * Starts a compound edit that can be undone in one operation.
     * Subclasses that implement undo should override this method;
     * this class has no undo functionality so this method is
     * empty.
     */
    public void beginCompoundEdit() {
    }

    /**
     * Ends a compound edit that can be undone in one operation.
     * Subclasses that implement undo should override this method;
     * this class has no undo functionality so this method is
     * empty.
     */
    public void endCompoundEdit() {
    }

    /**
     * Adds an undoable edit to this document's undo list. The edit
     * should be ignored if something is currently being undone.
     *
     * @param edit The undoable edit
     * @since jEdit 2.2pre1
     */
    public void addUndoableEdit(final UndoableEdit edit) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * We overwrite this method to update the token marker
     * state immediately so that any event listeners get a
     * consistent token marker.
     */
    @Override
    protected void fireInsertUpdate(final DocumentEvent evt) {
        if (this.tokenMarker != null) {
            final DocumentEvent.ElementChange ch = evt.getChange(
                    this.getDefaultRootElement());
            if (ch != null) {
                this.tokenMarker.insertLines(ch.getIndex() + 1,
                        ch.getChildrenAdded().length -
                                ch.getChildrenRemoved().length);
            }
        }

        super.fireInsertUpdate(evt);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We overwrite this method to update the token marker
     * state immediately so that any event listeners get a
     * consistent token marker.
     */
    @Override
    protected void fireRemoveUpdate(final DocumentEvent evt) {
        if (this.tokenMarker != null) {
            final DocumentEvent.ElementChange ch = evt.getChange(
                    this.getDefaultRootElement());
            if (ch != null) {
                this.tokenMarker.deleteLines(ch.getIndex() + 1,
                        ch.getChildrenRemoved().length -
                                ch.getChildrenAdded().length);
            }
        }

        super.fireRemoveUpdate(evt);
    }
}
