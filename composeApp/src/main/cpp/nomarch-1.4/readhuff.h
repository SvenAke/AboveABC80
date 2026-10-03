/* nomarch 1.0 - extract old `.arc' archives.
 * Copyright (C) 2001 Russell Marks. See main.c for license details.
 *
 * readhuff.h
 */

#ifdef __cplusplus
extern "C" {
#endif

extern unsigned char *convert_huff(unsigned char *data_in,
                                   unsigned long in_len,
                                   unsigned long orig_len);
extern unsigned long get_huff_output_length();

#ifdef __cplusplus
}
#endif
