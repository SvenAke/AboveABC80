#include "../native-lib.h"
#include <jni.h>
#ifdef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <fcntl.h>
#include <io.h>
#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

inline FILE *fmemopen(void *buf, size_t size, const char *mode) {
  char tempPath[MAX_PATH];
  char tempFileName[MAX_PATH];

  // 1. Hämta Windows temporära mapp (t.ex. C:\Users\Namn\AppData\Local\Temp\)
  if (GetTempPathA(MAX_PATH, tempPath) == 0)
    return NULL;

  // 2. Skapa ett unikt filnamn i den mappen
  if (GetTempFileNameA(tempPath, "KMP_TMP", 0, tempFileName) == 0)
    return NULL;

  // 3. Öppna filen med speciella flaggor som tvingar Windows att hålla den i
  // RAM (caching) samt raderar den automatiskt när filen stängs.
  HANDLE hFile = CreateFileA(
      tempFileName, GENERIC_READ | GENERIC_WRITE, 0, NULL, CREATE_ALWAYS,
      FILE_ATTRIBUTE_TEMPORARY | FILE_FLAG_DELETE_ON_CLOSE, NULL);

  if (hFile == INVALID_HANDLE_VALUE)
    return NULL;

  // 4. Konvertera Windows HANDLE till ett standard C-filsystem descriptor (fd)
  int fd = _open_osfhandle((intptr_t)hFile, _O_RDWR | _O_BINARY);
  if (fd == -1) {
    CloseHandle(hFile);
    return NULL;
  }

  // 5. Koppla fd till en vanlig FILE* ström
  FILE *f = _fdopen(fd, "w+b");
  if (!f) {
    _close(fd);
    return NULL;
  }

  // 6. Om en existerande buffer skickades med, kopiera in datan så den går att
  // läsa
  if (buf && size > 0) {
    fwrite(buf, 1, size, f);
    rewind(f); // Flytta tillbaka pekaren till början av filen för läsning
  }

  return f;
}
#endif
#else
#include <cstdio> // För Android (fmemopen)
#endif
#include "nomarch.h" // Ensure convert_rle, convert_huff, convert_lzw_dynamic are defined
#include <cctype>
#include <cerrno>
#include <climits>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <algorithm>
#include <vector>
#include <sys/types.h>
#ifdef _WIN32
#include <sys/utime.h>
#define utime _utime
#define utimbuf _utimbuf
#else
#include <utime.h>
#endif

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

/* nomarch 1.4 - extract old `.arc' archives.
 * Copyright (C) 2001-2006 Russell Marks.
 *
 * main.c - most of the non-extraction stuff.
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

char *archive_filename = nullptr;
char **archive_matchers = nullptr; /* NULL, or points to argv+N */
int num_matchers = 0;

int opt_list = 0, opt_print = 0, opt_test = 0, opt_verbose = 0;
int opt_preserve_case = 0;

int quiet = 0;

struct archived_file_header_tag {
  unsigned char method;
  char name[13];
  unsigned long compressed_size; /* 4 bytes in file */
  unsigned int date, time, crc;  /* 2 bytes each in file */
  unsigned long orig_size;       /* 4 bytes in file */

  int has_crc;
};

struct archived_file_result {
  char name[13];
  char *content;
  unsigned long content_size;
};

static unsigned long crc32_bytes(const unsigned char *data, size_t length) {
  unsigned long crc = 0xFFFFFFFFu;
  for (size_t index = 0; index < length; index++) {
    crc ^= data[index];
    for (int bit = 0; bit < 8; bit++)
      crc = (crc >> 1) ^ (0xEDB88320u & (-(long)(crc & 1u)));
  }
  return crc ^ 0xFFFFFFFFu;
}

