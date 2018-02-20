; Name: hello.asm
; Author: Jeffrey Bian
; Description: 
; 	This is a minimum hello world program for S-CHIP. Assemble with: 
; 		java -jar hello.asm -o hello.ch8
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