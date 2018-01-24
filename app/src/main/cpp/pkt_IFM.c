//
// Created by mskl on 2017-10-30.
//

#define _GLOVAL_VARIABLE_

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "pktDef.h"
#include "GVar.h"
#include "func.h"

#define LOG_TAG "pkt_IFM"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  0x10 | 01 | 0000001004 | 20171109195103 | 0x31 | 0x30 | 0001 |
typedef struct IC_CREDIT_APPROVAL_INPUT_DATA{
    byte	transCmd	[ 1];
    byte	transType	[ 2];	// 거래구분(01, 06, 07)
    byte	transAmount	[10];	// 거래금액
    byte	transDate	[14];	// 거래일시
//	byte	isSignPad	[ 1];	// 서명패드 입력여부 (0x31 fixed
//	byte	isCashICSum	[ 1];	// 현금IC 간소화 (0x30 fixed)
    byte	cntReceipt	[ 4];	// 전표출력 카운터 (거래카운터 % 9999를 사용)
} __attribute__ ((packed)) ICAID_t;	// IC Credit Approval Input Data
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
static word _makePkt_ReqCardReadToIFM(__in byte *pInputData, __out byte *pIfmBuf)
{
    ICAID_t			*pID=(ICAID_t *)pInputData;
    _kiqIcCreditA_t  *pICA=(_kiqIcCreditA_t *)pIfmBuf;

    /**< initialize the packet buffer... */
    memset(&pIfmBuf[1], 0x20, sizeof(_kiqIcCreditA_t));
    pIfmBuf[sizeof(_kiqIcCreditA_t)] = 0x00;

    /**< set data...*/
    pICA->stx[0] = 0x02;
    pICA->len = sizeof(_kiqIcCreditA_t) - 4;     // -1(STX), -2(Len), -1(LRC)
    ReverseCopy(&pIfmBuf[1], 2);
    pICA->cmd[0] = pID->transCmd[0];
    memcpy(pICA->transType, pID->transType, sizeof(pID->transType));
    memcpy(pICA->amount, pID->transAmount, sizeof(pID->transAmount));
    memcpy(pICA->date, pID->transDate, sizeof(pID->transDate));
    pICA->fSignPad[0] = '1';// fixed
    pICA->fCashSimp[0] = '0';// fixed
    memcpy(pICA->cntReceipt, pID->cntReceipt, sizeof(pID->cntReceipt));
    pICA->signDataChange[0] = '0';// fixed
    memcpy(pICA->entryMin, "00", 2);
    memcpy(pICA->entryMax, "06", 2);
    memcpy(pICA->indexWKey, "01", 2);
    memcpy(pICA->workingKey, "B77F25FB72762DD3", 16);
    pICA->etx[0] = 0x03;
    pICA->lrc[0] = _calcLRC(&pIfmBuf[1], sizeof(_kiqIcCreditA_t) - 2);

    return sizeof(_kiqIcCreditA_t);
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/** :: To IFM
 *
 * @param pIQPktBuf  :: IFM request packet buffer
 * @param pInputData :: UI input data
 * @param iqBufLen   :: size of IFM request packet buffer
 * @param inDataLen  :: length of UI input data
 * @return           :: length of IFM request packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_reqPreambleToIFM (JNIEnv *env, jobject this,
                                                                                      __out jbyteArray pIQPktBuf,
                                                                                      __in jbyteArray pInputData,
                                                                                      __in jint iqBufLen,
                                                                                      __in jint inDataLen)
{
    word    pktLen=0;
//    byte	*pIQPktMem=(byte *)malloc(sizeof(kiqIcCreditA_t) + 6);
    byte    *pTmpMem=(byte *)malloc(inDataLen + 1);

    memset(pTmpMem, 0x00, inDataLen + 1);

    (*env)->GetByteArrayRegion(env, pInputData, 0, inDataLen, (jbyte *)pTmpMem);

    pktLen = _makePkt_ReqCardReadToIFM(pTmpMem, _ifmPktBuf);

    if(iqBufLen >= pktLen)
        (*env)->SetByteArrayRegion(env, pIQPktBuf, 0, pktLen, (jbyte *)_ifmPktBuf);
    else
        pktLen = 0;

    if(pTmpMem != NULL)
        free(pTmpMem);

    return pktLen;
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
word _makePkt_ReqCreditApprovalResultToIFM(__in byte *pVRPktBuf, __out byte *pIfmBuf)
{
    kvrIC_TRANS_t       *pVRPkt=(kvrIC_TRANS_t *)pVRPktBuf;
    _kiqIcCreditAR_t    *pICAR=(_kiqIcCreditAR_t *)pIfmBuf;

    pICAR->stx[0] = 0x02;
    pICAR->len = sizeof(_kiqIcCreditAR_t) - 4;   // -1(STX), -2(Len), -1(LRC)
    ReverseCopy(&pIfmBuf[1], 2);
    pICAR->cmd[0] = 0x20;
    memcpy(pICAR->date, pVRPkt->approvalDate, sizeof(pICAR->date));
    _n2hex(pVRPkt->additionalRespData, pICAR->addRespData, sizeof(pVRPkt->additionalRespData));
    _n2hex(pVRPkt->issuerAuthData, pICAR->iAuthData, sizeof(pVRPkt->issuerAuthData));
    _n2hex(pVRPkt->issuerScript, pICAR->iScript, sizeof(pVRPkt->issuerScript));
    memcpy(pICAR->approvResult, pVRPkt->brandRespCode, sizeof(pVRPkt->brandRespCode));
    pICAR->etx[0] = 0x03;
    pICAR->lrc[0] = _calcLRC(&pIfmBuf[1], (sizeof(_kiqIcCreditAR_t) - 2));   // -1(STX), -1(LRC)

    return sizeof(_kiqIcCreditAR_t);
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/** :: To IFM
 *
 * @param piqPktBuf :: IFM request packet buffer
 * @param pvrPktBuf :: VAN response packet
 * @param iqBufLen  :: size of IFM request packet buffer
 * @param vrBufLen  :: length of VAN response packet
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_reqCreditApprovalResultToIFM (JNIEnv *env, jobject this,
                                                                                               __out jbyteArray piqPktBuf,
                                                                                               __in jbyteArray pvrPktBuf,
                                                                                               __in jint iqBufLen,
                                                                                               __in jint vrBufLen)
{
    word    pktLen=0;
    byte    *pTmpMem=(byte *)malloc(vrBufLen + 1);

    memset(pTmpMem, 0x00, vrBufLen + 1);

    (*env)->GetByteArrayRegion(env, pvrPktBuf, 0, vrBufLen, (jbyte *)pTmpMem);

    pktLen = _makePkt_ReqCreditApprovalResultToIFM(pTmpMem, _ifmPktBuf);

    if(iqBufLen >= pktLen)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, pktLen, (jbyte *)_ifmPktBuf);
    else
        pktLen = 0;

    if(pTmpMem != NULL)
        free(pTmpMem);

    return pktLen;
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_reqInitializeIFM (JNIEnv *env, jobject this,
                                                                                                 __out jbyteArray piqPktBuf,
                                                                                                 __in jint iqBufLen)
{
    word    pktLen=0;

    _ifmPktBuf[pktLen++] = 0x02;    // STX
    _ifmPktBuf[pktLen++] = 0x00;
    _ifmPktBuf[pktLen++] = 0x04;
    _ifmPktBuf[pktLen++] = 0xA0;
    _ifmPktBuf[pktLen++] = '9';
    _ifmPktBuf[pktLen++] = '9';
    _ifmPktBuf[pktLen++] = 0x03;    // ETX
    _ifmPktBuf[pktLen] = _calcLRC(&_ifmPktBuf[1], pktLen - 1);
    pktLen++;

    if(iqBufLen >= pktLen)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, pktLen, (jbyte *)_ifmPktBuf);
    else
        pktLen = 0;

    return pktLen;
}

/** :: To IFM
 *
 * @param piqPktBuf     :: IFM request packet buffer
 * @param pDateTime     :: Current Date Time (YYYYMMDDHHmmSS) : 14-Bytes
 * @param iqBufLen      :: size of IFM request packet buffer
 * @param dateTimeLen   :: length of Date & Time Buffer
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_reqSetSystemDateTime (JNIEnv *env, jobject this,
                                                                                                         __out jbyteArray piqPktBuf,
                                                                                                         __in jbyteArray pDateTime,
                                                                                                         __in jint iqBufLen,
                                                                                                         __in jint dateTimeLen)
{
    word    pktLen=0;
    byte    *pTmpMem=(byte *)malloc(dateTimeLen + 1);// YYYYMMDDHHmmSS

    memset(pTmpMem, 0x00, dateTimeLen + 1);

    (*env)->GetByteArrayRegion(env, pDateTime, 0, dateTimeLen, (jbyte *)pTmpMem);

    _ifmPktBuf[pktLen++] = 0x02;    // STX
    _ifmPktBuf[pktLen++] = 0x00;
    _ifmPktBuf[pktLen++] = 0x10;
    _ifmPktBuf[pktLen++] = 0xF1;
    memcpy(&_ifmPktBuf[pktLen], pTmpMem, dateTimeLen);  // 14-Byte
    pktLen += dateTimeLen;
    _ifmPktBuf[pktLen++] = 0x03;    // ETX
    _ifmPktBuf[pktLen] = _calcLRC(&_ifmPktBuf[1], pktLen - 1);
    pktLen++;

    if(iqBufLen >= pktLen)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, pktLen, (jbyte *)_ifmPktBuf);
    else
        pktLen = 0;

    if(pTmpMem != NULL)
        free(pTmpMem);

    return pktLen;
}

//////////////////////////////////////// Added by irene.choi - start
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  0x10 | 01 | 0000001004 | 20171109195103 | 0x31 | 0x30 | 0001 |
typedef struct IC_CANCEL_PAYMENT_INPUT_DATA{
    byte	transCmd	[ 1];
} __attribute__ ((packed)) ICCPID_t;	// IC Credit Approval Input Data
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
static word _makePkt_reqCancelPaymentToIFM(__in byte *pInputData, __out byte *pIfmBuf)
{
    ICCPID_t			*pID=(ICCPID_t *)pInputData;
    _kiqIcCancelP_t  *pICA=(_kiqIcCancelP_t *)pIfmBuf;

    /**< initialize the packet buffer... */
    memset(&pIfmBuf[1], 0x20, sizeof(_kiqIcCancelP_t));
    pIfmBuf[sizeof(_kiqIcCreditA_t)] = 0x00;

    /**< set data...*/
    pICA->stx[0] = 0x02;
    pICA->len = sizeof(_kiqIcCancelP_t) - 4;     // -1(STX), -2(Len), -1(LRC)
    ReverseCopy(&pIfmBuf[1], 2);
    pICA->cmd[0] = pID->transCmd[0];
    pICA->etx[0] = 0x03;
    pICA->lrc[0] = _calcLRC(&pIfmBuf[1], sizeof(_kiqIcCancelP_t) - 2);

    return sizeof(_kiqIcCancelP_t);
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/** :: To IFM
 *
 * @param pIQPktBuf  :: IFM request packet buffer
 * @param pInputData :: UI input data
 * @param iqBufLen   :: size of IFM request packet buffer
 * @param inDataLen  :: length of UI input data
 * @return           :: length of IFM request packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_reqCancelPaymentToIFM (JNIEnv *env, jobject this,
                                                                                            __out jbyteArray pIQPktBuf,
                                                                                            __in jbyteArray pInputData,
                                                                                            __in jint iqBufLen,
                                                                                            __in jint inDataLen)
{
    word    pktLen=0;
//    byte	*pIQPktMem=(byte *)malloc(sizeof(kiqIcCreditA_t) + 6);
    byte    *pTmpMem=(byte *)malloc(inDataLen + 1);

    memset(pTmpMem, 0x00, inDataLen + 1);

    (*env)->GetByteArrayRegion(env, pInputData, 0, inDataLen, (jbyte *)pTmpMem);

    pktLen = _makePkt_reqCancelPaymentToIFM(pTmpMem, _ifmPktBuf);

    if(iqBufLen >= pktLen)
        (*env)->SetByteArrayRegion(env, pIQPktBuf, 0, pktLen, (jbyte *)_ifmPktBuf);
    else
        pktLen = 0;

    if(pTmpMem != NULL)
        free(pTmpMem);

    return pktLen;
}
//////////////////////////////////////// Added by irene.choi - stop

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
// 이하 to IFM
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
typedef struct REQ_TERMINAL_INFO {
    byte    stx     [ 1];
    word	len;
    byte	cmd     [ 1];
    byte	date    [14];
    byte    etx     [ 1];
    byte    lrc     [ 1];
} __attribute__ ((packed)) kiqTmnlInfo_t; // 터미널 정보요청

/**<
 *
 * @param piqPktBuf
 * @param pInputData
 * @param iqBufLen
 * @param inputLen
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100DeviceInfo (JNIEnv *env, jobject this,
                                                                                      __out jbyteArray piqPktBuf,
                                                                                      __in jbyteArray dateTime,
                                                                                      __in jint iqBufLen){

//    byte    *pDTMem=(byte *)malloc(15);
//    int     i=0;
//
//    memset(pDTMem, 0x00, 15);
//    memset(_ifmPktBuf, 0x00, (sizeof(kiqTmnlInfo_t) + 4));
//
//    (*env)->GetByteArrayRegion(env, dateTime, 0, 14, (jbyte *)pDTMem);
//
//    kiqTmnlInfo_t   *pIPkt=(kiqTmnlInfo_t *)_ifmPktBuf;
//
//    pIPkt->stx[0] = 0x02;
//    pIPkt->len = 16;
//    ReverseCopy(&_ifmPktBuf[1], 2);
//    pIPkt->cmd[0] = 0x58;
//    memcpy(pIPkt->date, pDTMem, 14);
//    pIPkt->etx[0] = 0x03;
//    pIPkt->lrc[0] = _calcLRC(&_ifmPktBuf[1], i - 1);
//
//    if(pDTMem != NULL)
//        free(pDTMem);
//
//    if(iqBufLen >= sizeof(kiqTmnlInfo_t))
//        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, sizeof(kiqTmnlInfo_t), (jbyte *)_ifmPktBuf);
//    else
//        return 0;
//
//
//    return sizeof(kiqTmnlInfo_t);


    byte    *pDTMem=(byte *)malloc(15);
    int     i=0;

    memset(pDTMem, 0x00, 15);
    memset(_ifmPktBuf, 0x00, (sizeof(kiqTmnlInfo_t) + 4));

    (*env)->GetByteArrayRegion(env, dateTime, 0, 14, (jbyte *)pDTMem);

    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x00;
    _ifmPktBuf[i++] = 0x16;
    _ifmPktBuf[i++] = 0x58;
    memcpy(&_ifmPktBuf[i], pDTMem, 14);
    i += 14;
    _ifmPktBuf[i++] = 0x03;

    _ifmPktBuf[i] = _calcLRC(&_ifmPktBuf[1], i - 1);
    i++;

    if(pDTMem != NULL)
        free(pDTMem);

    if(iqBufLen >= i)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, i, (jbyte *)_ifmPktBuf);
    else
        i=0;

    return i;

}

JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100CreateFutureKey (JNIEnv *env, jobject this,
                                                                                           __out jbyteArray piqPktBuf,
                                                                                           __in jint iqBufLen)
{
    int     i=0;

    memset(_ifmPktBuf, 0x00, 7);

    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x00;
    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x30;
    _ifmPktBuf[i++] = 0x03;

    _ifmPktBuf[i] = _calcLRC(&_ifmPktBuf[1], i - 1);
    i++;

    if(iqBufLen >= i)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, i, (jbyte *)_ifmPktBuf);
    else
        i = 0;

    return i;

}

typedef struct RESP_SECURITY_KEY_UPDATE_FROM_VAN{
    common_header_t		cHead;
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	verifyRespData		[96];	// 검증 응답 데이터
    byte	respMsg				[32];	// 응답메시지(거절 시 응답 message)
    byte	RFU					[50];
    byte	cCR					[ 1];
} __attribute__ ((packed)) kvrSKeyUpdate_t;	// total 276-Byte

typedef struct REQUEST_SECUREKEY_UPDATE_TO_IFM{
    byte    stx             [ 1];
    word    len;
    byte    cmd             [ 1];
    byte    date            [14];
    byte    verifyRespData  [48];
    byte    etx             [ 1];
    byte    lrc             [ 1];
} __attribute__ ((packed)) _kiqSKeyUpdate_t;

/**< :: To IFM
 *
 * @param piqPktBuf  :: IFM packet buffer
 * @param pVRPktBuf  :: received VAN packet
 * @param iqBufLen   :: size of IFM packet buffer
 * @param vrPktLen   :: length of received VAN packet
 * @return           :: length of packet to IFM
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100UpdateFutureKey (JNIEnv *env, jobject this,
                                                                                           __out jbyteArray piqPktBuf,
                                                                                           __in jbyteArray pVRPktBuf,
                                                                                           __in jint iqBufLen,
                                                                                           __in jint vrPktLen)
{
    if((iqBufLen < sizeof(_kiqSKeyUpdate_t)) ||(vrPktLen != sizeof(kvrSKeyUpdate_t)))
        return 0;

    byte    *pVRPktMem=(byte *)malloc(vrPktLen + 1);

    memset(pVRPktMem, 0x00, (vrPktLen + 1));
    memset(_ifmPktBuf, 0x00, (sizeof(kiqTmnlInfo_t) + 1));

    (*env)->GetByteArrayRegion(env, pVRPktBuf, 0, vrPktLen, (jbyte *)pVRPktMem);

    kvrSKeyUpdate_t     *pVRPkt=(kvrSKeyUpdate_t *)pVRPktMem;
    _kiqSKeyUpdate_t    *pIQPkt=(_kiqSKeyUpdate_t *)_ifmPktBuf;

    pIQPkt->stx[0] = 0x02;
    pIQPkt->len = 64;
    ReverseCopy(&_ifmPktBuf[1], 2);
    pIQPkt->cmd[0] = 0x40;
    memcpy(pIQPkt->date, pVRPkt->cHead.transDate, 14);
    _n2hex(pVRPkt->verifyRespData, pIQPkt->verifyRespData, 96);
    pIQPkt->etx[0] = 0x03;
    pIQPkt->lrc[0] = _calcLRC(&_ifmPktBuf[1], sizeof(_kiqSKeyUpdate_t) - 2);    // -1(STX), -1(LRC)

    (*env)->SetByteArrayRegion(env, piqPktBuf, 0, sizeof(_kiqSKeyUpdate_t), (jbyte *)_ifmPktBuf);

    return sizeof(_kiqSKeyUpdate_t);
}

JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100SelfProtection (JNIEnv *env, jobject this,
                                                                                          __out jbyteArray piqPktBuf,
                                                                                          __in jint iqBufLen)
{
    if(iqBufLen < 6)
        return 0;

    int     i=0;

    memset(_ifmPktBuf, 0x00, 7);

    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x00;
    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x50;
    _ifmPktBuf[i++] = 0x03;

    _ifmPktBuf[i] = _calcLRC(&_ifmPktBuf[1], i - 1);
    i++;

    if(iqBufLen >= i)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, i, (jbyte *)_ifmPktBuf);
    else
        i = 0;

    return i;

}

/**<
 *
 * @param piqPktBuf  :: IFM packet buffer
 * @param pReqType   :: request type
 * @param iqBufLen   :: size of IFM packet buffer
 * @return           :: length of IFM packet buffer
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100MutualAuthentication (JNIEnv *env, jobject this,
                                                                                                __out jbyteArray piqPktBuf,
                                                                                                __in jbyteArray pReqType,
                                                                                                __in jint iqBufLen)
{
    byte    *pReqTypeMem=(byte *)malloc(5);
    int     i=0;

    memset(_ifmPktBuf, 0x00, 12);

    (*env)->GetByteArrayRegion(env, pReqType, 0, 4, (jbyte *)pReqTypeMem);

    _ifmPktBuf[i++] = 0x02;
    _ifmPktBuf[i++] = 0x00;
    _ifmPktBuf[i++] = 0x06;
    _ifmPktBuf[i++] = 0x52;
    memcpy(&_ifmPktBuf[i], pReqTypeMem, 4);
    i += 4;
    _ifmPktBuf[i++] = 0x03;

    _ifmPktBuf[i] = _calcLRC(&_ifmPktBuf[1], i - 1);
    i++;

    if(iqBufLen >= i)
        (*env)->SetByteArrayRegion(env, piqPktBuf, 0, i, (jbyte *)_ifmPktBuf);
    else
        i = 0;

    if(pReqTypeMem != NULL)
        free(pReqTypeMem);

    return i;

}



/**< :: To IFM
 *
 * @param pVanPktBuf    :: sending packet buffer to IFM
 * @param pVanPktBuf    :: a packet received from VAN-HOST
 * @param vanBufLen     :: size of sending packet buffer
 * @param rdBufLen      :: length of VAN response packet
 * @return              :: length of IFM packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100MutualAuthInfoResult (JNIEnv *env, jobject this,
                                                                                                __out jbyteArray pIfmPktBuf,
                                                                                                __in jbyteArray pVanPktBuf,
                                                                                                __in jint ifmBufLen,
                                                                                                __in jint vanPktLen)
{
    if(ifmBufLen < sizeof(_kiqMutualAuthReault_t))
        return 0;

    if(vanPktLen != sizeof(kvrMutualAuth_t))
        return 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    byte    *pVanPktMem=(byte *)malloc(sizeof(kvrMutualAuth_t) + 1);
//    int     i=0;

    (*env)->GetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvrMutualAuth_t), (jbyte *)pVanPktMem);
    pVanPktMem[sizeof(kvrMutualAuth_t)] = 0x00; // NULL terminate...
    memset(_ifmPktBuf, 0x00, sizeof(_kiqMutualAuthReault_t) + 1);   // for IFM request packet

    kvrMutualAuth_t         *pKVRPkt=(kvrMutualAuth_t *)pVanPktMem;
    _kiqMutualAuthReault_t   *pKIQPkt=(_kiqMutualAuthReault_t *)_ifmPktBuf;

    pKIQPkt->stx[0] = 0x02;
    pKIQPkt->len = (sizeof(_kiqMutualAuthReault_t) - 4);    // -1(STX), -2(Len), -1(LRC)
    pKIQPkt->cmd[0] = 0x54;

//  pKIQPkt->dateTime;

    memcpy(pKIQPkt->tmnlCertNum, pKVRPkt->tmnlCertiNum, sizeof(pKVRPkt->tmnlCertiNum));
    memcpy(pKIQPkt->tmnlSerialNum, pKVRPkt->tmnlSerialNum, sizeof(pKVRPkt->tmnlSerialNum));
    memcpy(pKIQPkt->respCode, "    ", 4);
    memcpy(pKIQPkt->respMsg, pKVRPkt->respMsg, sizeof(pKVRPkt->respMsg));
    _n2hex(pKVRPkt->connectSecurityKey, pKIQPkt->connSecKey, sizeof(pKVRPkt->connectSecurityKey));
    memcpy(pKIQPkt->numOfData, pKVRPkt->numOfData, sizeof(pKVRPkt->numOfData));
    memcpy(pKIQPkt->transProtocol, pKVRPkt->sndProtocol, sizeof(pKVRPkt->sndProtocol));
    memcpy(pKIQPkt->downSrvIP, pKVRPkt->downURLorIP, sizeof(pKVRPkt->downURLorIP));
    memcpy(pKIQPkt->downSvrPort, pKVRPkt->downSvrPort, sizeof(pKVRPkt->downSvrPort));
    memcpy(pKIQPkt->accountID, pKVRPkt->accountID, sizeof(pKVRPkt->accountID));
    memcpy(pKIQPkt->accountPWD, pKVRPkt->accountPWD, sizeof(pKVRPkt->accountPWD));
    memcpy(pKIQPkt->fwVERorDataType, pKVRPkt->fwVERorDataType, sizeof(pKVRPkt->fwVERorDataType));
    memcpy(pKIQPkt->fwVERorDataExpl, pKVRPkt->fwVERorDataExpl, sizeof(pKVRPkt->fwVERorDataExpl));
    memcpy(pKIQPkt->fileName, pKVRPkt->fileName, sizeof(pKVRPkt->fileName));
    memcpy(pKIQPkt->fileSize, pKVRPkt->fileSize, sizeof(pKVRPkt->fileSize));
    memcpy(pKIQPkt->fileChkType, pKVRPkt->fileChkType, sizeof(pKVRPkt->fileChkType));
    memcpy(pKIQPkt->fileChkSum, pKVRPkt->fileChkSum, sizeof(pKVRPkt->fileChkSum));
    memcpy(pKIQPkt->fileDecKey, pKVRPkt->fileDecKey, sizeof(pKVRPkt->fileDecKey));
    pKIQPkt->etx[0] = 0x03; // ETX
    pKIQPkt->lrc[0] = _calcLRC(&_ifmPktBuf[1], (sizeof(_kiqMutualAuthReault_t) - 2));
    ReverseCopy(&_ifmPktBuf[1], 2);

    (*env)->SetByteArrayRegion(env, pIfmPktBuf, 0, sizeof(_kiqMutualAuthReault_t), (jbyte *)_ifmPktBuf);

    if(pVanPktMem != NULL)
        free(pVanPktMem);

    return sizeof(_kiqMutualAuthReault_t);
}

#define SIZ_MAX_DOWN    2048 + 2

typedef struct FW_DOWNLOAD_PACKET {
	byte    stx             [ 1];           // STX
	word    len;                            // length 1
	byte    cmd             [ 1];           // command
	byte    reqDataType     [ 4];           // 요청데이터구분
	byte    dataTotal       [10];           // file size
	byte    sizTransmitData [10];           // size of transmit data
	byte    transmitData    [SIZ_MAX_DOWN]; // with ETX & LRC
} __attribute__ ((packed)) fwDownPkt_t;

//FirmwereUpdate Packet
/**<
 * @param pIfmPktBuf        : send packet buffer to IFM
 * @param ifmBufLen         : size of packet to IFM
 * @param pFileData         : send data
 * @param nSendDataLen      : length of send data
 * @param nFileTotalLen     : total length of Download Firmware
 * @return                  : length of packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ICReaderPacketManager_pm1100FirmwereUpdate (JNIEnv *env, jobject this,
                                                                                          __out jbyteArray pIfmPktBuf,
                                                                                          __in jint ifmBufLen,
                                                                                          __in jbyteArray pFileData,
                                                                                          __in jint nSendDataLen,
                                                                                          __in jint nFileTotalLen)
{
	int     sendLen=(nSendDataLen % 2048);

	if(!sendLen)
		sendLen = 2048;

	if(ifmBufLen < (sendLen + 30))
            return 0;
	
	word            pktLen=(1/*cmd*/ + 4/*요청데이터구분*/ + 20 + sendLen + 1/*ETX*/);
    byte            *pFDataMem=(byte *)malloc(sendLen + 1);  // for send data
	fwDownPkt_t     *pKIQPkt=(fwDownPkt_t *)_ifmPktBuf;

    (*env)->GetByteArrayRegion(env, pFileData, 0, sendLen, (jbyte *)pFDataMem);
	
	pKIQPkt->stx[0] = 0x02;
	pKIQPkt->len = pktLen;
	pKIQPkt->cmd[0] = 0x56;
	memcpy(pKIQPkt->reqDataType, "0001", 4);
	sprintf((char *)pKIQPkt->dataTotal, "%010d", nFileTotalLen);
	sprintf((char *)pKIQPkt->sizTransmitData, "%010d", nSendDataLen);
	memcpy(pKIQPkt->transmitData, pFDataMem, sendLen);
	pKIQPkt->transmitData[sendLen] = 0x03;
    ReverseCopy(&_ifmPktBuf[1], 2);
	pKIQPkt->transmitData[sendLen + 1] = _calcLRC(&_ifmPktBuf[1], (28 + sendLen));    // without STX, with ETX

    (*env)->SetByteArrayRegion(env, pIfmPktBuf, 0, sendLen + 30, (jbyte *)_ifmPktBuf);

	if(pFDataMem != NULL)
		free(pFDataMem);

    return sendLen + 30;
}

/**< end of file */
