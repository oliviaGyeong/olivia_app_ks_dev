//
// Created by mskl on 2017-10-31.
//

#ifndef PMPOS_FUNC_H
#define PMPOS_FUNC_H

void _H2N(__in byte cData, __out byte *pDest);
byte _N2H(__in byte *pStr);
void _h2nibble(__in byte *pSour, __out byte *pDest, __in word nLen);
short _n2hex(__in byte *pSour, __out byte *pDest, __in word nLen);
byte _calcLRC(__in byte *pStr, __in short nLen);
word SetFieldDataEx(__in void *pTitle, __out void *pDest, __in void *pSour, __in word sLen, __in word sizMax);
byte SetFieldData(__in char *pTitle, __out byte *pDest, __in void *pData, __in word dataLen, __in word sizMax, __in byte align);
void ReverseCopy(__in __out unsigned char *src, unsigned short srcLen);

word _makePkt_IQ_CreditApprovalResult(__in byte *pVRPktBuf, __out byte *pIQPktBuf);

int CRYPT_Base64_Encode( unsigned char *dst, size_t *dst_len, const unsigned char *src, size_t src_len );
int CRYPT_Base64_Decode( unsigned char *dst, size_t *dst_len, const unsigned char *src, size_t src_len );
jint CheckBufferSize(jint srcLen, jint targetBufSiz);

#endif //PMPOS_FUNC_H

/**< end of file */
