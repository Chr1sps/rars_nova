.eqv SYSCALL_BITMAP_DISPLAY		61
.eqv SYSCALL_RAND_SEED			40
.eqv SYSCALL_RAND_INT			41
.eqv SYSCALL_EXIT				93

.eqv WIDTH  100
.eqv HEIGHT 100
.eqv PIXELS 10000
.eqv BYTES  40000

.data
    .align 4
buffer:
    .space BYTES   # space for WIDTH*HEIGHT pixels (4 bytes per pixel)

.text
.globl main

.macro syscall (%number)
	li a7, %number
	ecall
.end_macro

# legend:
# s0 - width constant
# s1 - height constant
# t0 - image buffer pointer
# t1 - current width
# t2 - current height
main:
	li s0, WIDTH
	li s1, HEIGHT


	la t0, buffer

rand_seed:
	li a0, 0
	csrr a1, time
	syscall (SYSCALL_RAND_SEED)

height_loop_init:
	mv t2, zero

height_loop_body:
width_loop_init:
    mv t1, zero

width_loop_body:

randomize_value:

	li a0, 0
	syscall (SYSCALL_RAND_INT)

write_pixel:

	sw a0, 0(t0)

shift_image_pointer:
	addi t0, t0, 4

width_loop_end:
	addi t1, t1, 1
	bne t1, s0, width_loop_body
	
display_frame:

	la a0, buffer
	li a1, WIDTH
	li a2, HEIGHT
	syscall (SYSCALL_BITMAP_DISPLAY)

height_loop_end:
	addi t2, t2, 1
	bne t2, s1, height_loop_body

fin:
	mv zero, a0
	li a7, SYSCALL_EXIT
	ecall
