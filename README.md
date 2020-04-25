

# Specification of my lang

## Variable

- Declaration:

  Var:Type [= Expression]

  ```python
  x:int = 2		    # x is int with init value of 2
  y:int           # y is int with init value of 0
  s:str = "hello" # s is a string with init value of "hello"
  s:str						# s is a string with init value of None
  z:float = x+y   # z is a float with init value of x+y which is 2
  ```

- Assignment

  Var = Expression

  ```python
  s = "world"			# s is assigned to "world"
  z = "error"     # this will raise type incompatible error in compilation phase!
  ```

## List

Currently, len of list in this lang is fixed

- Syntax

  Var:"["Type"]" = "[" "]"

  Var:"["Type"]" = "[" \d (, \d)* "]"

  Var:"["Type"]" = "[" \d "]" * len  # only one init is allowed

```python
L0:[float] = None  # L0 is None
L1:[str] = []      # L1 is None  ([] is equal to None)
L2:[int] = [1,2,3] # initialize array with given elements
L3:[int] = [0]*10  # 10 zeros in one array
L5:[str] = [""]*10   # 10 empty string in one array
def f(x)->int:
  return x*2
L6:[float] = [2]*f(4)
def g(l:[int])->int:
  return l[0]
g([0]*20) 				# initialize a temp list and return 0
class A:
  pass
L7:[A] = [A()]*10  # this will create A() instance 10 times
a:A = A()
L8:[A] = [a] * 10 # this will create list of len 10 with the same instance a
L9:[A] = []*10 # invalid, must be at least 
```

## Tuple

a tuple is just a special immutable list

tuple must contain at least one element

- Syntax

```python
pair:(int,float) = (1,1.2)
triple:(str,int) = ("hello",2)
pair[0]		# 1
triple[0] = "world" # syntax error: cannot modify a tuple
triple[2]	# array out of boundary
len(triple) # 2
def f()->(int,str):
  return (1,"hello")
def g()->(int, str):
  return 2,"world"	# bracket can be omitted, they will be packed automatically
```



## Class

for now constructor of class only support one initialization function because i have not yet figure out how to deal with function overloading.

this is future improvement

## Function



## Namespace

#Grammar

This grammar is partly adopted from python3.5 grammar (remove all the feature I don't  ~~want to~~  implement )

This is absolutely not LL(1) or LR(1) grammar since for variable definition and variable assignment, their FIRST set overlaps with "identifier". There is a couple of solution: use val/var like what scala/js does ; use LL(2) or LR(2); handcoded LL(1) plus a dirty case checking for expression.

This is alomost LL(1) only for variable definition where we need do extra lookahead of two

```$xslt
program := (Newline|stmt)* EOF

stmt := class_def | func_def | nest_stmt
nest_stmt := simple_stmt
					| control_stmt

simple_stmt := (var_def | expr_stmt | del_stmt | pass_stmt 
						| import_stmt | assert_stmt | flow_stmt) Newline
var_def  := tfpdef (= expr)? // this is what breaks LL(1), only here do we check
tfpdef := Identifier: var_type
var_type := (Identifier | '[' var_type ']' | '(' var_type_list ')'
var_type_list := var_type (',' var_type)*

// can only allow for =>  "atom_expr = test" | "test"
// but for parser part we just parse as "test (= test)?"
// and we leave assignment checking to semantic analysis
expr_stmt := test ('=' test)?

/**
augassign: ('+=' | '-=' | '*=' | '@=' | '/=' | '%=' | '&=' | '|=' | '^=' |
            '<<=' | '>>=' | '**=' | '//=')
**/

flow_stmt := break_stmt | continue_stmt | return_stmt // | raise_stmt | yield_stmt

test:= or_test ('if' or_test 'else' test)?
or_test:= and_test ('or' and_test)*
and_test:= not_test ('and' not_test)*
not_test:= 'not' not_test 
				|  comparison
comparison := expr (comp_op expr)*
comp_op := '<'|'>'|'=='|'>='|'<='|'<>'|'!='|'in'|'not' 'in'|'is'|'is' 'not'

/**** not for now
expr:= xor_expr ('|' xor_expr)*
xor_expr:= and_expr ('^' and_expr)*
and_expr:= shift_expr ('&' shift_expr)*
shift_expr:= arith_expr (('<<'|'>>') arith_expr)*
arith_expr:= term (('+'|'-') term)*
term:= factor (('*'|'@'|'/'|'%'|'//') factor)*
factor:= ('+'|'-'|'~') factor | power
****/

expr := term (('+'|'-') term)*
term:= factor (('*'|'/'|'%'|'//') factor)*
factor:= ('+'|'-') factor | power
power:= atom_expr ('**' factor)?
atom_expr:= atom trailer*
atom:= '(' testlist? ')'
		| '[' testlist? ']'
		|  Identifier | Literal | 'None' | 'True' | 'False'
trailer:= '(' arglist? ')'
			| '[' subscriptlist ']'
			| '.' Identifier
arglist := (test (',' test)*)
subscriptlist := test

binary_op := +|-|*|/|//|%|==|!=|<=|>=|<|>|is
params := expr | expr, params

exprlist := expr (',' expr)*
testlist := test (',' test)*

del_stmt := del Identifier (',' Identifier)*
pass_stmt := pass
import_stmt := import Identifier // warning, supported in future
break_stmt := break
continue_stmt := continue
return_stmt := return exprlist?

control_stmt := if_stmt | while_stmt | for_stmt
control_block := Newline Indent nest_stmt* Dedent
if_stmt := if test: control_block Newline* elif_stmt* Newline* else_stmt?
elif_stmt := elif test: control_block elif_stmt
else_stmt := else: control_block
while_stmt := while test: control_block
for_stmt   := for Identifier in test: control_block

func_def := def Identifier parameters (-> var_type)? : control_block
parameters := '(' typedargslist ')'
typedargslist := (tfpdef (',' tfpdef)*)?

class_def := "class" identifier ('(' Identifier? ')')? : class_block
class_body := Newline Indent (var_def | func_def)* Dedent
```

# Support feature

- static typing
- support block scoping (no naming pollution)

# Limitation
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
- No list comprehension
- No dict comprehension
- No default params for function
- body of compound statements like if/while/for must be indented (no one line after : )
- SINGLE inheritance !!!
- No ELLIPSIS
- No empty line between if elif else
- No forward reference (anyway, python reads line by line, but other compile-type languages normally supports it)
- No funciton overloading

# TODO
- support complex number
- support tuple
- support list comprehension
- support dict comprehension
- support slice
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



## Some Interesting shits

- Infinite declaration -> stackoverflow

  ```python
  class A:
    // stackoverflow
    x:int = A().x
  ```

  