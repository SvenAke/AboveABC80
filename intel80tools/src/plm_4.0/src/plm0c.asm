CSEG
	public gnxtch
	public ingetc
	extrn inChrp
	extrn GetLin
	extrn lastch
	extrn Readf
	extrn oCurCh
	extrn oLstCh
	extrn srcFil
	extrn inbuf
	public trunc
	public rsrcln

gnxtch:	lhld	inChrp
	inx	h
	shld	inChrp

L58E6:
	mov	a, m
	cpi	0Ah
	jnz	L58F5
	call	GetLin
	lhld	inChrp
	jmp	L58E6

L58F5:	sta	lastch
	ret

; inSrcCh
; get next char	into m[inChr$p]
; char is 0x81 on EOF, cr is discarded
; Attributes: hidden

ingetc:	lhld	oCurCh
	lxi	d, oLstCh	; see if characters left in buffer
	ldax	d
	cmp	l
	jnz	L593A
	inx	d
	ldax	d
	cmp	h
	jnz	L593A
	lxi	h, 0
	shld	oCurCh
	lxi	b, srcFil
	push	b
	lxi	b, inbuf
	push	b
	lxi	b, 200h
	lxi	d, oLstCh
	call	Readf
	lhld	oLstCh
	mov	a, h
	ora	l
	jnz	L5930
	lhld	inChrp
	mvi	a, 81h
	mov	m, a
	ret

L5930:	dcx	h
	shld	oLstCh
	lhld	oCurCh
	jmp	L593E

L593A:	inx	h
	shld	oCurCh

L593E:	lxi	b, inbuf
	dad	b
	mvi	a, 7Fh
	ana	m
	cpi	0Dh
	jz	ingetc		; inSrcCh
				; get next char	into m[inChr$p]
				; char is 0x81 on EOF, cr is discarded
	lhld	inChrp
	mov	m, a
	ret

	DSEG
i:	ds 1
trunc:	db 0

	CSEG

rSrcLn:
	lxi	h, i
	mvi	m, 7Fh		; max 127 chars
	lhld	inChrp
	mov	a, m

L5958:	cpi	0Ah
	rz
	cpi	81h
	rz
	lda	i
	dcr	a
	jz	L5972
	sta	i
	inx	h
	shld	inChrp
	call	ingetc		; inSrcCh
				; get next char	into m[inChr$p]
				; char is 0x81 on EOF, cr is discarded
	jmp	L5958

L5972:
	mvi	a, 0FFh
	sta	trunc

L5977:
	call	ingetc		; inSrcCh
				; get next char	into m[inChr$p]
				; char is 0x81 on EOF, cr is discarded
	cpi	0Ah		; waste	rest of	line
	rz
	cpi	81h
	rz
	jmp	L5977
end