static unsigned char *decompress_crunch_80un(
    const unsigned char *data, size_t length, unsigned long capacity,
    unsigned long *output_length) {
  if (length < 4 || data[0] != 0x76 || data[1] != 0xFE)
    return nullptr;

  size_t position = 2;
  while (position < length && data[position] != 0)
    position++;
  if (position >= length || position + 5 > length)
    return nullptr;
  position++;

  const unsigned char siglevel = data[position + 1];
  const unsigned char errdetect = data[position + 2];
    logd("ZZZ: 80un header filename_end=%zu reflevel=%02X siglevel=%02X errdetect=%02X spare=%02X",
      position, data[position], siglevel, errdetect, data[position + 3]);
  position += 4;
  if (errdetect > 0)
    position += 2;
  if (position >= length)
    return nullptr;

  struct MsbReader {
    const unsigned char *data;
    size_t length;
    size_t position;
    unsigned int buffer = 0;
    unsigned int bits = 0;
    int read(int width) {
      while (bits < (unsigned int)width) {
        if (position >= length)
          return -1;
        buffer = (buffer << 8) | data[position++];
        bits += 8;
      }
      const int value = (buffer >> (bits - width)) & ((1 << width) - 1);
      bits -= width;
      if (bits == 0)
        buffer = 0;
      else
        buffer &= (1u << bits) - 1u;
      return value;
    }
  } reader{data, length, position};

  const bool v2 = siglevel >= 0x20;
  const int initial_bits = v2 ? 9 : 12;
  int code_bits = initial_bits;
  unsigned int next_code = 260;
  std::vector<std::vector<unsigned char>> dictionary(4096);
  for (unsigned int value = 0; value < 256; value++)
    dictionary[value].push_back((unsigned char)value);

  std::vector<unsigned char> decoded;
  std::vector<unsigned char> previous;
  while (decoded.size() < capacity) {
    if (v2 && next_code == (1u << code_bits) - 1 && code_bits < 12)
      code_bits++;
    const int code = reader.read(code_bits);
    if (code < 0)
      break;
    if (v2 && (code == 258 || code == 259))
      continue;
    if (code == 256)
      break;
    if (code == 257) {
      for (auto &entry : dictionary)
        entry.clear();
      for (unsigned int value = 0; value < 256; value++)
        dictionary[value].push_back((unsigned char)value);
      next_code = 260;
      code_bits = initial_bits;
      previous.clear();
      continue;
    }
    if (code < 0 || code >= (int)dictionary.size())
      break;

    std::vector<unsigned char> current;
    if (!dictionary[code].empty())
      current = dictionary[code];
    else if ((unsigned int)code == next_code && !previous.empty()) {
      current = previous;
      current.push_back(previous.front());
    } else {
      break;
    }

    decoded.insert(decoded.end(), current.begin(), current.end());
    if (!previous.empty() && next_code < dictionary.size()) {
      dictionary[next_code] = previous;
      dictionary[next_code].push_back(current.front());
      next_code++;
    }
    previous = std::move(current);
  }

  std::vector<unsigned char> result;
  result.reserve(decoded.size());
  const size_t raw_lzw_length = decoded.size();
  bool has_rle = std::find(decoded.begin(), decoded.end(), 0x90) != decoded.end();
  if (has_rle) {
    unsigned char previous_byte = 0;
    for (size_t index = 0; index < decoded.size(); index++) {
      unsigned char value = decoded[index];
      if (value != 0x90) {
        result.push_back(value);
        previous_byte = value;
      } else if (++index >= decoded.size()) {
        result.push_back(0x90);
      } else if (decoded[index] == 0) {
        result.push_back(0x90);
        previous_byte = 0x90;
      } else {
        result.insert(result.end(), decoded[index], previous_byte);
      }
    }
  } else {
    result = std::move(decoded);
  }

  logd("ZZZ: 80un raw_lzw_length=%zu rle=%d final_length=%zu",
      raw_lzw_length, has_rle ? 1 : 0, result.size());

  if (result.empty())
    return nullptr;
  unsigned char *output = (unsigned char *)malloc(result.size());
  if (!output)
    return nullptr;
  memcpy(output, result.data(), result.size());
  *output_length = (unsigned long)result.size();
  logd("ZZZ: 80un output crc32=%08lX length=%lu",
       crc32_bytes(output, result.size()), *output_length);
  return output;
}

static unsigned char *decompress_crunch_reference(
    const unsigned char *input, size_t input_length, int max_bits,
    bool block_compression, unsigned long output_capacity,
    unsigned long *output_length) {
  struct BitReader {
    const unsigned char *data;
    size_t length;
    size_t offset = 0;
    unsigned char block[16] = {};
    unsigned int block_bits = 0;
    unsigned int block_offset = 0;
    unsigned int block_width = 0;

    int read(int bits) {
      if (block_width != (unsigned int)bits || block_offset >= block_bits) {
        if (offset >= length)
          return -1;
        const size_t block_size =
            std::min<size_t>((size_t)bits, length - offset);
        memcpy(block, data + offset, block_size);
        offset += block_size;
        block_width = bits;
        block_offset = 0;
        block_bits = (unsigned int)(block_size * 8) - (bits - 1);
        if (block_bits == 0)
          return -1;
      }
      int value = 0;
      for (int bit = 0; bit < bits; bit++) {
        const unsigned int position = block_offset + bit;
        if (block[position / 8] & (1u << (position % 8)))
          value |= 1 << bit;
      }
      block_offset += bits;
      return value;
    }
  };

  if (max_bits < 9 || max_bits > 16)
    return nullptr;

  BitReader reader{input, input_length};
  std::vector<unsigned char> output;
  output.reserve(output_capacity < 65536 ? output_capacity : 65536);
  std::vector<unsigned short> prefix(1u << max_bits);
  std::vector<unsigned char> suffix(1u << max_bits);
  std::vector<unsigned char> stack(8000);
  unsigned int code_bits = 9;
  unsigned int max_code = (1u << code_bits) - 1;
  const unsigned int max_code_value = 1u << max_bits;
  unsigned int free_code = block_compression ? 257 : 256;
  bool clear_pending = false;
  unsigned int rle_state = 0;

  auto emit = [&](unsigned char value) {
    if (!block_compression) {
      output.push_back(value);
      return;
    }
    unsigned int count;
    unsigned int byte = rle_state;
    if (rle_state & 0x100) {
      if (value == 0 || (value == 1 && (rle_state & 0x80000000u))) {
        byte = 0x90;
        count = 1;
      } else {
        byte &= 0xff;
        count = value - 1;
      }
    } else if (value == 0x90) {
      byte = rle_state | 0x100;
      count = 0;
    } else {
      byte = value;
      count = 1;
    }
    rle_state = byte;
    while (count-- && output.size() < output_capacity)
      output.push_back((unsigned char)byte);
  };

  auto emit_string = [&](unsigned int code, unsigned char &first) -> bool {
    size_t stack_size = 0;
    while (code >= 256) {
      if (code >= free_code || stack_size >= stack.size())
        return false;
      stack[stack_size++] = suffix[code];
      code = prefix[code];
    }
    if (code > 255)
      return false;
    first = (unsigned char)code;
    emit(first);
    while (stack_size)
      emit(stack[--stack_size]);
    return true;
  };

  int code = reader.read(code_bits);
  if (code < 0 || code > 255)
    return nullptr;
  unsigned int old_code = (unsigned int)code;
  unsigned char first_char = (unsigned char)code;
  emit(first_char);

  while (output.size() < output_capacity) {
    code = reader.read(code_bits);
    if (code < 0)
      break;
    if (block_compression && code == 256) {
      clear_pending = true;
      free_code = 256;
      code_bits = 9;
      max_code = (1u << code_bits) - 1;
      code = reader.read(code_bits);
      if (code < 0)
        break;
    }
    if (clear_pending) {
      clear_pending = false;
      free_code = 257;
    }

    unsigned int in_code = (unsigned int)code;
    if ((unsigned int)code < free_code) {
      if (!emit_string((unsigned int)code, first_char))
        return nullptr;
    } else if ((unsigned int)code == free_code) {
      emit_string(old_code, first_char);
      emit(first_char);
    } else {
      return nullptr;
    }

    if (free_code < max_code_value) {
      prefix[free_code] = (unsigned short)old_code;
      suffix[free_code] = first_char;
      free_code++;
      if (free_code > max_code && code_bits < max_bits) {
        code_bits++;
        max_code = (1u << code_bits) - 1;
      }
    }
    old_code = in_code;
  }

  if (output.empty())
    return nullptr;
  unsigned char *result = (unsigned char *)malloc(output.size());
  if (!result)
    return nullptr;
  memcpy(result, output.data(), output.size());
  *output_length = (unsigned long)output.size();
  return result;
}

