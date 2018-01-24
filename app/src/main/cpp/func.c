#include "pktDef.h"
#include "base64.h"

/////////////////////////////////////////////////////////////////////////////////////////////////
void _H2N(__in unsigned char cData,
          __out unsigned char *pDest)
{
    unsigned char	hv = 0, lv = 0;

    hv = (cData >> 4) & 0x0f;
    lv = cData & 0x0f;

    *pDest++ = (hv >= 0 && hv <= 9) ? hv + '0' : hv + 'A' - 10;
    *pDest++ = (lv >= 0 && lv <= 9) ? lv + '0' : lv + 'A' - 10;
}

byte _N2H(__in byte *pStr)
{
    unsigned char	hv = 0, lv = 0, hc = 0, lc = 0;

    hc = *pStr++;		lc = *pStr++;

    hv = (hc <= '9') ? hc - '0' : ((hc <= 'Z') ? hc - 'A' + 10 : hc - 'a' + 10);
    lv = (lc <= '9') ? lc - '0' : ((lc <= 'Z') ? lc - 'A' + 10 : lc - 'a' + 10);

    return ((hv << 4) | lv);
}

void _h2nibble(__in byte *pSour,
               __out byte *pDest,
               __in word nLen)
{
    unsigned short	i;
    unsigned char	hv = 0, lv = 0;

    for(i = 0; i < nLen; i++)
    {
        hv = (*pSour >> 4) & 0x0f;
        lv = *(pSour++) & 0x0f;

        *pDest++ = (hv >= 0 && hv <= 9) ? hv + '0' : hv + 'A' - 10;
        *pDest++ = (lv >= 0 && lv <= 9) ? lv + '0' : lv + 'A' - 10;
    }
}

short _n2hex(__in byte *pSour, __out byte *pDest, __in word nLen)
{
    unsigned short	i = 0;

    nLen /= 2;

    for(; i < nLen; i++)
        *pDest++ = _N2H(&pSour[i * 2]);

    return i;
}

byte _calcLRC(__in byte *pStr, __in short nLen)
{
    short	i = 0;
    byte	lrc = 0x00;

    for(; i < nLen; i++)
        lrc ^= *pStr++;

    return lrc;
}

/////////////////////////////////////////////////////////////////////////////////////////////////
byte SetFieldData(char *pTitle, byte *pDest, void *pData, word dataLen, word sizMax, byte align)
{
    if(dataLen > sizMax)
        dataLen = sizMax;

    if(align == LJFS)
    {//space fill
        memset(pDest, 0x20, sizMax);
        memcpy(pDest, (unsigned char *)pData, dataLen);
    }
    else
    {// RJLZ
        memset(pDest, 0x30, sizMax);
        memcpy(&pDest[sizMax - dataLen], (byte *)pData, dataLen);
    }

    return sizMax;
}

/************************************************************************/
/* pSour : TLV                                                          */
/************************************************************************/
word SetFieldDataEx(void *pTitle, void *pDest, void *pSour, word sLen, word sizMax)
{
    unsigned char	*pD=(unsigned char *)pDest;
    unsigned char	*pS=(unsigned char *)pSour;
    int				tLen=strlen((char *)pTitle);

    if(sizMax < (sLen * 2))
        sLen = sizMax;

    _h2nibble(pS, pD, sLen);

    return sizMax;
}

/* array를 중간대칭으로 재배열한다. */
void ReverseCopy(__in __out unsigned char *src,
                  unsigned short srcLen)
{
    unsigned char	cTmp;
    unsigned char	*dest = src + srcLen - 1;
    
    srcLen /= 2;
    
    while(srcLen--)
    {
        cTmp = *src;
        
        *src++ = *dest;
        *dest-- = cTmp;
    }
    
    return;
}

/**< end of file */
