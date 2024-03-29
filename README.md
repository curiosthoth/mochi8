Mochi8
======
- Table of Contents
  - [Overview](#overview)
  - [Installation](#installation)
  - [Quick Start](#quick-start)
  - [Emulator Usage](#emulator-usage)
  - [Assembler Usage](#assembler-usage)
  - [Assembly Language Reference](#assembly-language-reference)
    - [Language Overview](#language-overview)
      - [The Basics](#the-basics)
      - [Expressions & Arithmetic](#expressions--arithmetic)
      - [Conventions](#conventions)
    - [Instructions](#instructions)
      - [Directives](#directives)
  - [References](#references)

## Overview
Mochi8 is a CHIP-8/SCHIP toolkit written in Java 8. It includes a Swing based emulator, an assembler and a disassembler.
If you would like to read more about CHIP-8, visit [CHIP-8 Wikipedia Page](https://en.wikipedia.org/wiki/CHIP-8).

![sample](pics/sample01.png)

*(Screenshot of game Blinky from David Winter's CHIP-8 game pack)*

#### Features

Emulator:

- Supports both CHIP-8/S-CHIP instruction sets 
- Supports 64x32, 64x64 (CHIP-8 Hires) and 128x64 screen resolutions
- Supports various window sizes and aspect ratios
- Supports changing foreground and background colors
- Supports various speed mode (100Hz to 10,000Hz)
- Supports misc features such as Windows Always On Top, Pause on Background etc

Assembler / Disassembler:
- Assembler is mostly Christian Egeberg's CHIPPER v2.11 compatible with a few exceptions
- Disassembles CHIP-8/S-CHIP binary files
- Supports forward referencing of variables before they are declared
- Supports bin/hex/oct mixed notions in expressions, e.g., (9%3) * 2 + #F & 8 - 77/$111
- Limits: currently only supports assembling to a binary file for emulators. Does not support generating CHIP-48 or other real hardware-recognizable formats.
- Limits: Not enough error detection during assembling.

#### Why CHIP-8?
Writing emulators is particularly helpful in understanding how CPU runs and how memory is arranged.  
CHIP-8 features a small set of instructions that are very well documented and there was no complicated memory modes 
involved, thus it is a very ideal candidate to use as a show case of writing an emulator.

## Installation

Java 8 needed.

    mvn clean package
    
## Quick Start

Follow this quick start guide to get a sip of Mochi8 assembler and emulators.

1. Save the sample Hello source code below.
    

```ASM
; Name: hello.asm
; Author: Jeffrey Bian
; Description: 
; 	This is a minimum hello world program for S-CHIP. Assemble with: 
; 		java -jar <mochi8-0.8.0.jar> -a asmhello.asm -o hello.ch8
;	Then load with mochi8 emulator. This prgram will print 6 numbers 5 4 3 2 1 0 from left to right. 
;

HIGH				; Enters HIGH resolution mode
CLS					; Clears the screen
LD V2, LENGTH		; Set the maximum number
LD V3, 1
LD V5, 24			; The Y coords

LOOP:				; Begins the loop
SUB V2, V3
LD HF, V2			; Load I with the address of the HiRes Number Sprites
ADD V4, U			; Increment X coords for each number to print
DRW V4, V5, H		; Draw!
SE V2, 0			
JP LOOP

FIN: JP FIN			; Stays at the screen and don't exit.

LENGTH = 6
U = #10
H = 5 * 2

```
2. Switch to the directory where you have your Mochi8 assembler downloaded, copy the downloaded hello.asm there too. Then type the following command in command line, 

```java -jar <mochi8-0.8.0.jar> asm a -o hello.ch8 hello.asm```

It will assemble the source to a binary file named hello.ch8, which is runnable by the emulator. 

3. Launch the emulator with the command 

```java -jar mochi8-0.8.0.jar```

It should bring up the emulator main window. If first time run, there will be a popup dialog reminding you of the key mappings. 

4. Hit Control+L (Windows/Linux) or Command + L (Mac OS X), or select File -> Load. Then in the open file dialog, select the generated hello.ch8 and click open.

5. You should be able to see a slow drawing on the screen now. If not, please make sure in Emulator menu, "Run on Load" is checked. The output looks like something like below,


![Hello](pics/hello.png)


Have fun! 


## Emulator Usage

If launching the jar without any parameters, it will invoke the emulator (Swing based).
    
    java -jar mochi8-0.8.0.jar

## Assembler Usage

If launching the jar with *asm* as the sub command name, it will invoked the assembler.
    
    java -jar mochi8-0.8.0.jar asm <options> input_file

where the *input_file* argument is required. It could be either the name for the source file to assemble or the binary file to disassemble. 

Available options as below,

* **--asm, -a**   
    Assemble mode, this is the default mode if neither --asm or --dsm is specified. The output will be the assembled binary file. In this mode, make sure the *input_file* is an ASCII compatible text file.

* **--dsm, -d**   
    Disassemble mode. The output will be the disassembled text file. In this mode, make sure the *input_file* is a binary CHIP-8/S-CHIP program.
* **--out <name>, -o <name>**   
    Specifies output file. If not specified or specified as 'stdout', it will print the result to the standard output.
* **--base <addr>, -b <addr>**   
    Specifies Base loading address, default to 200 (hex). The value *addr* must be a valid hex-decimal string which will be interpreted as base 16 integer.
* **--print, -p**  
    Print the intermediate Assembled lines to STDOUT. Works only under assemble mode. Good for debugging the assembler.
* **--help, -h**  
    Print this quick help message.

## Assembly Language Reference

The assumption is you are already familiar with basic CHIP-8/S-CHIP program structure, registers and memory model. A good source to revisit these prerequisites is at [Cowgod's CHIP-8 Tehnical Reference](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM). Mochi8 assembler tries to be compatible with Christian Egeberg's CHIPPER assembler, but there are incompatible pieces. Though normally CHIPPER assembly files will compile with Mochi8.

The reference text presented here shows all supported instructions and directives for Mochi8 assembler. Please do not use it as a CHIPPER assembly reference.

### Language Overview

#### The Basics
The basic assembly language structures are instructions, directives, variables and labels. 

* Instructions

One instruction maps to one 16-bit long opcode in the final assembled file. Say, the instruction *ADD V4, 32* will translate to 7432, a 16-bit long opcode in final assembled file. Only 1 instruction per line is allowed. An instruction starts with the name of the instruction, then white spaces, then a comma separated list of arguments.

* Directives

Assistant language structure to make writing programs easier. Directives normally do not consume any program space in the final file. It only helps control the program structure. There are two categories of directives, prefix and infix. Most directives fall into the former, there are only two falling in the latter, namely the *EQU* and *=* directives.

* Variables  

Variables are only for assemble time. A variable is defined by an infix directive *EQU* or *=*. E.g., Score EQU 33 . There can only be one variable definition per line.

* Labels  

Labels are optional. There could be only one label per line and it must appear before any other text in one line. E.g., 
*Loop: JP Loop* gives an infinite loop. Labels end with a colon (:) and will always have a default value as the current in memory address, which is the loading address + the file offset.

The CHIP-8 assembly language is highly line oriented and each line takes the format of:
 
```<label:> instruction | directive | variable assignment```


#### Expressions & Arithmetic
Mochi8 supports expressions anywhere a constant may appear. An expression can contain any variables, labels, constants and arithmetic operators. E.g., $111 * SPRITE_N + (~COLLISION) + 3/2 is a valid expression. 

Numbers without prefix are considered as decimal number. *$NNNN* represents a binary number. *#NNNN* represents a hex number. *@NNNN* represents an octal number. 

Below lists the arithmetic operator priority ( from highest to lowest ), as excerpted from *CHIPPER.doc*. 

    (  ; Start parentheses expression

    )  ; End of parentheses expression

    ----------------------------------

    +  ; Unary plus sign

    -  ; Unary minus sign

    ~  ; Bitwise NOT operator

    ----------------------------------

    !  ; Power of operator

    <  ; Shift left number of bits

    >  ; Shift right number of bits

    ----------------------------------

    *  ; Multiply

    /  ; Divide

    ----------------------------------

    +  ; Add

    -  ; Subtract

    ----------------------------------

    &  ; Bitwise AND operator

    |  ; Bitwise OR operator

    ^  ; Bitwise XOR operator

    ----------------------------------

    \  ; Low priority divide

    %  ; Modulus operator



Note that the expression must be resolvable at assemble time.

#### Conventions
Mochi8 assembler will convert all text, except for those quoted by single quotes (') into upper case. Given this said, LD and ld means the same instruction while My_VAr and MY_VAR will be treated as the same variable. 

Strings are single quoted ('). Any consecutive single quote will produce an escaped single quote character. Say, *'Good Morning Ma''am'* will resolve as Good **Morning Ma'am**.

By default, the data in program will align at word boundary. But this can be turn on / off by ALIGN directive.

### Instructions

The section in below is excerpted from *CHIPPER.doc*.

      ADD   I, VX           ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set I = I + VX
      ADD   VX, Byte        ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX + Byte
      ADD   VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX + VY, VF = carry
      AND   VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX & VY, VF updates
      CALL  Addr            ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Call subroutine at Addr (16 levels)
      CLS                   ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Clear display
      DRW   VX, VY, Nibble  ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Draw Nibble byte sprite stored at
                                 ; [I] at VX, VY. Set VF = collision
      DRW   VX, VY, 0       ; SCHIP10, SCHIP11
                                 ; Draw extended sprite stored at [I]
                                 ; at VX, VY. Set VF = collision
      EXIT                  ; SCHIP10, SCHIP11
                                 ; Terminate the interpreter 
      HIGH                  ; SCHIP10, SCHIP11
                                 ; Enable extended screen mode
      JP    Addr            ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Jump to Addr
      JP    V0, Addr        ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Jump to Addr + V0
      LD    B, VX           ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Store BCD of VX in [I], [I+1], [I+2]
      LD    DT, VX          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set delaytimer = VX
      LD    F, VX           ; CHIP8, CHIP48
                                 ; Point I to 5 byte numeric sprite
                                 ; for value in VX
      LD    HF, VX          ; SCHIP10, SCHIP11
                                 ; Point I to 10 byte numeric sprite
                                 ; for value in VX
      LD    I, Addr         ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set I = Addr
      LD    LF, VX          ; SCHIP10, SCHIP11
                                 ; Point I to 5 byte numeric sprite
                                 ; for value in VX
      LD    R, VX           ; SCHIP10, SCHIP11
                                 ; Store V0 .. VX in RPL user flags.
                                 ; Only V0 .. V7 valid
      LD    ST, VX          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set soundtimer = VX
      LD    VX, Byte        ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = Byte
      LD    VX, DT          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = delaytimer
      LD    VX, K           ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = key, wait for keypress
      LD    VX, R           ; SCHIP10, SCHIP11
                                 ; Read V0 .. VX from RPL user flags.
                                 ; Only V0 .. V7 valid
      LD    VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VY, VF updates
      LD    VX, [I]         ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Read V0 .. VX from [I] .. [I+X]
      LD    [I], VX         ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Store V0 .. VX in [I] .. [I+X]
      LOW                   ; SCHIP10, SCHIP11
                                 ; Disable extended screen mode
      OR    VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX | VY, VF updates
      RET                   ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Return from subroutine (16 levels)
      RND   VX , Byte       ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = random & Byte
      SCD   Nibble          ; SCHIP11
                                 ; Scroll screen Nibble lines down
      SCL                   ; SCHIP11
                                 ; Scroll screen 4 pixels left         
      SCR                   ; SCHIP11
                                 ; Scroll screen 4 pixels right    
      SE    VX, Byte        ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if VX == Byte
      SE    VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if VX == VY
      SHL   VX {, VY}       ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX << 1, VF = carry
      SHR   VX {, VY}       ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX >> 1, VF = carry
      SKP   VX              ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if key VX down
      SKNP  VX              ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if key VX up
      SNE   VX, Byte        ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if VX != Byte
      SNE   VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Skip next instruction if VX != VY
      SUB   VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX - VY, VF = !borrow
      SUBN  VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VY - VX, VF = !borrow
      SYS   Addr            ; CHIP8
                                 ; Call CDP1802 code at Addr. This  
                                 ; is not implemented on emulators
      XOR   VX, VY          ; CHIP8, CHIP48, SCHIP10, SCHIP11
                                 ; Set VX = VX ^ VY, VF updates

    
#### Directives
* Var EQU Expr
* Var = Expr   
    Assign the value of expression to variable *Var*. 
* ALIGN [ON | OFF]  
    Turn on / off the word alignment.  
* DA String   
    Define string at current address. The string should be single quoted.
* DB Byte1 {, Byte2, Byte3, ... ByteN}     
    Define byte(s) at current address. 
* DS  N  
    Allocate N uninitialized bytes at current address.
* DW Word1 {, Word2, Word3, ... WordN}  
    Define word(s) at current address
* DEFINE COND     
    Define COND, making the symbol COND evaluates to TRUE.
* UNDEF COND  
    Undefine COND, making the symbol COND evaluates to FALSE.
* IFDEF COND  
    Disable further assembly if COND evaluates to FALSE.
* IFUND COND  
    Disable further assembly if COND evaluates to TRUE.
* ELSE   
    Flips further assembly according to the previous COND test.
* ENDIF    
    End of IFDEF/IFUND block. Back to normal assembly.
* ORG Addr  
    Set current assembly (in memory) address to Addr. Used to explicitly control program memory layout. Use with caution.
* USED [NO | ON | OFF | YES | Symbol]   
    ON - Turn on auto-use symbols from this line onward,which means all symbols defined will be considered as used automatically.
    OFF - Turn off auto-use symbols from this line onward,which means all symbols defined will not be considered as used automatically.
    NO - Turns on the Unused Symbol warnings.
    YES - Turns off the Unused Symbol warnings.
    Symbol - Mark the specific symbol as used thus suppressing the Unused Symbol warning for that symbol.

Note: directives such as XREF, END etc are recognized but simply ignored. 

## References

1. David Winter, CHIP-8 game source code and his documentations (CHIPPER.doc ) [[Link]](http://www.pong-story.com/chip8/)
2. Cowgod's CHIP-8 Reference [[Link]](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM)
3. Erik Bryntse' S-CHIP Reference [[Link]](http://devernay.free.fr/hacks/chip8/schip.txt)
4. CHIP-8 Pack, a compilation of works from various contributors, used extensively for testing. [[Link]](http://chip8.com/?page=109)
5. Mkoelew@yahoo.com's Fish'n'Chips Emulator [[Link]](http://www.chip8.com/downloads/FishNChips.zip)
6. Buzz sound comes from Online Tone Generator [[Link]](http://onlinetonegenerator.com/)

I used Cowgod's CHIP-8 documents ([[Link]](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM)) extensively as my main reference, and the programs shipped in CHIP-8 pack ([[Link]](http://chip8.com/?page=109)) as 
my tests for the emulator. 

For the assembler part, the main reference is David Winter's compilation of documentations especially CHIPPER.doc 
and CHIP8.doc.
