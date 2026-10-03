/* nomarch 1.3 - extract old `.arc' archives.
 * Copyright (C) 2001,2002 Russell Marks. See main.c for license details.
 *
 * readlzw.h
 */

#ifdef __cplusplus
extern "C" {
#endif

extern unsigned char *convert_lzw_dynamic(unsigned char *data_in,
                                          int bits,int use_rle,
                                          unsigned long in_len,
                                          unsigned long orig_len);
extern unsigned long get_lzw_output_length();

#ifdef __cplusplus
}
#endif
