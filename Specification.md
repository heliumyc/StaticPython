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

## Class

for now constructor of class only support one initialization function because i have figure out how to deal with function overloading.

this is future improvement

## Function



## Namespace