void free_archived_file_results(struct archived_file_result *results,
                                size_t result_count) {
  for (size_t index = 0; index < result_count; index++)
    free(results[index].content);
  free(results);
}

struct archived_file_result *make_error_result(const char *message,
                                               size_t *result_count) {
  struct archived_file_result *result =
      (struct archived_file_result *)calloc(1, sizeof(*result));
  if (result == nullptr)
    return nullptr;

  memcpy(result->name, "--------.---", 12);
  result->content_size = strlen(message);
  result->content = (char *)malloc(result->content_size + 1);
  if (result->content == nullptr) {
    free(result);
    return nullptr;
  }

  memcpy(result->content, message, result->content_size + 1);
  *result_count = 1;
  return result;
}

int maybe_downcase(int c) {
  if (opt_preserve_case)
    return (c);

  return (tolower(c));
}

/* there is no overall header for the archive, but there's a header
 * for each file stored in it.
 * returns zero if we couldn't get a header.
 * NB: a header with method zero marks EOF.
 */
int read_file_header(FILE *in, struct archived_file_header_tag *hdrp) {
  unsigned char
      buf[4 + 2 + 2 + 2 + 4]; /* used to read size1/date/time/crc/size2 */
  int bufsiz = sizeof(buf);
  int method_high;
  int c, f;
  // logd("nomarch: reading header...");
  hdrp->method = 0xff;
  if (fgetc(in) != 0x1a)
    return (0);

  if ((c = fgetc(in)) == EOF)
    return (0);

  /* allow for the spark archive variant's alternate method encoding */
  method_high = (c >> 7);
  hdrp->method = (c & 127);

  /* zero if EOF, which also means no further `header' */
  if (hdrp->method == 0)
    return (1);

  /* `old' version of uncompressed storage was weird */
  if (hdrp->method == 1)
    bufsiz -= 4; /* no `orig_size' field */

  if (fread(hdrp->name, 1, sizeof(hdrp->name), in) != sizeof(hdrp->name) ||
      fread(buf, 1, bufsiz, in) != bufsiz)
    return (0);

  /* extract the bits from buf */
  hdrp->compressed_size =
      (buf[0] | (buf[1] << 8) | (buf[2] << 16) | (buf[3] << 24));
  hdrp->date = (buf[4] | (buf[5] << 8));
  hdrp->time = (buf[6] | (buf[7] << 8));
  hdrp->crc = (buf[8] | (buf[9] << 8)); /* yes, only 16-bit CRC */
  hdrp->has_crc = 1;
  if (hdrp->method == 1)
    hdrp->orig_size = hdrp->compressed_size;
  else
    hdrp->orig_size =
        (buf[10] | (buf[11] << 8) | (buf[12] << 16) | (buf[13] << 24));

  /* make *sure* name is asciiz */
  hdrp->name[12] = 0;

  /* strip top bits, and lowercase the name */
  for (f = 0; f < strlen(hdrp->name); f++)
    hdrp->name[f] = maybe_downcase(hdrp->name[f] & 127);

  /* lose the possible extra bytes in spark archives */
  if (method_high) {
    if (fread(buf, 1, 12, in) != 12)
      return (0);

    /* has a weird recursive-.arc file scheme for subdirs,
     * and since these are supposed to be dealt with inline
     * (though they aren't here) the CRCs could be junk.
     * So check for it being marked as a stored dir.
     */
    if (hdrp->method == 2 && buf[3] == 0xff && buf[2] == 0xfd && buf[1] == 0xdc)
      hdrp->has_crc = 0;
  }

  return (1);
}

