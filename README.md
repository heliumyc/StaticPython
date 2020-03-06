# Support feature

- static typing
- support block scoping (no naming pollution)

# Limit
- formfeeds outside of string is considered invalid (in python used
as indentation token)
- tab is considered as 8 whitespaces for simplicity
- no complex number is supported ("2j" is invalid)
- number consists only float(64bit) and int(32bit) and long
(actually considering add number type of unlimited length number)
- no underscore is allowed in numbers
- only single line string for simplicity
- nonlocal and global is deprecated (using static typing which means
explicit declaration and block scoping means no need for nonlocal
and global)
- semicolon ; as statement end is illegal

# TODO
- support compound  expression like oneline if else
- support complex number
- support unlimited length number (maybe add Number type later)
- support decorator
- support auto tuple unpack
- support lambda
- support yield
- add multiline string that is triple quote leading string (parse error)
- support async/await 
- support try catch/with
- support import mechanism (ignore now, and warning)
- standard library (math, sys and so on) (no standard library support for now)

#Number format regex

```$xslt
decimalPattern = ^([1-9][0-9]*)|(00*)
pointFloatPattern = ^(([0-9]*)?\.[0-9]+)|([0-9]+\.)
exponentFloatPattern = ^([0-9]+|(([0-9]*)?\.[0-9]+)|([0-9]+\.))([eE])([+\-])?[0-9]+
```

#Grammar

This grammar is partly adopted from python3 grammar and chocopy grammar

This is absolutely not LL(1) or LR(1) grammar since for variable definition and variable assignment, their FIRST set overlaps with "identifier". There is a couple of solution: use val/var like what scala/js does ; use LL(2) or LR(2); handcoded LL(1) plus a dirty case checking for expression.

```$xslt
program := (newline|stmt)* EOF
stmt      := simple_stmt | compound_stmt
simple_stmt := var_def | expression | del_stmt | pass_stmt | import_stmt | assert_stmt
						| break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
var_def    := identifier : identifier
expression := 
class_def := class identifier class_base? : newline class_body
class_base := (identifier) | ()
class_body := indent stmt* dedent
func_
```






# Others
