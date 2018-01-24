//
// Created by mskl on 2017-10-25.
//
#include <string.h>

#define DAMO_CRYPT_ERR_BASE64_BUFFER_TOO_SMALL               -50000  /* Output buffer too small. */
#define DAMO_CRYPT_ERR_BASE64_INVALID_CHARACTER              -50001  /* Invalid character in input. */

#ifndef PMPOS_BASE64_H
#define PMPOS_BASE64_H

int DAMO_CRYPT_Base64_Encode( unsigned char *dst, size_t *dlen, const unsigned char *src, size_t slen );

int DAMO_CRYPT_Base64_Decode( unsigned char *dst, size_t *dlen, const unsigned char *src, size_t slen );

#endif //PMPOS_BASE64_H