/* self-extracting archives, for both CP/M and MS-DOS, have up to
 * 3 bytes before the initial ^Z. This skips those if present.
 * Returns zero if there's an input error, or we fail to find ^Z in
 * the first 4 bytes.
 *
 * This should work with self-extracting archives for CP/M
 * (e.g. unarc16.ark), and those produced by `arc'. It won't work with
 * pkpak self-extracting archives, for two reasons:
 *
 * - they have 4 bytes before the ^Z.
 * - they have an EOF member (zero byte) right after that, giving you
 *   an archive containing no files (grrr).
 *
 * So I thought it was better (and less confusing) to effectively stick
 * with the not-an-archive error for those. :-)
 */
int skip_sfx_header(FILE *in) {
  int c, f, got = 0;
  // logd("nomarch: start skipping sfx header...");
  for (f = 0; f < 4; f++) {
    // logd("nomarch: skipping %d...", f);
    if ((c = fgetc(in)) == EOF) {
      // logd("nomarch: error reading file header\n");
      return (0);
    }
    // logd("nomarch: %d", c);
    if (c == 0x1a) {
      got = 1;
      ungetc(c, in);
      break;
    }
  }
  // logd("nomarch: skipping sfx header...%d", got);
  return (got);
}

/* read file data, assuming header has just been read from in
 * and hdrp's data matches it. Caller is responsible for freeing
 * the memory allocated.
 * Returns NULL for file I/O error only; OOM is fatal (doesn't return).
 */
unsigned char *read_file_data(FILE *in, struct archived_file_header_tag *hdrp) {
  unsigned char *data;
  int siz = hdrp->compressed_size;

  if ((data = (unsigned char *)malloc(siz)) == nullptr)
    return nullptr;

  if (fread(data, 1, siz, in) != siz) {
    free(data);
    data = nullptr;
  }

  return (data);
}

/* variant which just skips past the data */
int skip_file_data(FILE *in, struct archived_file_header_tag *hdrp) {
  int siz = hdrp->compressed_size;
  int f;

  for (f = 0; f < siz; f++)
    if (fgetc(in) == EOF)
      return (0);

  return (1);
}

/* convert MS-DOS time format to string */
char *mkdatetimestr(unsigned int hdate, unsigned int htime) {
  static char buf[128];
  int day, month, year, hour, min;

  year = 1980 + (hdate >> 9);
  month = ((hdate >> 5) & 15);
  day = (hdate & 31);
  hour = (htime >> 11);
  min = ((htime >> 5) & 63);
  /* seconds ignored */

  sprintf(buf, "%4d-%02d-%02d %02d:%02d", year, month, day, hour, min);
  return (buf);
}

/* convert to time_t */
time_t mkdatetimet(unsigned int hdate, unsigned int htime) {
  struct tm tms;

  tms.tm_year = 80 + (hdate >> 9);
  tms.tm_mon = ((hdate >> 5) & 15) - 1;
  tms.tm_mday = (hdate & 31);
  tms.tm_hour = (htime >> 11);
  tms.tm_min = ((htime >> 5) & 63);
  tms.tm_sec = (htime & 31) * 2;
  tms.tm_isdst = -1; /* i.e. unknown */

  return (mktime(&tms));
}

/* simple wildcard-matching code. It implements shell-like
 * `*' and `?', but not `[]' or `{}'.
 * XXX might be nice to replace this with something better :-)
 */
int is_match(char *filename, char *wildcard) {
  char *ptr = filename, *match = wildcard;
  char *tmp, *tmp2;
  int old;
  int ok = 1;

  while (*ptr && *match && ok) {
    switch (*match) {
    case '*':
      /* need to check that everything up to the next * or ? matches
       * at some point from here onwards */

      /* skip the `*', and any * or ? following */
      while (*match == '*' || *match == '?')
        match++;

      /* find the next * or ?, or the end of the name */
      tmp = strchr(match, '*');
      if (tmp == NULL)
        tmp = strchr(match, '?');
      if (tmp == NULL)
        tmp = match + strlen(match);
      tmp--;

      /* if *match is NUL, the * was the last char, so it's already matched
       * if tmp+1-match>strlen(ptr) then it can't possibly match
       */
      if (*match == 0)
        ptr += strlen(ptr); /* just `*', so skip to end */
      else {
        if (tmp + 1 - match > strlen(ptr))
          ok = 0;
        else {
          /* go forward through the string, attempting to match the text
           * from match to tmp (inclusive) each time
           */
          old = tmp[1];
          tmp[1] = 0;
          if ((tmp2 = strstr(ptr, match)) == NULL)
            ok = 0; /* didn't match */
          else
            ptr = tmp2;

          tmp[1] = old;
        }
      }
      break;

    case '?':
      match++;
      ptr++; /* always matches */
      break;

    default:
      if (*match != *ptr)
        ok = 0;
      else {
        match++;
        ptr++;
      }
    }
  }

  if (*ptr || *match)
    ok = 0; /* if any text left, it didn't match */

  return (ok);
}

