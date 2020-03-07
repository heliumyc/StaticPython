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
- no consecutive assign is allowed ( eg. x = y = z = 1 is illegal)

# TODO
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

For simplicity, I just use LL(2) grammar and also recursive descent parser can be easily written for LL(2).

```$xslt
program := (Newline|stmt)* EOF
stmt      := simple_stmt Newline
					| compound_stmt
simple_stmt := var_def | expr | del_stmt | pass_stmt | import_stmt | assert_stmt | flow_stmt
flow_stmt := break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
var_def  := Identifier : type = expr
type := Identifier | '[' Identifier ']'

expr := cexpr
			| not expr
			| expr [and|or] expr     // left recursion
      | expr if expr else expr // left recursion
cexpr := Identifier 
      | Literal
      | '[' params? ']'
      | '(' expr ')'
      | member_expr
      | index_expr
      | member_expr '(' params? ')'
      | Identifier '(' params? ')'
      | cexpr binary_op cexpr  // left recursion and op precedence
      | '-' cexpr   // unary operator
binary_op := +|-|*|/|//|%|==|!=|<=|>=|<|>|is
params := expr | expr, params

del_stmt := del Identifier
pass_stmt := pass
import_stmt := import .* // warning, supported in future
assert_stmt := assert .* // warning, supported in future
raise_stmt  := raise .* // warning, supported in future
yield_stmt  := yield .* // warning, supported in future
break_stmt := break
continue_stmt := continue
return_stmt := return expr?
 
compound_stmt := if_stmt | while_stmt | for_stmt | func_def | class_def

if_stmt := if expr: block elif_stmt else_stmt?
elif_stmt := elif expr: block elif_stmt | <empty>
else_stmt := else: block
block  := Newline Indent expr Dedent

while_stmt := while expr: block
for_stmt   := for Identifier in expr: block

func_def := def Identifier '(' argslist? ')' -> type : Newline Indent func_body Dedent
func_body := stmt*

class_def := "class" identifier class_base? : Newline Indent class_body Dedent
class_base := '('Identifier')' | '(' ')'
class_body := stmt* 

```






# Others
