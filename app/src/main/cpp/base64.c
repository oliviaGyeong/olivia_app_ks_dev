#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include "base64.h"
#include "pktDef.h"

////////////////////////////////////////////////////////////////////
static const unsigned char base64_enc_map[64] =
        {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
                'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', '+', '/'
        };

static const unsigned char base64_dec_map[128] =
        {
                127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
                127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
                127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
                127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
                127, 127, 127,  62, 127, 127, 127,  63,  52,  53,
                54,  55,  56,  57,  58,  59,  60,  61, 127, 127,
                127,  64, 127, 127, 127,   0,   1,   2,   3,   4,
                5,   6,   7,   8,   9,  10,  11,  12,  13,  14,
                15,  16,  17,  18,  19,  20,  21,  22,  23,  24,
                25, 127, 127, 127, 127, 127, 127,  26,  27,  28,
                29,  30,  31,  32,  33,  34,  35,  36,  37,  38,
                39,  40,  41,  42,  43,  44,  45,  46,  47,  48,
                49,  50,  51, 127, 127, 127, 127, 127
        };

int CRYPT_Base64_Encode( unsigned char *dst, size_t *dst_len, const unsigned char *src, size_t src_len )
{
    size_t          i, n;
    int             C1, C2, C3;
    unsigned char   *p;

    if( src_len == 0 )
        return( 0 );

    n = ( src_len << 3 ) / 6;

    switch( ( src_len << 3 ) - ( n * 6 ) )
    {
        case  2: n += 3; break;
        case  4: n += 2; break;
        default: break;
    }

    if( *dst_len < n + 1 )
    {
        *dst_len = n + 1;
        return( DAMO_CRYPT_ERR_BASE64_BUFFER_TOO_SMALL );
    }

    n = ( src_len / 3 ) * 3;

    for( i = 0, p = dst; i < n; i += 3 )
    {
        C1 = *src++;
        C2 = *src++;
        C3 = *src++;

        *p++ = base64_enc_map[(C1 >> 2) & 0x3F];
        *p++ = base64_enc_map[(((C1 &  3) << 4) + (C2 >> 4)) & 0x3F];
        *p++ = base64_enc_map[(((C2 & 15) << 2) + (C3 >> 6)) & 0x3F];
        *p++ = base64_enc_map[C3 & 0x3F];
    }

    if( i < src_len )
    {
        C1 = *src++;
        C2 = ( ( i + 1 ) < src_len ) ? *src++ : 0;

        *p++ = base64_enc_map[(C1 >> 2) & 0x3F];
        *p++ = base64_enc_map[(((C1 & 3) << 4) + (C2 >> 4)) & 0x3F];

        if( ( i + 1 ) < src_len )
            *p++ = base64_enc_map[((C2 & 15) << 2) & 0x3F];
        else *p++ = '=';

        *p++ = '=';
    }

    *dst_len = p - dst;
    *p = 0;

    return( 0 );
}

int CRYPT_Base64_Decode( unsigned char *dst, size_t *dst_len, const unsigned char *src, size_t src_len )
{
    size_t i, n;
    uint32_t j, x;
    unsigned char *p;

    for( i = n = j = 0; i < src_len; i++ )
    {
        if( ( src_len - i ) >= 2 &&
            src[i] == '\r' && src[i + 1] == '\n' )
            continue;

        if( src[i] == '\n' )
            continue;

        if( src[i] == '=' && ++j > 2 )
            return( DAMO_CRYPT_ERR_BASE64_INVALID_CHARACTER );

        if( src[i] > 127 || base64_dec_map[src[i]] == 127 )
            return( DAMO_CRYPT_ERR_BASE64_INVALID_CHARACTER );

        if( base64_dec_map[src[i]] < 64 && j != 0 )
            return( DAMO_CRYPT_ERR_BASE64_INVALID_CHARACTER );

        n++;
    }

    if( n == 0 )
        return( 0 );

    n = ( ( n * 6 ) + 7 ) >> 3;
    n -= j;

    if( dst == NULL || *dst_len < n )
    {
        *dst_len = n;
        return( DAMO_CRYPT_ERR_BASE64_BUFFER_TOO_SMALL );
    }

    for( j = 3, n = x = 0, p = dst; i > 0; i--, src++ )
    {
        if( *src == '\r' || *src == '\n' )
            continue;

        j -= ( base64_dec_map[*src] == 64 );
        x  = ( x << 6 ) | ( base64_dec_map[*src] & 0x3F );

        if( ++n == 4 )
        {
            n = 0;
            if( j > 0 ) *p++ = (unsigned char)( x >> 16 );
            if( j > 1 ) *p++ = (unsigned char)( x >>  8 );
            if( j > 2 ) *p++ = (unsigned char)( x       );
        }
    }

    *dst_len = p - dst;

    return( 0 );
}

jint CheckBufferSize(jint srcLen, jint targetBufSiz)
{
    int     n=(srcLen / 3);

    if(srcLen %3)
        n += 1;

    if(n > targetBufSiz)
        return 0;

    return n;
}

JNIEXPORT jint JNICALL Java_device_apps_pmpos_VanHostManager_encodeBase64 (JNIEnv *env,
                                                                           __in __out jbyteArray pDataBuf,
                                                                           __in jint srcLen,
                                                                           __in jint dataBufSiz)
{
    jint    encLen=0;// encoding data length

    if (!srcLen)
        return 0;

    if(!(encLen = CheckBufferSize(srcLen, dataBufSiz)))
        return 0;

    byte    *pTmpBuf=(byte *)malloc(srcLen);
    byte    *pEncBuf=(byte *)malloc(encLen);

    (*env)->GetByteArrayRegion(env, pDataBuf, 0, srcLen, (jbyte *)pTmpBuf);

    CRYPT_Base64_Encode(pEncBuf, (size_t *)&encLen, pTmpBuf, (size_t)srcLen);

    (*env)->SetByteArrayRegion(env, pDataBuf, 0, encLen, (jbyte *) pTmpBuf);

    return encLen;
}

/**< end of file */