/* see if file matches any of the match strings specified
 * on the cmdline.
 */
int file_matches(char *filename) {
  int f;

  if (!num_matchers)
    return (1);

  for (f = 0; f < num_matchers; f++)
    if (is_match(filename, archive_matchers[f]))
      return (1);

  return (0);
}

int arc_list(int verbose) {
#define NUM_METHODSTR 10
  const char *methodstr[NUM_METHODSTR] = {
      "EOF",    /* should never be shown */
      "Stored", /* adopting a zip term here :-) */
      "Stored",   "Packed",
      "Squeezed", "crunched", /* lowercase `important' */
      "crunched", "crunched",
      "Crunched", "Squashed"};
  FILE *in;
  struct archived_file_header_tag hdr;
  int done = 0;

  if ((in = fopen(archive_filename, "rb")) == NULL)
    // logd("nomarch: %s\n", strerror(errno)), exit(1);

  if (!skip_sfx_header(in) || !read_file_header(in, &hdr))
    // logd("nomarch: bad first header - not a .arc file?\n"), 
    return(1);
  do {
    if (hdr.method == 0) /* EOF */
    {
      done = 1;
      continue;
    }

    if (!skip_file_data(in, &hdr))
      // logd("nomarch: error reading data (hit EOF)\n"), exit(1);

    if (file_matches(hdr.name)) {
      if (verbose) {
        printf("%-13s\t%d (%s)\t%9ld %9ld  %s  ", hdr.name, hdr.method,
               hdr.method < NUM_METHODSTR
                   ? methodstr[hdr.method]
                   : (hdr.method == 127 ? "Compress" : "unknown"),
               hdr.compressed_size, hdr.orig_size,
               mkdatetimestr(hdr.date, hdr.time));
        if (hdr.has_crc)
          printf("%04X", hdr.crc);
        putchar('\n');
      } else
        printf("%-13s\t%9ld   %s\n", hdr.name, hdr.orig_size,
               mkdatetimestr(hdr.date, hdr.time));
    }

    /* read header ready for next file */
    if (!read_file_header(in, &hdr)) {
      fclose(in);
      return (1);
    }
  } while (!done);

  fclose(in);

  return (0);
}

struct archived_file_result *arc_extract_or_test(int test_only, FILE *in,
                                                 size_t *result_count) {
  struct archived_file_header_tag hdr{};
  int done = 0;
  unsigned char *data, *orig_data;
  int supported;
  struct archived_file_result *results = nullptr;
  size_t results_size = 0;

  (void)test_only;
  *result_count = 0;
  if (in == nullptr)
    return make_error_result("nomarch: could not open archive buffer",
                             result_count);

  if (!skip_sfx_header(in) || !read_file_header(in, &hdr)) {
    fclose(in);
    return make_error_result("nomarch: bad first header - not a .arc file",
                             result_count);
  }
  do {
    if (hdr.method == 0) /* EOF */
    {
      done = 1;
      continue;
    }

    if (!file_matches(hdr.name)) {
      if (!skip_file_data(in, &hdr)) {
        fclose(in);
        free_archived_file_results(results, results_size);
        return make_error_result("nomarch: error reading data (hit EOF)",
                                 result_count);
      }
    } else {
      if ((data = read_file_data(in, &hdr)) == NULL) {
        fclose(in);
        free_archived_file_results(results, results_size);
        return make_error_result("nomarch: error reading data (hit EOF)",
                                 result_count);
      }

      /* decompress file data */
      if (!quiet) {
        // logd("%-12s\t", hdr.name);
        fflush(stdout);
      }

      orig_data = NULL;
      supported = 0;

      /* FWIW, most common types are (by far) 8/9 and 2.
       * (127 is the most common in Spark archives, but only those.)
       * 3 and 4 crop up occasionally. 5 and 6 are very, very rare.
       * And I don't think I've seen a *single* file with 1 or 7 yet.
       */
      switch (hdr.method) {
      case 1:
      case 2: /* no compression */
        supported = 1;
        orig_data = data;
        break;

      case 3: /* "packed" (RLE) */
        supported = 1;
        orig_data = convert_rle(data, hdr.compressed_size, hdr.orig_size);
        break;

      case 4: /* "squeezed" (Huffman, like CP/M `SQ') */
        supported = 1;
        orig_data = convert_huff(data, hdr.compressed_size, hdr.orig_size);
        break;

      case 5: /* "crunched" (12-bit static LZW) */
        supported = 1;
        orig_data =
            convert_lzw_dynamic(data, 0, 0, hdr.compressed_size, hdr.orig_size);
        break;

      case 6: /* "crunched" (RLE+12-bit static LZW) */
        supported = 1;
        orig_data =
            convert_lzw_dynamic(data, 0, 1, hdr.compressed_size, hdr.orig_size);
        break;

      case 7: /* PKPAK docs call this one "internal to SEA" */
        /* it looks like this one was only used by a development version
         * of SEA ARC, so chances are it can be safely ignored.
         * OTOH, it's just method 6 with a slightly different hash,
         * so I presume it wouldn't be *that* hard to add... :-)
         */
        break;

      case 8: /* "Crunched" [sic]
               * (RLE+9-to-12-bit dynamic LZW, a *bit* like GIF) */
        supported = 1;
        orig_data = convert_lzw_dynamic(data, 12, 1, hdr.compressed_size,
                                        hdr.orig_size);
        break;

      case 9: /* "Squashed" (9-to-13-bit, no RLE) */
        supported = 1;
        orig_data = convert_lzw_dynamic(data, 13, 0, hdr.compressed_size,
                                        hdr.orig_size);
        break;

      case 127: /* "Compress" (9-to-16-bit, no RLE) ("Spark" only) */
        supported = 1;
        orig_data = convert_lzw_dynamic(data, 16, 0, hdr.compressed_size,
                                        hdr.orig_size);
        break;
      }

      /* there was a `pak 2.0' which added a type 10 ("distill"), but I don't
       * plan to support that unless there's some desperate need for it.
       */

      if (orig_data == NULL) {
        free(data);
        fclose(in);
        free_archived_file_results(results, results_size);
        return make_error_result(
            supported ? "nomarch: error decompressing file"
                      : "nomarch: unsupported compression method",
            result_count);
      } else {
        char *ptr;
        int content_uses_input = (orig_data == data);

        /* CP/M stuff in particular likes those slashes... */
        while ((ptr = strchr(hdr.name, '/')) != NULL)
          *ptr = '_';

        struct archived_file_result *new_results =
            (struct archived_file_result *)realloc(
                results, (results_size + 1) * sizeof(*results));
        if (new_results == nullptr) {
          if (content_uses_input)
            free(data);
          else {
            free(orig_data);
            free(data);
          }
          free_archived_file_results(results, results_size);
          fclose(in);
          return make_error_result("nomarch: out of memory", result_count);
        }

        results = new_results;
        memcpy(results[results_size].name, hdr.name,
               sizeof(results[results_size].name));
        results[results_size].content = (char *)orig_data;
        results[results_size].content_size = hdr.orig_size;
        results_size++;

        if (content_uses_input)
          data = nullptr;
      }

      free(data);
    }

    /* read header ready for next file */
    if (!read_file_header(in, &hdr)) {
      fclose(in);
      free_archived_file_results(results, results_size);
      return make_error_result("nomarch: error reading record header",
                               result_count);
    }
  } while (!done);

  fclose(in);

  *result_count = results_size;
  return (results);
}

