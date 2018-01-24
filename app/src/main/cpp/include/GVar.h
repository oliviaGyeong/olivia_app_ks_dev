//
// Created by mskl on 2017-10-25.
//

#ifndef PMPOS_GVAR_H
#define PMPOS_GVAR_H

#define MAX_BUF_SIZ     2560
//#define TEST_DATA_ENABLE

#ifdef _GLOVAL_VARIABLE_

#define GEXT_VAR

byte                _vanPktBuf[MAX_BUF_SIZ]={0x00,};
byte                _ifmPktBuf[MAX_BUF_SIZ]={0x00,};
//byte	            _szTmnlCertiNo[]={"####PM1100LH1711####KOCESPOS1001\x00"};
byte	            _szTmnlCertiNo[]={"                ####KOCESPOS1001\x00"};
byte	            _szPktVer[]={"A1\x0"};	// 0xa1(비암호화) or 0xa2(암호화)로 저장?
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
byte                _vanTmnlDN_Data[640]={0x00, };
kvrTmnlDown_t       *_pTmnlDownData=NULL;
byte                _szDemonPkt[340]={0x00, };
receiptToDemon_t    *_pReceiptPkt=(receiptToDemon_t *)&_szDemonPkt[4];

#else

#define GEXT_VAR    extern

GEXT_VAR byte   _vanTmnlDN_Data[640];   // 단말기 다운로드 Data
GEXT_VAR byte   _vanPktBuf[MAX_BUF_SIZ];
GEXT_VAR byte   _ifmPktBuf[MAX_BUF_SIZ];
GEXT_VAR byte	_szTmnlCertiNo[];
GEXT_VAR byte	_szPktVer[];
GEXT_VAR kvrTmnlDown_t   *_pTmnlDownData;

#endif


#endif //PMPOS_GVAR_H

/**< end of file */
