# File format

Each line contains specification for one pseudo-op, including optional description.
First item is source statement syntax, specified in same "example" parser format used for regular instructions.
Source statement specification ends with a tab. It is followed by a tab-separated list of basic instruction
templates to complete and substitute for the pseudo-op.
Format for specifying syntax of templates is different from specifying syntax of source statement:

- `(n=0,1,2,3,...)` is token position in source statement (operator is token 0, parentheses are tokens but commas
  aren't)
- `RGn` means substitute register found in n'th token of source statement
- `LLn` means substitute low order 16-bits from label address in source token n.
- `LHn` means substitute high order 16-bits from label address in source token n. Must add 1 if address bit 11 is 1.
- `PCLn` is similar to `LLn` except the value substituted will be relative to PC of the pseudo-op.
- `PCHn` is similar to `LHn` except the value substituted will be relative to PC of the pseudo-op.
- `VLn` means substitute low order 16-bits from 32-bit value in source token n.
- `VHn` means substitute high order 16-bits from 32-bit value in source token n, then add 1 if value's bit 11 is 1.
- `LAB` means substitute textual label from last token of source statement. Used for various branches.

Everything else is copied as is into the generated statement (you must use register numbers not mnemonics)
The list of basic instruction templates is optionally followed a description of the instruction for help purposes.
To add optional description, append a tab then the '#' character followed immediately (no spaces) by the description.

See documentation for ExtendedInstruction.makeTemplateSubstitutions() for more details.

Matching for a given instruction mnemonic is first-fit not best-fit. If an instruction has both 16 and 32-bit
immediate operand options, they should be listed in that order (16-bit version first). Otherwise, the 16-bit
version will never be matched since the 32-bit version fits small immediate values first.

The pseudo-op specification must start in the first column. If first column is blank, the line will be skipped!

When specifying the example instruction (first item on the line), the conventions I follow are:

- for a register operand, specify a numbered register (e.g. `$t1` or `$f1`) to represent any register in the set.
  The numerical value is not significant. This is NOT the case when writing the templates that follow!
  In the templates, numbered registers are parsed as is (use only `$0` and `$1`, which are `$zero` and `$at`).
- for an immediate operand, specify a positive value indicative of the expected range. I use 10 to represent
  a 5 bit value, 100 to represent a 16-bit value, and 100000 to represent a 32-bit value.
- for a label operand, I use the string "label" (without the quotes).
  The idea is to give the parser an example that will be parsed into the desired token sequence. Syntax checking
  is done by comparing the source token sequence to list of token sequences generated from the examples.

IMPORTANT NOTE:  The use of `$t1`, `$t2`, etc. in the instruction sample means that any CPU register reference
can be used in that position. It is simply a placeholder. By contrast, when
`$1` is used in the template specification, `$1` (`$at`) is literally placed into the generated
instruction!  If you want the generated code to echo the source register, use `RG1`, `RG2`, etc.

## Copyright notice

Copyright (c) 2003-2010, Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, https://www.opensource.org/licenses/mit-license.html)