void usage_help(void) {
  // logd("nomarch %s - copyright (c) 2001-2006 Russell Marks.\n", NOMARCH_VER);
  logd(
      "\n"
      "usage: nomarch [-hlptUv] [archive.arc] [match1 [match2 ...]]\n"
      "\n"
      "	-h	this usage help.\n"
      "	-l	list contents of archive.\n"
      "	-p	extract to standard output, rather than to separate files.\n"
      "	-t	test archive (i.e. check file CRCs).\n"
      "	-U	use uppercase filenames (preserve case).\n"
      "	-v	give verbose listing (when used with `-l').\n"
      "\n"
      "  archive.arc	the .arc or .ark file to list/extract/test.\n"
      "		(The default action is to extract the files.)\n"
      "\n"
      "  match1 etc.	zero or more wildcards, for files to "
      "list/extract/test;\n"
      "		the file is processed if it matches any one of these.\n"
      "		(If *no* wildcards are specified, all files match.)\n"
      "		Wildcard operators supported are shell-like `*' and `?'.\n");
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_aboveware_abovecpm_NativeLib_extractArkArchiveNative(
    JNIEnv *env, jobject, jbyteArray ark_data) {
  // 1. Get a pointer to the Kotlin byte array data
  jsize ark_size = (*env).GetArrayLength(ark_data);
  jbyte *buffer = (*env).GetByteArrayElements(ark_data, nullptr);
  if (!buffer)
    return nullptr;

  auto *ptr = (unsigned char *)buffer;
  // logd("Extracting archive...");
  FILE *in = fmemopen(ptr, ark_size ? ark_size : 1, "rb");
  // logd("Opened archive...%p", in);
  size_t result_count = 0;
  struct archived_file_result *results = arc_extract_or_test(0, in, &result_count);

  // Always release the input array elements to avoid JNI memory leaks
  (*env).ReleaseByteArrayElements(ark_data, buffer, JNI_ABORT);
  size_t packed_size = sizeof(uint32_t);
  bool valid_size = result_count <= UINT32_MAX;
  for (size_t index = 0; valid_size && index < result_count; index++) {
    if (results[index].content_size > UINT32_MAX ||
        packed_size > SIZE_MAX - 13 - sizeof(uint32_t) -
                           results[index].content_size)
      valid_size = false;
    else
      packed_size += 13 + sizeof(uint32_t) + results[index].content_size;
  }

  jbyteArray result_array = nullptr;
  if (valid_size && packed_size <= INT_MAX) {
    unsigned char *packed_data = (unsigned char *)malloc(packed_size);
    if (packed_data != nullptr) {
      size_t offset = 0;
      uint32_t packed_count = (uint32_t)result_count;
      memcpy(packed_data + offset, &packed_count, sizeof(packed_count));
      offset += sizeof(packed_count);

      for (size_t index = 0; index < result_count; index++) {
        uint32_t content_size = (uint32_t)results[index].content_size;
        memcpy(packed_data + offset, results[index].name, 13);
        offset += 13;
        memcpy(packed_data + offset, &content_size, sizeof(content_size));
        offset += sizeof(content_size);
        memcpy(packed_data + offset, results[index].content, content_size);
        offset += content_size;
      }

      result_array = (*env).NewByteArray((jsize)packed_size);
      if (result_array)
        (*env).SetByteArrayRegion(result_array, 0, (jsize)packed_size,
                                  (jbyte *)packed_data);
      free(packed_data);
    }
  }

  free_archived_file_results(results, result_count);
  return result_array;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_aboveware_abovecpm_NativeLib_decompressSqueezedNative(
    JNIEnv *env, jobject, jbyteArray compressed_data) {
  const jsize input_size = env->GetArrayLength(compressed_data);
  logd("ZZZ: decompress start, input_size=%d", (int)input_size);
  jbyte *input_bytes = env->GetByteArrayElements(compressed_data, nullptr);
  logd("ZZZ: GetByteArrayElements returned %p", (void *)input_bytes);
  if (input_bytes == nullptr || input_size == 0) {
    logd("ZZZ: abort, empty input or null JNI buffer");
    return nullptr;
  }

  auto *input = (unsigned char *)input_bytes;
  unsigned char *decoded = nullptr;
  unsigned long decoded_length = 0;
  const auto output_capacity =
      (unsigned long)((input_size > 1024 ? input_size * 64 : 65536));
  logd("ZZZ: output_capacity=%lu", output_capacity);

  if (input_size > 1 && input[0] == 0x76 && input[1] == 0xFE) {
    logd("ZZZ: trying exact 80un Crunch decoder");
    decoded = decompress_crunch_80un(input, input_size, output_capacity,
                                      &decoded_length);
    if (decoded != nullptr) {
      logd("ZZZ: exact 80un decoder succeeded, length=%lu", decoded_length);
      env->ReleaseByteArrayElements(compressed_data, input_bytes, JNI_ABORT);
      jbyteArray result = env->NewByteArray((jsize)decoded_length);
      if (result != nullptr)
        env->SetByteArrayRegion(result, 0, (jsize)decoded_length,
                                (jbyte *)decoded);
      free(decoded);
      return result;
    }
    logd("ZZZ: exact 80un Crunch decoder failed; keeping diagnostic fallbacks");
  }

  // Standard SQ/ZZZ files have: magic (2), CP/M filename (8),
  // NUL separator (1), then the Huffman tree.
  jsize tree_offset = 12;
  logd("ZZZ: header bytes=%02X %02X name='%.*s' separator=%02X",
       input[0], input[1], 8, (char *)(input + 2), input[11]);
  if (input_size < tree_offset + 2 || input[0] != 0x76 ||
      (input[1] != 0xFE && input[1] != 0xFF) || input[11] != 0) {
    logd("ZZZ: invalid SQ header");
    env->ReleaseByteArrayElements(compressed_data, input_bytes, JNI_ABORT);
    return nullptr;
  }

    // SQ stores the node count in one byte, while convert_huff expects a
    // little-endian 16-bit count before the same 4-byte node records.
    const unsigned int nodes = input[tree_offset];
    logd("ZZZ: tree_offset=%d nodes=%u tree_bytes=%u payload_bytes=%d",
      (int)tree_offset, nodes, nodes * 4 + 1,
      (int)input_size - (int)tree_offset - 1 - (int)(nodes * 4));
  if (nodes == 0 || nodes > 4096 ||
     tree_offset + 1 + nodes * 4 > input_size) {
    logd("ZZZ: invalid Huffman tree, nodes=%u input_size=%d", nodes,
         (int)input_size);
    env->ReleaseByteArrayElements(compressed_data, input_bytes, JNI_ABORT);
    return nullptr;
  }

    const size_t huff_input_length = (size_t)input_size - tree_offset;
    unsigned char *huff_input = (unsigned char *)malloc(huff_input_length + 1);
    if (huff_input == nullptr) {
      logd("ZZZ: failed to allocate normalized Huffman input");
      env->ReleaseByteArrayElements(compressed_data, input_bytes, JNI_ABORT);
      return nullptr;
    }
    huff_input[0] = (unsigned char)(nodes & 0xFF);
    huff_input[1] = 0;
    memcpy(huff_input + 2, input + tree_offset + 1, huff_input_length - 1);

    logd("ZZZ: calling convert_huff(data=%p, length=%d, capacity=%lu)",
      (void *)huff_input, (int)huff_input_length + 1, output_capacity);
    decoded = convert_huff(huff_input, huff_input_length + 1,
            output_capacity);
    free(huff_input);
  logd("ZZZ: convert_huff returned %p", (void *)decoded);
  if (decoded != nullptr) {
    decoded_length = get_huff_output_length();
    logd("ZZZ: decoded_length=%lu", decoded_length);
  } else {
      logd("ZZZ: byte-count layout failed; trying 16-bit tree at offset 13");
      if (input_size > 15) {
        const unsigned int word_nodes = input[13] | (input[14] << 8);
        logd("ZZZ: alternate tree nodes=%u", word_nodes);
        if (word_nodes > 0 && word_nodes < 4096 &&
            15 + word_nodes * 4 <= input_size) {
          decoded = convert_huff(input + 13, input_size - 13,
                                 output_capacity);
          logd("ZZZ: alternate convert_huff returned %p", (void *)decoded);
          if (decoded != nullptr)
            decoded_length = get_huff_output_length();
          logd("ZZZ: alternate decoded_length=%lu", decoded_length);
        }
      }
      if (decoded == nullptr)
        logd("ZZZ: Huffman layouts failed; trying Crunch/LZW");
    }

    if (decoded == nullptr && input_size > 12 && input[1] == 0xFE) {
      const int reference_offsets[] = {12, 13};
      const int reference_bits[] = {12, 16};
      for (int offset : reference_offsets) {
        for (int bits : reference_bits) {
          if (decoded != nullptr)
            break;
          unsigned long reference_length = 0;
          logd("ZZZ: trying reference Crunch offset=%d bits=%d RLE=1",
               offset, bits);
          decoded = decompress_crunch_reference(
              input + offset, input_size - offset, bits, true, output_capacity,
              &reference_length);
          if (decoded != nullptr) {
            decoded_length = reference_length;
            logd("ZZZ: reference Crunch success offset=%d bits=%d length=%lu",
                 offset, bits, decoded_length);
          } else {
            logd("ZZZ: reference Crunch failed offset=%d bits=%d", offset,
                 bits);
          }
        }
      }
    }

    if (decoded == nullptr && input_size > 12 && input[1] == 0xFE) {
      const int lzw_modes[][2] = {{12, 1}, {12, 0}, {0, 1}, {0, 0}};
      const int payload_offsets[] = {13, 12};
      unsigned char *best_decoded = nullptr;
      unsigned long best_length = 0;
      int best_score = -1;
      for (const int payload_offset : payload_offsets) {
        const unsigned long lzw_input_length =
            (unsigned long)input_size - payload_offset;
        logd("ZZZ: trying payload_offset=%d payload_length=%lu",
             payload_offset, lzw_input_length);
        for (const auto &mode : lzw_modes) {
          logd("ZZZ: trying LZW max_bits=%d use_rle=%d", mode[0], mode[1]);
          unsigned char *candidate = convert_lzw_dynamic(
              input + payload_offset, mode[0], mode[1], lzw_input_length,
              output_capacity);
        if (candidate != nullptr) {
          const unsigned long candidate_length = get_lzw_output_length();
          const unsigned long sample_length =
              candidate_length < 4096 ? candidate_length : 4096;
          int score = 0;
          for (unsigned long index = 0; index < sample_length; index++) {
            const unsigned char value = candidate[index];
            if ((value >= 32 && value <= 126) || value == '\n' ||
                value == '\r' || value == '\t')
              score++;
          }
          logd("ZZZ: LZW success offset=%d max_bits=%d use_rle=%d decoded_length=%lu",
            payload_offset, mode[0], mode[1], candidate_length);
          logd("ZZZ: decoded prefix=%02X %02X %02X %02X %02X %02X %02X %02X",
               candidate_length > 0 ? candidate[0] : 0,
               candidate_length > 1 ? candidate[1] : 0,
               candidate_length > 2 ? candidate[2] : 0,
               candidate_length > 3 ? candidate[3] : 0,
               candidate_length > 4 ? candidate[4] : 0,
               candidate_length > 5 ? candidate[5] : 0,
               candidate_length > 6 ? candidate[6] : 0,
               candidate_length > 7 ? candidate[7] : 0);
          logd("ZZZ: candidate text score=%d/%lu", score, sample_length);
          if (score > best_score) {
            free(best_decoded);
            best_decoded = candidate;
            best_length = candidate_length;
            best_score = score;
          } else {
            free(candidate);
          }
        } else {
          logd("ZZZ: LZW failed offset=%d max_bits=%d use_rle=%d",
               payload_offset, mode[0], mode[1]);
        }
        }
      }
      decoded = best_decoded;
      decoded_length = best_length;
      logd("ZZZ: selected LZW candidate score=%d length=%lu", best_score,
           decoded_length);
  }

  env->ReleaseByteArrayElements(compressed_data, input_bytes, JNI_ABORT);
  logd("ZZZ: input JNI buffer released");
  if (decoded == nullptr || decoded_length > INT_MAX) {
    logd("ZZZ: invalid decoded result, pointer=%p length=%lu",
         (void *)decoded, decoded_length);
    free(decoded);
    return nullptr;
  }

  logd("ZZZ: allocating Java byte array length=%lu", decoded_length);
  jbyteArray result = env->NewByteArray((jsize)decoded_length);
  logd("ZZZ: NewByteArray returned %p", (void *)result);
  if (result != nullptr) {
    env->SetByteArrayRegion(result, 0, (jsize)decoded_length,
                            (jbyte *)decoded);
    logd("ZZZ: SetByteArrayRegion completed");
  }
  free(decoded);
  logd("ZZZ: decompression finished");
  return result;
}
