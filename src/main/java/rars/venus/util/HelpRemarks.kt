package rars.venus.util

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import rars.util.toHexString
import java.awt.Color

fun createHelpRemarks(backgroundColor: Color): String = createHTML().html {
    body {
        div {
            attributes["style"] = "text-align: center;"
            // Table with attributes
            table {
                attributes["bgcolor"] = backgroundColor.toHexString()
                attributes["border"] = "0"
                attributes["cellpadding"] = "0"

                header("Operand Key for Example Instructions")
                row("label, target", "any textual label")
                row("t1, t2, t3", "any integer register")
                row("f2, f4, f6") {
                    i { +"even-numbered" }
                    +" floating point register"
                }
                row("f0, f1, f3") {
                    i { +"any" }
                    +" floating point register"
                }
                row("10", "unsigned 5-bit integer (0 to 31)")
                row("-100", "signed 16-bit integer (-32768 to 32767)")
                row("100", "unsigned 16-bit integer (0 to 65535)")
                row("100000", "signed 32-bit integer (-2147483648 to 2147483647)")
                emptyRow()

                header("Load & Store addressing mode, basic instructions")
                row("-100(t2)", "sign-extended 16-bit integer added to contents of t2")
                emptyRow()

                header("Load & Store addressing modes, usePseudoInstructions instructions")
                row("t2", "contents of t2")
                row("-100", "signed 16-bit integer")
                row("100", "unsigned 16-bit integer")
                row("100000", "signed 32-bit integer")
                row("100(t2)", "zero-extended unsigned 16-bit integer added to contents of t2")
                row("100000(t2)", "signed 32-bit integer added to contents of t2")
                row("label", "32-bit address of label")
                row("label(t2)", "32-bit address of label added to contents of t2")
                row("label+100000", "sum of 32-bit integer and label's address")
                row("label+100000(t2)", "sum of 32-bit integer, label's address, and contents of t2")
            }
        }
    }
}

private fun TABLE.header(title: String) = tr {
    th {
        attributes["colspan"] = "2"
        b {
            i {
                span {
                    attributes["style"] = "font-size: larger;"
                    +title
                }
            }
        }
    }
}

private fun TABLE.row(format: String, description: String) = tr {
    td { code { +format } }
    td { +description }
}

private fun TABLE.row(format: String, descriptionFunc: TD.() -> Unit) = tr {
    td { code { +format } }
    td { descriptionFunc() }
}

private fun TABLE.emptyRow() = tr { }