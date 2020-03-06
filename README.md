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
- support complex number (parse error)
- support unlimited length number (maybe add Number type later)
- support decorator (error)
- support auto tuple unpack (error)
- support lambda (error)
- support yield (error)
- add multiline string that is triple quote leading string (parse error)
- support async/await (error)
- support try catch/with (error)
- support import mechanism (ignore now, and warning)
- standard library (math, sys and so on) (no standard library support for now)


# Number format regex
```$xslt
decimalPattern = ^([1-9][0-9]*)|(00*)
pointFloatPattern = ^(([0-9]*)?\.[0-9]+)|([0-9]+\.)
exponentFloatPattern = ^([0-9]+|(([0-9]*)?\.[0-9]+)|([0-9]+\.))([eE])([+\-])?[0-9]+
```
