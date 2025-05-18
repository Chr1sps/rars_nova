package rars.riscv.lang.preprocessing

import kotlin.test.Test
import kotlin.test.fail

class PreprocessingTests {
    @Test
    fun `eqv 1`() = """
        .eqv VALUE 5
        li a1, VALUE
    """ preprocessesInto """
        li a1, 5
    """

    @Test
    fun `empty macro`() = """
        .macro empty
        .end_macro
        li a7, 10
        empty
        ecall
    """ preprocessesInto """
        li a7, 10
        
        ecall
    """

    @Test
    fun `macro with body`() = """
        .macro some_macro
        li a7, 10
        ecall
        .end_macro
        some_macro
    """ preprocessesInto """
        li a7, 10
        ecall
    """

    @Test
    fun `macro with parameters`() = """
        .macro terminate (%exit_code)
        li a0, %exit_code
        li a7, 93
        ecall
        .end_macro
        terminate 0
    """ preprocessesInto """
        li a0, 0
        li a7, 93
        ecall
    """
}

private infix fun String.preprocessesInto(expected: String) {
    this.trimIndent()
    val trimmed = this.trimMargin()
    val expectedTrimmed = expected.trimMargin()
    fail("""This test is not implemented yet.""")
}

/*
# Some preprocessing notes

Rough idea of how preprocessing works:
First, we parse the code. Then, we analyze it in terms of includes.

Macro rules:
- macros can't contain the .macro and .end_macro directives
- 
 */