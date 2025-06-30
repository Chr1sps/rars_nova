package rars.assembler;

import org.jetbrains.annotations.NotNull;
import rars.ErrorList;
import rars.Globals;

import java.util.ArrayList;

import static rars.util.UtilsKt.unwrap;

/**
 * Handy class to handle forward label references appearing as data
 * segment operands. This is needed because the data segment is completely
 * processed by the end of the first assembly pass, and its directives may
 * contain labels as operands. When this occurs, the label's associated
 * address becomes the operand value. If it is a forward reference, we will
 * save the necessary information in this object for finding and patching in
 * the correct address at the end of the first pass (for this file or for all
 * files if more than one).
 * If such a parsed label refers to a local or global label not defined yet,
 * pertinent information is added to this object:
 * - memory address that needs the label's address,
 * - number of bytes (addresses are 4 bytes but may be used with any of
 * the integer directives: .word, .half, .byte)
 * - the label's token. Normally need only the name but error message needs
 * more.
 */
final class DataSegmentForwardReferenceList {
    private final @NotNull ArrayList<@NotNull DataSegmentForwardReference> forwardReferenceList;

    DataSegmentForwardReferenceList() {
        this.forwardReferenceList = new ArrayList<>();
    }

    /**
     * Add a new forward reference entry. Client must supply the following:
     * - memory address to receive the label's address once resolved
     * - number of address bytes to store (1 for .byte, 2 for .half, 4 for .word)
     * - the label's token. All its information will be needed if error message
     * generated.
     */
    public void add(
        final int patchAddress,
        final int length,
        final @NotNull Token token
    ) {
        this.forwardReferenceList.add(new DataSegmentForwardReference(
            patchAddress,
            length,
            token));
    }

    /**
     * Add the entries of another DataSegmentForwardReferences object to this one.
     * Can be used at the end of each source file to dump all unresolved references
     * into a common list to be processed after all source files parsed.
     */
    public void add(final @NotNull DataSegmentForwardReferenceList another) {
        this.forwardReferenceList.addAll(another.forwardReferenceList);
    }

    /**
     * Clear out the list. Allows you to re-use it.
     */
    public void clear() {
        this.forwardReferenceList.clear();
    }

    /**
     * Will traverse the list of forward references, attempting to resolve them.
     * For each entry it will first search the provided local symbol table and
     * failing that, the global one. If passed the global symbol table, it will
     * perform a second, redundant, search. If search is successful, the patch
     * is applied and the forward reference removed. If search is not successful,
     * the forward reference remains (it is either undefined or a global label
     * defined in a file not yet parsed).
     */
    public void resolve(final @NotNull SymbolTable localSymbolTable) {
        this.forwardReferenceList.removeIf(entry -> {
            final var localAddress = localSymbolTable.getAddress(entry.token.getText());
            final var labelAddress = (localAddress != null)
                ? localAddress
                : Globals.GLOBAL_SYMBOL_TABLE.getAddress(entry.token.getText());
            final var doRemove = labelAddress != null;
            if (doRemove) {
                // patch address has to be valid b/c we already stored there...
                unwrap(Globals.MEMORY_INSTANCE.set(
                    entry.patchAddress,
                    labelAddress,
                    entry.length
                ));
            }
            return doRemove;
        });
    }

    /**
     * Call this when you are confident that remaining list entries are to
     * undefined labels.
     */
    public void generateErrorMessages(final @NotNull ErrorList errors) {
        for (final DataSegmentForwardReference entry : this.forwardReferenceList) {
            final var message = "Symbol \"%s\" not found in symbol table."
                .formatted(entry.token().getText());
            errors.addTokenError(entry.token(), message);
        }
    }

    /**
     * Inner record to hold each entry of the forward reference list.
     */
    private record DataSegmentForwardReference(int patchAddress, int length,
                                               @NotNull Token token) {
    }

}
