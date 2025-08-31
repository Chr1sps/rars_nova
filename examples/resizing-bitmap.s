.eqv SYSCALL_BITMAP_DISPLAY		61
.eqv SYSCALL_RAND_SEED			40
.eqv SYSCALL_RAND_INT			41
.eqv SYSCALL_EXIT				93

.eqv MIN_SIZE 1
.eqv MAX_SIZE 200
.eqv MAX_BYTES  160000

.data
    .align 4
buffer:
    .space MAX_BYTES   # space for MAX_SIZE^2 pixels (4 bytes per pixel)

.text
.globl main

.macro syscall (%number)
	li a7, %number
	ecall
.end_macro

# legend:
# s0 - max size constant
# t0 - image buffer pointer
# t1 - current width
# t2 - current height
# t3 - current size
# t4 - current colour
main:

	li s0, MAX_SIZE

rand_seed:
	li a0, 0
	csrr a1, time
	syscall (SYSCALL_RAND_SEED)

size_loop_init:
	li t3, MIN_SIZE

size_loop_body:
randomize_value:

	li a0, 0
	syscall (SYSCALL_RAND_INT)
	mv t4, a0

image_pointer_init:
	la t0, buffer
	
height_loop_init:
	mv t2, zero

height_loop_body:
width_loop_init:
    mv t1, zero

width_loop_body:

write_pixel:
	sw t4, 0(t0)
	
shift_image_pointer:
	addi t0, t0, 4

width_loop_end:
	addi t1, t1, 1
	bne t1, t3, width_loop_body

display_frame:

	la a0, buffer
	mv a1, t3
	mv a2, t3
	syscall (SYSCALL_BITMAP_DISPLAY)

height_loop_end:
	addi t2, t2, 1
	bne t2, t3, height_loop_body

size_loop_end:
	addi t3, t3, 1
	bne t3, s0, size_loop_body

fin:
	mv zero, a0
	li a7, SYSCALL_EXIT
	ecall
