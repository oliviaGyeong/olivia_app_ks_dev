
#define _GLOVAL_VARIABLE_
#define	SET_READ_DATA

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "pktDef.h"
#include "GVar.h"
#include "func.h"

// #include <android/log.h>

#define LOG_TAG "van_packet"
#define LOGI(...)   __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGD(...)   __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...)   __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

////////////////////////////////////////////////////////////////////////////////////////////////////
void _makeCommonHeader(char *pReqCmd, char *pTransDate, char *pTransSeqNo, void *pDest)
{
    common_header_t	*pCH=(common_header_t *)pDest;

    /**< set the common head */
    SetFieldData("appVer.",		pCH->applVer,		"A1",					       2,  2, LJFS);//전문버전
    SetFieldData("svcType.",	pCH->svcType,		"TF",				           2,  2, LJFS);//서비스종류
    SetFieldData("transType.", 	pCH->transType,		pReqCmd,				       2,  2, LJFS);//업무구분
    SetFieldData("sndType.", 	pCH->sndType,		"S",					       1,  1, LJFS);//전송구분
    SetFieldData("tmnlID.", 	pCH->tmnlID,		_pTmnlDownData->tmnlID,	      10, 10, LJFS);//터미널ID : 단말기 다운로드시 VAN에서 부여 받은 data
    SetFieldData("transDate.", 	pCH->transDate,		pTransDate,		              14, 14, LJFS);//거래일시
    SetFieldData("transSeqNo.",	pCH->transSeqNo,	pTransSeqNo,			      10, 10, LJFS);//거래일련번호의 관리는 VAN에서 ?
    SetFieldData("mctData.", 	pCH->mctData,		_pTmnlDownData->merchantData, 20, 20, LJFS);//가맹점정보(단말기 DOWNLOAD Data)
    SetFieldData("respCode.",	pCH->respCode,		"    ",					       4,  4, LJFS);//응답코드(SPACE fixed)

    return;
}

typedef struct UI_ENTRY_DATA {
    byte    transType       [ 2];   // 업무구분
    byte    transDate       [14];   // 거래일시
    byte    transSeqNo      [10];   // 거래일련번호
    byte    mctData         [20];   // 가맹점 정보
    byte    maskedTrack2    [40];
    byte    ksn             [10];
    byte    encTrack2       [48];
    byte    fSwipe          [ 1];   // 카드번호 입력 방법 : 'S':Swipe, 'I':IC, 'K':Key-In
    byte    inst            [ 2];   // 할부기간
    byte    transAmt        [12];   // 거래금액
    byte    feeAmt          [ 9];   // 봉사료
    byte    surTax          [ 9];   // 세금
    byte    orgApprovalNo   [12];   // 원승인번호
    byte    orgApprovalDate [ 8];   // 원승인일자
    byte    fTmnlCancel     [ 1];   // 단말취소구분
    byte    fElectSign      [ 1];   // 전자서명사용여부
    byte    esLength        [ 4];   // 전자서명 길이
    byte    esData          [1086]; // 전자서명 Data
    byte    fallbackCode    [ 2];   // fallback 사유
    byte    isIndividual    [ 1];   // 현금영수증 [개인, 법인 구분]
    byte    resonOfCancel   [ 1];   // 현금영수증 취소사유
} __attribute__ ((packed)) UiEntryData_t;  //size = 1293

static byte _tmpKSN[11]={0x00, };

/////////////////////////////////////////////////////////////////////////////////////////////////////
void _clearMem(byte *pMem, word memLen)
{
    memset(pMem, 0x00, memLen);
    memset(pMem, 0xff, memLen);
    memset(pMem, 0x00, memLen);
    return;
}

/////////////////////////////////////////////////////////////////////////////////////////////////////
/**< make 망취소 packet
 * 1. "CTEOT\x0d" 미수신시
 * 2. IC신용승인 거래시 IC 2차검증의 미수신시
 * 3. 승인요청 후 Response를 받지 못했을 때 ????? (원승인번호와 원승인 Data의 값이 없음.)
 * mskl-TODO: "CTACK\x0d" 전송위치 확인요망...
 * */
void _makeReqComTroubleCancelationPacket(__in UiEntryData_t *pUiEntry, __out kvqIC_TRANS_t *pKVQIT)
{
    SetFieldData("masked TRACK-2.", pKVQIT->maskedTrackII, " ", 1, 40, LJFS);
    SetFieldData("KSN", pKVQIT->KSN, " ", 1, 10, LJFS);
    SetFieldData("enciphered TRACK-2.", pKVQIT->encTrackII, " ", 1, 96, LJFS);
    
    /**< 카드번호입력방법 --------------------------------------------------------------------------*/
    pKVQIT->fSwipe[0] = pUiEntry->fSwipe[0];
//  SetFieldData("card-num input type", pKVQIT->fSwipe, pUiEntry->fSwipe, 1, 1, LJFS);
    
    /**< 원승인번호 (취소일때 설정):: 12---------------------------------------------------------------*/
    SetFieldData("original approval number.", pKVQIT->approvalNo, pUiEntry->orgApprovalNo, 12, 12, LJFS);
    
    /**< 원승인일자 (취소일때 설정)::  8---------------------------------------------------------------*/
    SetFieldData("original approval date.", pKVQIT->approvalDate, pUiEntry->orgApprovalDate, 8, 8, LJFS);
    
    /**< 단말취소구분 구분  :: 1 ----------------------------*/
    SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, "1", 1, 1, LJFS);    // 망상(단말기) 취소 fixed this function....
    
    pKVQIT->cCR[0] = 0x0d;

    return;
}

void _makeReqIcDeclineCancellationPacket(__in UiEntryData_t *pUiEntry, __out kvqIC_TRANS_t *pKVQIT)
{
    SetFieldData("masked TRACK-2.", pKVQIT->maskedTrackII, " ", 1, 40, LJFS);
    /**< IC승인시 IFM으로 부터 수신된 KSN값을 설정한다. */
    SetFieldData("KSN", pKVQIT->KSN, pUiEntry->ksn, 10, 10, LJFS);
    SetFieldData("enciphered TRACK-2.", pKVQIT->encTrackII, " ", 1, 96, LJFS);

    /**< 카드번호입력방법 --------------------------------------------------------------------------*/
    pKVQIT->fSwipe[0] = pUiEntry->fSwipe[0];
//  SetFieldData("card-num input type", pKVQIT->fSwipe, pUiEntry->fSwipe, 1, 1, LJFS);

    /**< 원승인번호(12)---------------------------------------------------------------*/
    SetFieldData("original approval number.", pKVQIT->approvalNo, pUiEntry->orgApprovalNo, 12, 12, LJFS);
    
    /**< 원승인일자(8)----------------------------------------------------------------*/
    SetFieldData("original approval date.", pKVQIT->approvalDate, pUiEntry->orgApprovalDate, 8, 8, LJFS);
    
    /**< 단말취소구분 구분  :: 1 ----------------------------*/
    SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, "3", 1, 1, LJFS);    // IC카드 취소 fixed this function....
    
    pKVQIT->cCR[0] = 0x0d;

    return;
}

void _makeReqCashReceiptPacket(__in UiEntryData_t *pUiEntry, _kirIC_TRANS_t *pKRPkt, __out kvqIC_TRANS_t *pKVQIT)
{
    /**< masked TRACK-2 ----------------------------------------------------------------------------*/
    if (pKRPkt->cmd[0] == 0x11 || pKRPkt->cmd[0] == 0x14) // present the receiving data from IFM...
    {
        SetFieldData("masked TRACK-2.", pKVQIT->maskedTrackII, pKRPkt->mTrack2, 40, 40, LJFS);
	    memcpy(_pReceiptPkt->cardNo, pKVQIT->maskedTrackII, 40);

        SetFieldData("KSN", pKVQIT->KSN, pKRPkt->ksn, 10, 10, LJFS);

        /**< IFM packet의KSN이space일경우non encryption으로간주한다. */
        if (!memcmp(pKRPkt->ksn, "          ", 10))
            SetFieldData("enciphered TRACK-2.", pKVQIT->encTrackII, pKRPkt->encTrack2, 48, 96, LJFS);
        else
            _h2nibble(pKRPkt->encTrack2, pKVQIT->encTrackII, 48);
    }
    else
    {
        SetFieldData("masked TRACK-2.", pKVQIT->maskedTrackII, pUiEntry->maskedTrack2, 13, 40, LJFS);
        // => KSN은 default값 'SPACE'로 채워진다.
        SetFieldData("KSN", pKVQIT->KSN, " ", 1, 10, LJFS);
        SetFieldData("masked TRACK-2.", pKVQIT->encTrackII, pUiEntry->maskedTrack2, 13, 40, LJFS);
    }

    /**< 카드번호입력방법 --------------------------------------------------------------------------*/
    pKVQIT->fSwipe[0] = pUiEntry->fSwipe[0];
//  SetFieldData("card-num input type", pKVQIT->fSwipe, pUiEntry->fSwipe, 1, 1, LJFS);
	_pReceiptPkt->swipeType[0] = pUiEntry->fSwipe[0];
    
    /**< 할부---------------------------------------------------------------------------------------*/
//  SetFieldData("inst.", pKVQIT->inst, pUiEntry->inst, 2, 2, LJFS);
	SetFieldData("inst.", pKVQIT->inst, "00", 2, 2, LJFS);    // "00"으로 fix
	memcpy(_pReceiptPkt->instalment, pKVQIT->inst, 2);

    /**< 거래금액(Authorized Amount) :: 12 ---------------------------------------------------------*/
    SetFieldData("transAmt.", pKVQIT->transAmt, pUiEntry->transAmt, 12, 12, RJLZ);
	memcpy(_pReceiptPkt->amount, &pKVQIT->transAmt[2], 10);

    /**< 봉사료:: 9 --------------------------------------------------------------------------------*/
    SetFieldData("fee.", pKVQIT->feeAmt, pUiEntry->feeAmt, 9, 9, RJLZ);
	memcpy(_pReceiptPkt->fee, pKVQIT->feeAmt, 9);
    
    /**< 세금(부가세) :: 9 --------------------------------------------------------------------------*/
    SetFieldData("tax", pKVQIT->surtax, pUiEntry->surTax, 9, 9, RJLZ);// UI를통한입력방식또는Demon에서수신
	memcpy(_pReceiptPkt->surTax, pKVQIT->surtax, 9);
    
    /**< 통화코드:: 3 ---------------------------------- fixed -------------------------------------*/
    SetFieldData("Currency Code.", pKVQIT->crcCode, "410", 3, 3, LJFS);// 원화(410), Dollar(840), 엔화(392) :: fixed
    
    /**< working index :: 2 ---------------------------- fixed -------------------------------------*/
    SetFieldData("working key index.", pKVQIT->keyIndex, "AA", 2, 2, LJFS);// 비밀번호미사용("AA"), 비암호화된비밀번호("BB")
    
    /**< 원승인번호 (취소일때 설정):: 12---------------------------------------------------------------*/
    SetFieldData("original approval number.", pKVQIT->approvalNo, pUiEntry->orgApprovalNo, 12, 12, LJFS);
	memcpy(_pReceiptPkt->orgApprovalNo, pKVQIT->approvalNo, 12);
    
    /**< 원승인일자 (취소일때 설정)::  8---------------------------------------------------------------*/
    SetFieldData("original approval date.", pKVQIT->approvalDate, pUiEntry->orgApprovalDate, 8, 8, LJFS);
	memcpy(_pReceiptPkt->orgApprovalDate, pKVQIT->approvalDate, 8);
    
    /**< 접속업체코드 :: 3 -------------------------------- fixed (SPACE) */
//    SetFieldData("contact code.", pKVQIT->connectCode, " ", 1, 3, LJFS);
    SetFieldData("contact code.", pKVQIT->connectCode, "TES", 3, 3, LJFS);  // 임의

    /**< 단말취소구분 구분  :: 1 ----------------------------*/
    if(pUiEntry->transType[1] == '3')   // 승인
        SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, " ", 1, 1, LJFS);
    else {
        // 정상처리"0", 망상취소"1", 통신장애"2", IC카드취소"3", 직전취소"5"
//      SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, pUiEntry->fTmnlCancel, 1, 1, LJFS);
        SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, "0", 1, 1, LJFS);
    }
    
    /**< PIN-Pad for Encryption index(PIN-Pad 암호화용인덱스) :: 2 :: fixed (SPACE) */
//    SetFieldData("PIN-Pad for Encryption index.", pKVQIT->pedEncIndex, " ", 1, 2, LJFS);
    SetFieldData("PIN-Pad for Encryption index.", pKVQIT->pedEncIndex, "01", 2, 2, LJFS);    // 임의
    
    /**< 단말 유.무선 코드 :: Contact or Contactless :: 1 -- fixed */
//  SetFieldData("card num entry type(ICC, RF...).", pKVQIT->fCardComType, "I", 1, 1, RJLZ);	// RF거래'R'...
    SetFieldData("card num entry type(ICC, RF...).", pKVQIT->fCardComType, "0", 1, 1, RJLZ);	// 임의 RF거래'R'...

    /**< 전자서명사용여부 :: 1 --------------------------- fixed */
    SetFieldData("whether to use digital signatures.", pKVQIT->fElectSign, "0", 1, 1, LJFS);	// 사용함"1", 사용안함"0", NO-CVM"2"

    /**< 현금영수증.개인/법인 구분 ---------------------------------------------*/
    SetFieldData("receipt of the cash.", pKVQIT->isIndividual, pUiEntry->isIndividual, 1, 1, LJFS);
    
    /**< 현금영수증. 취소사유 -------------------------------------------------*/
//  SetFieldData("reason of the cancellation of the cash receipt", pKVQIT->reasonOfCancelation, pUiEntry->resonOfCancel, 1, 1, LJFS);
    if(pUiEntry->transType[1] == '4')   // 취소거래
        SetFieldData("reason of the cancellation of the cash receipt", pKVQIT->reasonOfCancelation, "1", 1, 1, LJFS);
    else
        SetFieldData("reason of the cancellation of the cash receipt", pKVQIT->reasonOfCancelation, " ", 1, 1, LJFS);

    pKVQIT->cCR[0] = 0x0d;

    return;
}

/**<:: To VAN
 * 1. 신용승인(F1), 2. 신용취소(F2), 3. 현금영수증 승인(H3), 4. 현금영수증 취소(H4), 5. 망취소(FC), 6. IC승인거절 취소(FC)
 * 신용취소(일반) : 원승인번호, 원승인일자, 단말취소구분('0')
 *
 * @param env
 * @param this
 * @param pVanPktBuf    :: out :: request VAN packet
 * @param pRdPktBuf     ::  in :: packet received from IFM
 * @param pInputData    ::  in :: UI input Data & variable data
 * @param vanBufLen     ::  in :: Max size of 'pVanPktBuf'
 * @param rdBufLen      ::  in :: size(length) of 'pRdPktBuf'
 * @param inputLen      ::  in :: size(length) of 'pInputData'
 * @return              :: out :: size(length) of making packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_VanHostPacketManager_reqApprovalCancelToVAN (JNIEnv *env, jobject this,
                                                                                          jbyteArray pVanPktBuf,
                                                                                          jbyteArray pRdPktBuf,
                                                                                          jbyteArray pInputData,
                                                                                          jint vanBufLen,
                                                                                          jint rdBufLen,
                                                                                          jint inputLen)
{
    if(vanBufLen < sizeof(kvqIC_TRANS_t) || _pTmnlDownData == NULL/*not present terminal download data*/)
        return 0;

    if(inputLen != sizeof(UiEntryData_t))
        return 0;

    byte    *pIDMem=(byte *)malloc(sizeof(UiEntryData_t) + 1);      // memory for input data
    byte    *pRdPktMem=(byte *)malloc(sizeof(_kirIC_TRANS_t) + 1);  // memory for packet received from IFM
    
    memset(pRdPktMem, 0x00, (sizeof(_kirIC_TRANS_t) + 1));
    memset(pIDMem, 0x00, (sizeof(UiEntryData_t) + 1));
    memset(_vanPktBuf, 0x20, (sizeof(kvqIC_TRANS_t) + 1)); /**< initialize the sending packet buffer */

    ////////////////////////////////////////////////////////////////////////////////////////////////
    (*env)->GetByteArrayRegion(env, pInputData, 0, sizeof(UiEntryData_t), (jbyte *)pIDMem);
    /**< IFM으로 부터 받은 packet이 존재하면... */
    if(rdBufLen)
        (*env)->GetByteArrayRegion(env, pRdPktBuf, 0, rdBufLen, (jbyte *)pRdPktMem);
    else// all space...
        memset(pRdPktMem, 0x20, sizeof(_kirIC_TRANS_t));
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**< UI entry data */
    UiEntryData_t   *pUiEntry=(UiEntryData_t *)pIDMem;
    /**< packet received from IFM */
    _kirIC_TRANS_t   *pKRPkt=(_kirIC_TRANS_t *)pRdPktMem;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**< output : KOCES VAN Request IC Transaction */
    kvqIC_TRANS_t	*pKVQIT=(kvqIC_TRANS_t *)_vanPktBuf;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    char    szTransType[3]={'F', '2',};
    
    if(pUiEntry->transType[1] == 'C' || pUiEntry->transType[1] == '9')
        memcpy(szTransType, "F2", 2);
    else
        memcpy(szTransType, pUiEntry->transType, 2);
    
    memcpy(_pReceiptPkt->transType, szTransType, 2);
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**< set the common header :: F1, F2, RF, ... mskl-TODO: transaction sequence number 관리확인 */
    /**< make common header... */
    pUiEntry->transSeqNo[9] = 0x31; // 임의
    _makeCommonHeader(szTransType, (char *)pUiEntry->transDate, (char *)pUiEntry->transSeqNo, _vanPktBuf);
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**< 단말기 인증번호 -----------------------------------------------------------------------------*/
    // IFM hard coding data. 단말기 다운로드시 단말기 다운로드 data와 함께 저장된다. (ref. kvrTmnlDown_t)
    SetFieldData("tmnlCertiNum(IFM).", &pKVQIT->tmnlCertiNum[ 0], _pTmnlDownData->tmnlCertiNum, 16, 16, LJFS);
    // Payment-App hard coding data.
    SetFieldData("tmnlCertiNum(APP).", &pKVQIT->tmnlCertiNum[16], &_szTmnlCertiNo[16], 16, 16, LJFS);
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    switch(pUiEntry->transType[1])
    {
        case '3':   // 현금영수증 승인
        case '4':   // 현금영수증 취소
            _makeReqCashReceiptPacket(pUiEntry, pKRPkt, pKVQIT);
            goto END_OF_MAKE_PACKET;
        case 'C':   // 망상취소
            _makeReqComTroubleCancelationPacket(pUiEntry, pKVQIT);
            goto END_OF_MAKE_PACKET;
        case '9':   // IC card decline IC신용 취소  // mskl-TODO:call할 때 "F9"으로 설정 할것.
            _makeReqIcDeclineCancellationPacket(pUiEntry, pKVQIT);
            goto END_OF_MAKE_PACKET;
        case '1':   // IC신용 승인
        case '2':   // IC신용 취소
            break;
        default:
            if(pRdPktMem != NULL)
                free(pRdPktMem);
        
            if(pIDMem != NULL)
                free(pIDMem);

            return 0;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**< masked TRACK-2 ----------------------------------------------------------------------------*/
    SetFieldData("masked TRACK-2.", pKVQIT->maskedTrackII, pKRPkt->mTrack2, 40, 40, LJFS);
    memcpy(_pReceiptPkt->cardNo, pKVQIT->maskedTrackII, 40);
    
    /**< KSN(10) :: 비암호화시 반드시 space padding --------------------------------------------------*/
    if(rdBufLen != 0 && pKRPkt->cmd[0] == 0x11)  // IFM에서 Card 정보를 읽었을 때...(신용승인/취소, 현금영수증승인/취소)
    {
        SetFieldData("KSN", pKVQIT->KSN, pKRPkt->ksn, 10, 10, LJFS);
    
        /**< IFM packet의 KSN이 space일 경우 non encryption으로 간주 한다. */
        if (!memcmp(pKRPkt->ksn, "          ", 10))
            SetFieldData("enciphered TRACK-2.", pKVQIT->encTrackII, pKRPkt->encTrack2, 48, 96, LJFS);
        else
            _h2nibble(pKRPkt->encTrack2, pKVQIT->encTrackII, 48);
#if 0
        /**< IC승인요청일 경우 IC Card승인거절 취소를 위해 KSN을 임시로 저장한다... */
        if (!memcmp(pUiEntry->transType, "F1", 2) && pUiEntry->fSwipe == 'I')
            memcpy(_tmpKSN, pKRPkt->ksn, 10);
#endif
    }
    else
    {
        /**< enciphered Track-2는 space default......................... */
        /**< 취소구분이 IC카드 취소일때... 2nd Generator AC가 fail 일때.... */
        if(!memcmp(pUiEntry->transType, "F2", 2) && pUiEntry->fTmnlCancel[0] == '3')   // IFM packet parameter를 NULL로...
            SetFieldData("KSN", pKVQIT->KSN, pUiEntry->ksn, 10, 10, LJFS);
//          SetFieldData("KSN", pKVQIT->KSN, _tmpKSN, 10, 10, LJFS);
        else
            SetFieldData("KSN", pKVQIT->KSN, " ", 1, 10, LJFS);
    }

    /**< 카드번호 입력방법  --------------------------------------------------------------------------*/
    pKVQIT->fSwipe[0] = pUiEntry->fSwipe[0];
//    SetFieldData("card-num input type", pKVQIT->fSwipe, pUiEntry->fSwipe, 1, 1, LJFS);
    _pReceiptPkt->swipeType[0] = pUiEntry->fSwipe[0];
    
    /**< 할부 ---------------------------------------------------------------------------------------*/
    SetFieldData("inst.", pKVQIT->inst, pUiEntry->inst, 2, 2, LJFS);
	memcpy(_pReceiptPkt->instalment, pUiEntry->inst, 2);
	
    /**< 거래금액 (Authorized Amount) :: 12 ---------------------------------------------------------*/
    SetFieldData("transAmt.", pKVQIT->transAmt, pUiEntry->transAmt, 12, 12, RJLZ);
    memcpy(_pReceiptPkt->amount, &pKVQIT->transAmt[2], 10);
    
    /**< 봉사료 :: 9 --------------------------------------------------------------------------------*/
    SetFieldData("fee.", pKVQIT->feeAmt, pUiEntry->feeAmt, 9, 9, RJLZ);
    memcpy(_pReceiptPkt->fee, pKVQIT->feeAmt, 9);
    
    /**< 세금(부가세) :: 9 --------------------------------------------------------------------------*/
    SetFieldData("tax", pKVQIT->surtax, pUiEntry->surTax, 9, 9, RJLZ);// UI를 통한 입력 방식 또는 Demon에서 수신
    memcpy(_pReceiptPkt->surTax, pKVQIT->surtax, 9);
    
    /**< 통화코드 :: 3 ---------------------------------- fixed -------------------------------------*/
    SetFieldData("Currency Code.", pKVQIT->crcCode, "410", 3, 3, LJFS);// 원화(410), Dollar(840), 엔화(392) :: fixed

    /**< working index :: 2 ---------------------------- fixed -------------------------------------*/
    SetFieldData("working key index.", pKVQIT->keyIndex, "AA", 2, 2, LJFS);// 비밀번호미사용("AA"), 비암호화된 비밀번호("BB"),

    /**< enciphered PIN or clear PIN with space padding :: 16 --------------------------------------*/
    if(_N2H(pKVQIT->keyIndex) == 0xAA)
        SetFieldData("PIN Block(not use).", pKVQIT->pinBlock, "    ", 4, 16, LJFS);	// not used PIN
    else
    {
        if(_N2H(pKVQIT->keyIndex) == 0xBB)
            SetFieldData("PIN Block(clear PIN).", pKVQIT->pinBlock, "1234", 4, 16, LJFS);	// clear PIN & space padding
        else
            SetFieldData("PIN Block.(enciphered PIN block)", pKVQIT->pinBlock, "1234aabbccddeeff", 4, 16, LJFS);	// enciphered PIN block
    }

    /**< cardholder information :: 10 ------------------ fixed (SPACE) -----------------------------*/
    SetFieldData("cardholder info.", pKVQIT->cardholder, " ", 1, 10, LJFS);

    /**< ECI ??? :: ECI_Level :: 2 --------------------- fixed (SPACE) -----------------------------*/
    SetFieldData("ECI Level.", pKVQIT->ECI_LEVEL, "  ", 2, 2, LJFS); 		// "06", "07", "08", "09"
//    SetFieldData("ECI Level.", pKVQIT->ECI_LEVEL, "08", 2, 2, LJFS); 		// "06", "07", "08", "09"

    /**< Domain :: 30 ---------------------------------- fixed (SPACE) -----------------------------*/
    SetFieldData("Domain", pKVQIT->HOSTdomain, "          ", 10, 30, LJFS);
//    SetFieldData("Domain", pKVQIT->HOSTdomain, "WWW.BC.COM", 10, 30, LJFS);

    /**< MxID ??? :: 30 -------------------------------- fixed (SPACE) -----------------------------*/
    SetFieldData("MxID.", pKVQIT->mxID, " ", 1, 30, LJFS);		// 조흥 : 실 shopping mall의 ID(이름)
//    SetFieldData("MxID.", pKVQIT->mxID, "----------", 10, 30, LJFS);		// 조흥 : 실 shopping mall의 ID(이름)

    /**< MbizNo ??? :: 10 ------------------------------ fixed (SPACE) -----------------------------*/
    SetFieldData("MbizNo.", pKVQIT->mbizNo, " ", 1, 10, LJFS);	// Shopping mall의 사업자 등록번호

    /**< 원승인번호 (취소일때 설정):: 12---------------------------------------------------------------*/
    SetFieldData("original approval number.", pKVQIT->approvalNo, pUiEntry->orgApprovalNo, 12, 12, LJFS);
    memcpy(_pReceiptPkt->orgApprovalNo, pKVQIT->approvalNo, 12);
    
    /**< 원승인일자 (취소일때 설정)::  8---------------------------------------------------------------*/
    SetFieldData("original approval date.", pKVQIT->approvalDate, pUiEntry->orgApprovalDate, 8, 8, LJFS);
    memcpy(_pReceiptPkt->orgApprovalDate, pKVQIT->approvalDate, 8);

    /**< 포인트카드코드 :: 2 ------------------------------- fixed (SPACE) */
//    SetFieldData("point card code.", pKVQIT->pntCardCode, " ", 1, 2, LJFS);
    /**< 포인트카드번호 :: 40 ------------------------------ fixed (SPACE) */
//    SetFieldData("point card number.", pKVQIT->pntCardNum, " ", 1, 40, LJFS);
    /**< 접속업체코드  :: 3 -------------------------------- fixed (SPACE) */
//    SetFieldData("contact code.", pKVQIT->connectCode, " ", 1, 3, LJFS);
    SetFieldData("contact code.", pKVQIT->connectCode, "TES", 3, 3, LJFS);  // 임의
    /**< is temporary approval :: 1 ------------------- fixed --------------------------------------*/
    SetFieldData("flag temporary approval.", pKVQIT->fTmpApproval, " ", 1, 1, LJFS);	// 기승인"1", 승인"0"or" "

    /**< 단말취소구분 구분  :: 1 ----------------------------*/
    if(pUiEntry->transType[1] == '1')   // 승인
        SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, " ", 1, 1, LJFS);
    else {
        // 정상처리"0", 망상취소"1", 통신장애"2", IC카드취소"3", 직전취소"5"
        SetFieldData("terminal cancel type.", pKVQIT->fTmnlCancel, pUiEntry->fTmnlCancel, 1, 1, LJFS);
    }

    /**< PIN-Pad for Encryption index(PIN-Pad 암호화용 인덱스 ) :: 2 :: fixed (SPACE) */
//    SetFieldData("PIN-Pad for Encryption index.", pKVQIT->pedEncIndex, " ", 1, 2, LJFS);
    SetFieldData("PIN-Pad for Encryption index.", pKVQIT->pedEncIndex, "01", 2, 2, LJFS);    // 임의
    /**< BC paper issue 구분코드  :: 1 -------------------  fixed (SPACE) */
//    SetFieldData("BC Paperless.", pKVQIT->fBCPaperless, "1", 1, 1, LJFS);
    /**< 할인금액 :: 7 ----------------------------------*/
//  SetFieldData("Discount amount.", pKVQIT->discountedAmt, "0", 1, 7, RJLZ);
    SetFieldData("Discount amount.", pKVQIT->discountedAmt, " ", 1, 7, RJLZ);   // 임의
    /**< alliance service code(제휴 서비스  코드):: 2 ------ fixed (SPACE) */
//    SetFieldData("alliance service code.", pKVQIT->svcAffiliate, " ", 1, 2, RJLZ);

    /**< 단말 유.무선 코드 :: Contact or Contactless :: 1 -- fixed */
//  SetFieldData("card num entry type(ICC, RF...).", pKVQIT->fCardComType, "I", 1, 1, RJLZ);	// RF거래'R'...
    SetFieldData("card num entry type(ICC, RF...).", pKVQIT->fCardComType, " ", 1, 1, RJLZ);	// 임의 RF거래'R'...

    /**< 서명패드버전 :: 16 ------------------------------*/
//  SetFieldData("Version of the sign-pad.", pKVQIT->signPadVer, "PM1100LH.V.2.0.5", 16, 16, LJFS);
    SetFieldData("Version of the sign-pad.", pKVQIT->signPadVer, " ", 1, 16, LJFS); // 임의

    /**< 전자서명 사용여부  :: 1 --------------------------- fixed */
    SetFieldData("whether to use digital signatures.", pKVQIT->fElectSign, "0", 1, 1, LJFS);	// 사용함"1", 사용안함"0", NO-CVM"2"

    /**< length of the digital signatures :: 4 ------*/
    SetFieldData("length of the digital signature.", pKVQIT->fES_length, pUiEntry->esLength, 4, 4, RJLZ);
//  SetFieldData("length of the digital signature.", pKVQIT->fES_length, "    ", 4, 4, RJLZ);   // 임의

    /**< digital signature data(전자서명 데이터 ) :: 1086 */
    SetFieldData("digital signature data.", pKVQIT->fES_Data, pUiEntry->esData, 1086, 1086, LJFS);
//  SetFieldData("digital signature data.", pKVQIT->fES_Data, " ", 1, 1086, LJFS);

    /**< POS보안 사용 유무 :: 1 -------------------------- fixed '0'사용안함 */
//  SetFieldData("whether to use POS security.", pKVQIT->fSeed, "0", 1, 1, LJFS);	// '0'사용안함, '1'사용
    /**< POS보안 Serial Number :: 16 ---------------- fixed (SPACE) */
//  SetFieldData("POS security serial num.", pKVQIT->posSecurSN, " ", 1, 16, LJFS);
    /**< 암호화된 카드 데이터  :: 32 ---------------------- fixed (SPACE) */
//  SetFieldData("enciphered card data.", pKVQIT->cryptData, " ", 1, 32, LJFS);
    /**< 암호화에 사용된 Key :: 128 --------------------- fixed (SPACE) */
//  SetFieldData("the key used for encryption.", pKVQIT->cryptKey, " ", 1, 128, LJFS);
    /**< 비과세 금액  :: 9 ------------------------------ fixed (all '0') */
    SetFieldData("tax free amount.", pKVQIT->taxFreeAmt, "0", 1, 9, RJLZ);

    //////////////////////////////////////////////////////////////////////////////////////////////////
    if((!memcmp(pUiEntry->transType, "F1", 2) || !memcmp(pUiEntry->transType, "F2", 2)) &&
       pUiEntry->fSwipe[0] == 'I')
    {
        //------------------------------------------------------------------------------IC data fields
        /**< 이하 ICC 관련 데이터  ********************************************************************/
        /**< EMV 거래구분 :: Card Brand ------------------------*/
//        _h2nibble(pKRPkt->eData.emvBrand, pKVQIT->eIC.brandType, 1);
        SetFieldDataEx("Card Brand", pKVQIT->eIC.brandType, pKRPkt->eData.emvBrand, 1, 2);
        /**< POS Entry Mode Code :: "0510" */
        SetFieldDataEx("POS Entry Mode Code", pKVQIT->eIC.POSEntry, pKRPkt->eData.posEntryMode, 2, 4);
        /**< Card Sequence Number */
        SetFieldDataEx("Card Sequence Number", pKVQIT->eIC.cardSeqNum, pKRPkt->eData.cardSeqNum, 2, 4);
        /**< Additional POS Information :: "050500001000" */
        SetFieldDataEx("Additional POS Information", pKVQIT->eIC.addsPOSInfo, pKRPkt->eData.addPosInfo, 6, 12);
        /**< Issuer Script Result ---------------------------*/
        SetFieldDataEx("9F5B", pKVQIT->eIC.t9F5B, pKRPkt->eData.t9F5B, 23, 46);
        /**< Application Cryptogram -------------------------*/
        SetFieldDataEx("9F26", pKVQIT->eIC.t9F26, pKRPkt->eData.t9F26, 11, 22);
        /**< Cryptogram Information Data --------------------*/
        SetFieldDataEx("9F27", pKVQIT->eIC.t9F27, pKRPkt->eData.t9F27, 4, 8);
        /**< Issuer Application Data(IAD) -------------------*/
        SetFieldDataEx("9F10", pKVQIT->eIC.t9F10, pKRPkt->eData.t9F10, 35, 70);
        /**< Unpredictable Number ---------------------------*/
        SetFieldDataEx("9F37", pKVQIT->eIC.t9F37, pKRPkt->eData.t9F37, 7, 14);
        /**< Application Transaction Counter(ATC) -----------*/
        SetFieldDataEx("9F36", pKVQIT->eIC.t9F36, pKRPkt->eData.t9F36, 5, 10);
        /**< Terminal Verification Result (TVR) -------------*/
        SetFieldDataEx("95", pKVQIT->eIC.t95, pKRPkt->eData.t0095, 7, 14);
        /**< Transaction Date -------------------------------*/
        SetFieldDataEx("9A", pKVQIT->eIC.t9A, pKRPkt->eData.t009A, 5, 10);
        /**< Transaction Type -------------------------------*/
        SetFieldDataEx("9C", pKVQIT->eIC.t9C, pKRPkt->eData.t009C, 3, 6);
        /**< Transaction Amount -----------------------------*/
        SetFieldDataEx("9F02", pKVQIT->eIC.t9F02, pKRPkt->eData.t9F02, 9, 18);
        /**< Transaction Currency Code ----------------------*/
        SetFieldDataEx("5F2A", pKVQIT->eIC.t5F2A, pKRPkt->eData.t5F2A, 5, 10);
        /**< Application Interchange Profile (AIP) ----------*/
        SetFieldDataEx("82", pKVQIT->eIC.t82, pKRPkt->eData.t0082, 4, 8);
        /**< Terminal Country Code --------------------------*/
        SetFieldDataEx("9F1A", pKVQIT->eIC.t9F1A, pKRPkt->eData.t9F1A, 5, 10);
        /**< Amount Other -----------------------------------*/
        SetFieldDataEx("9F03", pKVQIT->eIC.t9F03, pKRPkt->eData.t9F03, 9, 18);
        /**< Cardholder Verification Method (CVM) Result ----*/
        SetFieldDataEx("9F34", pKVQIT->eIC.t9F34, pKRPkt->eData.t9F34, 6, 12);
        /**< Terminal Capabilities --------------------------*/
        SetFieldDataEx("9F33", pKVQIT->eIC.t9F33, pKRPkt->eData.t9F33, 6, 12);
        /**< Terminal Type ----------------------------------*/
        SetFieldDataEx("9F35", pKVQIT->eIC.t9F35, pKRPkt->eData.t9F35, 4, 8);
        /**< Interface Device(IFD) Serial Number ----------- mskl-TODO:설정 data확인요망 */
        SetFieldDataEx("9F1E", pKVQIT->eIC.t9F1E, pKRPkt->eData.t9F1E, 11, 22);
        /**< Transaction Category Code --------------------- mskl-TODO:고정된 값인지 확인요망 */
//      byte szDummy[6] = {0x9f, 0x53, 0x01, 0x13, 0x00,};
//      SetFieldDataEx("9F53", pKVQIT->eIC.t9F53, (byte *) szDummy, 4, 8);
        SetFieldDataEx("9F53", pKVQIT->eIC.t9F53, pKRPkt->eData.t9F53, 4, 8);
        /**< Dedicated File Name ---------------------------*/
        SetFieldDataEx("84", pKVQIT->eIC.t84, pKRPkt->eData.t0084, 18, 36);
        /**< Terminal Application Version Number -----------*/
        SetFieldDataEx("9F09", pKVQIT->eIC.t9F09, pKRPkt->eData.t9F09, 5, 10);
        /**< Transaction Sequence Counter ------------------*/
        SetFieldDataEx("9F41", pKVQIT->eIC.t9F41, pKRPkt->eData.t9F41, 7, 14);

        /**< Reason of Fallback :: IC승인전문 생성이므로 fallback Code는 "00"으로 고정한다.------------*/
#if 1
//      SetFieldData("reason of fallback.", pKVQIT->eIC.fallbackCode, pUiEntry->fallbackCode, 2, 2, LJFS);
        SetFieldData("reason of fallback.", pKVQIT->eIC.fallbackCode, "00", 2, 2, LJFS);
#else
        byte	_resonOfFallback = 0x00;
        _H2N(_resonOfFallback, (byte *)pKVQIT->eIC.fallbackCode);
#endif
   }

    //---------------------------------------------------------------------------------
#if 0   // fixed (SPACE)
    /**< DDC Flag : '1'환율 조회전 DDC승인, '2'환율조회 후 DDC승인, 그 외 Space */
    SetFieldData("DDC Flag.", pKVQIT->fDDC, " ", 1, 1, LJFS);
    /**< DDC 환율조회 거래일자  :: DDC Flag가 '2'일 경우 설정, DDC조회일자 입력(YYYYMMDD) */
    SetFieldData("DDC exchange rate inquiry date.", pKVQIT->ddcExcRateDate, " ", 1, 8, LJFS);
    /**< DDC 환율조회 거래키 : DDC Flag가 '2'일 경우 설정, DDC 조회응답(OLTP 8958)시 받은 조회거래키 입력 */
    SetFieldData("DDC exchange rate trans key.", pKVQIT->ddcExcRateKey, " ", 1, 12, LJFS);
    /**< DDC 환율조회 정보 : DDC Flag가 '2'일 경우 설정
     *   DDC거래통화(3)+DDC거래금액(12)+DDC거래환율 소수점자리수(1)+DDC거래환율(9)+미화(원화)매입금액(12) */
    SetFieldData("DDC exchange rate inquiry info.", pKVQIT->ddcExcRateInfo, " ", 1, 37, LJFS);
    /**< 유종코드 : space(유류지원 거래 아님) --------------------------------*/
    SetFieldData("oil code.", pKVQIT->oilCode, " ", 1, 1, LJFS);
    /**< 유량  -------------------------------------------------------------------*/
    SetFieldData("oil amount.", pKVQIT->oilAmount, "0.00", 4, 10, RJLZ);
    /**< 주유단가 ---------------------------------------------------------------*/
    SetFieldData("oil unit price.", pKVQIT->oilUnitPrice, "0", 1, 10, RJLZ);
    /**<면세유 유종코드 --------------------------------------------------------*/
    SetFieldData("oil code of the tax free.", pKVQIT->tFreeOilCode, " ", 1, 1, LJFS);
    /**< 면세유 유량 ------------------------------------------------------------*/
    SetFieldData("oil amount of the tax free.", pKVQIT->tFreeOilAmount, "0.00", 4, 6, RJLZ);
    /**< 면세유 공급구분 -------------------------------------------------------*/
    SetFieldData("supply type of the tax free oil.", pKVQIT->supplyCategory, " ", 1, 1, LJFS);
    /**< 외상거래 결제일자  ---------------------------------------------------*/
    SetFieldData("repay date of the debt trade.", pKVQIT->debtTradeRepayDate, " ", 1, 8, LJFS);
#else
    // mskl-TODO:실거래에서도 SPACE로 처리 된다면 제거할 것.
    /**< 유량  ------------------------------------------------------------------*/
//  SetFieldData("oil amount.", pKVQIT->oilAmount, "0.00", 4, 10, RJLZ);
    /**< 주유단가 ---------------------------------------------------------------*/
//  SetFieldData("oil unit price.", pKVQIT->oilUnitPrice, "0", 1, 10, RJLZ);
    /**< 면세유 유량 ------------------------------------------------------------*/
//  SetFieldData("oil amount of the tax free.", pKVQIT->tFreeOilAmount, "0.00", 4, 6, RJLZ);
#endif

//  /**< 현금영수증.개인/법인 구분 ---------------------------------------------*/
//    SetFieldData("receipt of the cash.", pKVQIT->isIndividual, pUiEntry->isIndividual, 1, 1, LJFS);
//  /**< 현금영수증. 취소사유 -------------------------------------------------*/
//    SetFieldData("reason of the cancellation of the cash receipt", pKVQIT->reasonOfCancelation, pUiEntry->resonOfCancel, 1, 1, LJFS);

#if 0   // fixed (SPACE)
    /**< 서비스구분  : pKVQIT->serviceType --------------------------------------------------------*/
    SetFieldData("service type.", pKVQIT->serviceType, "  ", 2, 2, LJFS);
    /**< 결제구분   : pKVQIT->repayType------------------------------------------------------------*/
    SetFieldData("repay type.", pKVQIT->repayType, "02", 2, 2, LJFS);
    /**< Filler     :pKVQIT->RFU------------------------------------------------------------------*/
    SetFieldData("Filler.", pKVQIT->RFU, " ", 1, 44, LJFS);
#endif

    pKVQIT->cCR[0] = 0x0d;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
END_OF_MAKE_PACKET:
    
    (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvqIC_TRANS_t), (jbyte *) _vanPktBuf);
    
    _clearMem(pRdPktMem, (sizeof(_kirIC_TRANS_t) + 1));
    _clearMem(pIDMem, (sizeof(UiEntryData_t) + 1));
    _clearMem(_vanPktBuf, (sizeof(kvqIC_TRANS_t) + 1));

    if(pRdPktMem != NULL)
        free(pRdPktMem);

    if(pIDMem != NULL)
        free(pIDMem);

    
    return sizeof(kvqIC_TRANS_t);
}

/** :: To VAN
 *
 * @param pVanPktBuf    :: VAN packet buffer
 * @param pRdPktBuf     :: packet from IFM
 * @param vanBufLen     :: size of VAN packet buffer
 * @param rdBufLen      :: length of IFM packet
 * @return              :: VAN packet length
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_AdminVanPacketManager_futureKeyUpdateRequest (JNIEnv *env, jobject this,
                                                                                                   __out jbyteArray pVanPktBuf,
                                                                                                   __in jbyteArray pRdPktBuf,
                                                                                                   __in jbyteArray pTransDate,
                                                                                                   __in jint vanBufLen,
                                                                                                   __in jint rdBufLen)
{
    if((vanBufLen < sizeof(kvqFK_UPDATE_t)) || (rdBufLen !=  sizeof(_kirFK_UPDATE_t)))
        return 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    byte    *pIPktMem=(byte *)malloc(sizeof(_kirFK_UPDATE_t) + 1);   // for IFM packet
    byte    *pTDMem=(byte *)malloc(15);

    memset(pIPktMem, 0x00, (sizeof(_kirFK_UPDATE_t) + 1));
    memset(pTDMem, 0x00, 15);

    /**< initialize the sending packet buffer */
    memset(_vanPktBuf, 0x20, sizeof(kvqFK_UPDATE_t));

    (*env)->GetByteArrayRegion(env, pRdPktBuf, 0, rdBufLen, (jbyte *)pIPktMem);
    (*env)->GetByteArrayRegion(env, pTransDate, 0, 14, (jbyte *)pTDMem);

    _kirFK_UPDATE_t   *pKRPkt=(_kirFK_UPDATE_t *)pIPktMem;
    kvqFK_UPDATE_t	 *pKVQIT=(kvqFK_UPDATE_t *)_vanPktBuf;

    /**< set the common header */
    _makeCommonHeader("F9", (char *)pTDMem, "0000000001", (char *)_vanPktBuf);

    SetFieldData("tmnlCertiNum.", pKVQIT->tmnlCertiNum, pKRPkt->tmnlCertiNum, 32, 32, LJFS);

//    SetFieldData("verifData.", pKVQIT->verifData, pKRPkt->verifData, 128, 256, LJFS);
    _h2nibble(pKRPkt->verifData, pKVQIT->verifData, 128);

    SetFieldData("Filler.", pKVQIT->RFU, " ", 1, 50, LJFS);

    pKVQIT->cCR[0] = 0x0d;

    (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvqFK_UPDATE_t), (jbyte *)_vanPktBuf);

    if(pIPktMem != NULL)
        free(pIPktMem);

    return sizeof(kvqFK_UPDATE_t);
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
typedef struct TMNL_DOWN_INPUT_DATA{
    byte	tmnlID          [10];	// terminal ID :: 요청시 space :: 다운로드시 VAN에서 부여받는다.
    byte    transDateTime   [14];	// 거래일시
    byte    versionSW       [ 5];	// S/W version
    byte    terminalSN      [20];	// terminal serial number
    byte    businessNum     [10];	// 사업자번호
    byte    merchantInfo    [20];   // 가맹점 정보
} __attribute__ ((packed)) tmnlDownInput_t;

/*  :: To VAN
 * "          |YYYYMMDDHHmmSS|ZH301|ZH17090013          |1234567890" : <터미널ID(10)|거래일시(14)|S/W-Version(5)|터미널SN(20)|사업자번호(1)>
 * =====================================================================================================================================
 * @param pVanPktBuf    :: VAN packet buffer
 * @param pInputData    :: UI input data
 * @param vanBufLen     :: sizeof VAN packet buffer
 * @param inputDataLen  :: length of input data
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_AdminVanPacketManager_deviceDownloadRequest (JNIEnv *env, jobject this,
                                                                                           __out jbyteArray pVanPktBuf,
                                                                                           __in jbyteArray pInputData,
                                                                                           __in jint vanBufLen,
                                                                                           __in jint inputDataLen)
{
    if((vanBufLen < sizeof(kvqTmnlDown_t)) || (inputDataLen != sizeof(tmnlDownInput_t)))
        return 0;

    byte    *pInMem=(byte *)malloc(sizeof(tmnlDownInput_t) + 1);

    memset(pInMem, 0x00, (sizeof(tmnlDownInput_t) + 1));

    /**< initialize the sending packet memory */
    memset(_vanPktBuf, 0x20, sizeof(kvqTmnlDown_t));

    (*env)->GetByteArrayRegion(env, pInputData, 0, inputDataLen, (jbyte *)pInMem);

    tmnlDownInput_t     *pInput=(tmnlDownInput_t *)pInMem;
    kvqTmnlDown_t	    *pKVQIT=(kvqTmnlDown_t *)_vanPktBuf;

    SetFieldData("appVer.",			pKVQIT->applVer,		"A1",				     2,  2, LJFS);//전문버전
    SetFieldData("srvType.",		pKVQIT->srvType,		"DN",				     2,  2, LJFS);//서비스종류
    SetFieldData("taskType.", 		pKVQIT->taskType,		"T1",				     2,  2, LJFS);//업무구분
    SetFieldData("sndType.", 		pKVQIT->sndType,		"S",				     1,  1, LJFS);//전송구분
//  SetFieldData("tmnlID.", 		pKVQIT->tmnlID,			" ",		             1, 10, LJFS);//터미널ID :: SPACE :: 다운로드시 VAN에서 부여 받는다.
    SetFieldData("tmnlID.", 		pKVQIT->tmnlID,			pInput->tmnlID,		    10, 10, LJFS);//터미널ID :: SPACE :: 다운로드시 VAN에서 부여 받는다.
    SetFieldData("transDate.",		pKVQIT->transDateTime,	pInput->transDateTime,	14, 14, LJFS);//거래일시
    SetFieldData("transSeqNo.",		pKVQIT->transSeqNum,	"0000000013",		    10, 10, LJFS);//거래일련번호(Terminal에서 Unique하게 생성)
    SetFieldData("merchantData",	pKVQIT->merchantData,	pInput->merchantInfo,	20, 20, LJFS);//가맹점사용영역
    SetFieldData("respCode.",		pKVQIT->respCode,		" ",				     1,  4, LJFS);//응답코드
    SetFieldData("tmnlSwVer",		pKVQIT->tmnlSwVer,		pInput->versionSW,	     5,  5, LJFS);//S/W Version
    SetFieldData("tmnlSN",			pKVQIT->tmnlSN,			pInput->terminalSN,	    20, 20, LJFS);//terminal serial number
    SetFieldData("businessNum",		pKVQIT->businessNum,	pInput->businessNum,	10, 10, LJFS);//사업자번호
    pKVQIT->cCR[0] = 0x0d;

    (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvqTmnlDown_t), (jbyte *) _vanPktBuf);

    if(pInMem != NULL)
        free(pInMem);

    return sizeof(kvqTmnlDown_t);
}

typedef struct RESP_MUTUAL_AUTH {
    byte    stx         [ 1];
    word	len;
    byte	cmd         [ 1];
    byte	tmnlAuthNum [32];
    byte	tmnlSN      [10];
    byte	reqKind     [ 4];         // "0001" Latest-Firmware, "0003"EMV Key(CAPK)
    byte	secKey      [128];        // Secure Key
    byte	emvKey      [10];         // EMV Key Version Information (미사용시 space)
    byte	filler      [40];
    byte    etx         [ 1];
    byte    lrc         [ 1];
} __attribute__ ((packed)) _kirMutualAuth_t;  // response mutual authentication from IFM

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**< :: To VAN
 *
 * @param pVanPktBuf :: VAN packet buffer
 * @param pRdPktBuf  :: IFM response packet
 * @param vanBufLen  :: sizeof VAN packet buffer
 * @param rdBufLen   :: length of IFM response packet
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_AdminVanPacketManager_mutualAuthenticationRequest (JNIEnv *env, jobject this,
                                                                                                        __out jbyteArray pVanPktBuf,
                                                                                                        __in jbyteArray pRdPktBuf,
                                                                                                        __in jbyteArray pTransDate,
                                                                                                        __in jint vanBufLen,
                                                                                                        __in jint rdBufLen)
{
    if((vanBufLen < sizeof(kvqMutualAuth_t)) || (rdBufLen != sizeof(_kirMutualAuth_t)))
        return 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    byte	*pRdPktMem=(byte *)malloc(sizeof(_kirMutualAuth_t)+1);
    byte    *pTDMem=(byte *)malloc(15);

    memset(pRdPktMem, 0x00, (sizeof(_kirMutualAuth_t)+1));

    /**< space initialize */
    memset(_vanPktBuf, 0x20, sizeof(kvqMutualAuth_t));

    (*env)->GetByteArrayRegion(env, pRdPktBuf, 0, rdBufLen, (jbyte *)pRdPktMem);
    (*env)->GetByteArrayRegion(env, pTransDate, 0, 14, (jbyte *)pTDMem);

    _kirMutualAuth_t	*pRdPkt=(_kirMutualAuth_t *)pRdPktMem;
    kvqMutualAuth_t		*pKVQPkt=(kvqMutualAuth_t *)_vanPktBuf;

    /**< fill the header */
    _makeCommonHeader("T2", (char *)pTDMem, "0000000001", (char *)_vanPktBuf);

    /**< 단말기 인증번호 */
    SetFieldData("tmnlCertiNum.", pKVQPkt->tmnlCertiNum, pRdPkt->tmnlAuthNum, 32, 32, LJFS);

    /**< terminal serial number */
    SetFieldData("terminalSN.", pKVQPkt->tmnlSerialNum, pRdPkt->tmnlSN, 10, 10, LJFS);

    /**< 요청 data 구분 */
    SetFieldData("request data.", pKVQPkt->reqDataType, pRdPkt->reqKind, 4, 4, LJFS);   // "0001" 최신firmware, "0002" firmware list

    /**< 보안키 (secure key) */
    _h2nibble(pRdPkt->secKey, pKVQPkt->securityKey, 128);                               // HEX string (인증키 값)

    /**< EMV key : 미사용 : Space padding */
    // pKVQPkt->emvKey => SPACE

    /**< Filler : Space padding */
    // pKVQPkt->RFU => SPACE

    /**< CR */
    pKVQPkt->cCR[0] = 0x0d;

    (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvqMutualAuth_t), (jbyte *)_vanPktBuf);

    if(pRdPktMem != NULL)
        free(pRdPktMem);

    return sizeof(kvqMutualAuth_t);
}

/**
 * @param pVanPktBuf    :: Terminal download packet received from VAN-HOSt
 * @param vanBufLen     :: length of terminal download packet
 */
JNIEXPORT void JNICALL Java_device_apps_pmpos_packet_AdminVanPacketManager_setTerminalDownData (JNIEnv *env, jobject this,
                                                                                      __out jbyteArray pVanPktBuf,
                                                                                      __in jint vanBufLen)
{
    _vanTmnlDN_Data[0] = 'P';  _vanTmnlDN_Data[0] = 'M';  // magic code
    
    (*env)->GetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvrTmnlDown_t), (jbyte *)&_vanTmnlDN_Data[2]);

    _pTmnlDownData=(kvrTmnlDown_t *)&_vanTmnlDN_Data[2];
    
    return;
}

/**<
 * @param pVanPktBuf    : buffer to set the terminal download data
 * @param vanBufLen     : size of the output buffer
 * @return              : length of the terminal download data
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_AdminVanPacketManager_getTerminalDownData (JNIEnv *env, jobject this,
                                                                                                __out jbyteArray pVanPktBuf,
                                                                                                __in jint vanBufLen)
{
    (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, sizeof(kvrTmnlDown_t), (jbyte *)&_vanTmnlDN_Data[2]);

    return sizeof(kvrTmnlDown_t);
}

//response to Daemon App
/**<
 * @param pVanPktBuf        : VAN response packet
 * @param vanBufLen         : length of VAN's response packet
 * @param pReceiptData      : receipt data buffer
 * @param receiptDataLen    : size of receipt data buffer
 * @return                  : size of packet
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_packet_ReceiptPrintData_respPaymentReceiptPacket (JNIEnv *env, jobject this,
                                                                                                __in jbyteArray pVanPktBuf,
                                                                                                __in jint vanBufLen,
                                                                                                __out jbyteArray pReceiptData,
                                                                                                __in jint receiptDataLen)
{
	if(receiptDataLen < sizeof(receiptToDemon_t))
		return 0;
	
	int     i=0;
	word    dataLen=(sizeof(receiptToDemon_t) + 2);
	byte    *pKVRPktMem=(byte *)malloc(vanBufLen + 1);  // memory for VAN response packet
	
	(*env)->GetByteArrayRegion(env, pVanPktBuf, 0, vanBufLen, (jbyte *)pKVRPktMem);
	
	kvrIC_TRANS_t   *pKVRPkt=(kvrIC_TRANS_t *)pKVRPktMem;
	
	_szDemonPkt[i++] = 0x02;
	memcpy(&_szDemonPkt[1], (byte *)&dataLen, 2);
	ReverseCopy(&_szDemonPkt[1], 2);
	i += 2;
	_szDemonPkt[i++] = 0xB1;
	i += sizeof(receiptToDemon_t);
	memcpy(_pReceiptPkt->respCode, pKVRPkt->cHead.respCode, 4);         // VAN 응답코드
	memcpy(_pReceiptPkt->respMsg, pKVRPkt->respMsg, 32);                // 응답메시지(거절시 설정됨, 선불카드시 잔액)
	memcpy(_pReceiptPkt->approvalDate, pKVRPkt->approvalDate, 14);      // 승인시간
	memcpy(_pReceiptPkt->approvalNo, pKVRPkt->approvalNo, 12);          // 승인번호
	memcpy(_pReceiptPkt->acquirerCode, pKVRPkt->acquirerCode, 4);       // 매입사코드
	memcpy(_pReceiptPkt->acquirerName, pKVRPkt->acquirerName, 8);       // 매입사명
	memcpy(_pReceiptPkt->transSeqNum, pKVRPkt->cHead.transSeqNo, 10);   // 거래일련번호
	memcpy(_pReceiptPkt->issuerCode, pKVRPkt->issuerCode, 4);           // 발급사코드
	memcpy(_pReceiptPkt->issuerName, pKVRPkt->issuerName, 12);          // 발급사명
	memcpy(_pReceiptPkt->cardType, pKVRPkt->cardType, 1);               // 카드구분 (1-신용카드, 2-체크카드, 3-기프트카드, 4-선불카드)
	/**< 단말기 다운로드 data */
	memcpy(_pReceiptPkt->terminalID, _pTmnlDownData->tmnlID, 10);
	memcpy(_pReceiptPkt->businessNo, _pTmnlDownData->businessNum, 10);
	memcpy(_pReceiptPkt->shopName, _pTmnlDownData->merchantName, 40);
	memcpy(_pReceiptPkt->shopOwonerName, _pTmnlDownData->representName, 20);
	memcpy(_pReceiptPkt->shopAddress, _pTmnlDownData->merchantAddr, 50);
	memcpy(_pReceiptPkt->shopTelNo, _pTmnlDownData->merchantPhoneNum, 15);
	_szDemonPkt[i++] = 0x03;
	_szDemonPkt[i] = _calcLRC(_szDemonPkt, i);
	i++;
	
	(*env)->SetByteArrayRegion(env, pReceiptData, 0, i, (jbyte *)_szDemonPkt);
	
	return i;
}


typedef enum PACKET_RECEIVE_STEP{
    RCV_INIT=0x01,
    RCV_CHK_STX,
    RCV_CHK_LEN1,
    RCV_CHK_LEN2,
    RCV_CHK_DATA
}pktRcvStep_t;

/**<
 *
 * @param pIfmPktBuf    :: final packet store buffer
 * @param pRcvData      :: received data
 * @param ifmBufLen     :: size of packet store buffer
 * @param chkDataLen    :: received data length
 * @param fInit         :: STEP init flag
 * @return
 */
JNIEXPORT jint JNICALL Java_device_apps_pmpos_RdiManager_checkReceivePacketFromIFM (JNIEnv *env, jobject this,
                                                                                            __out jbyteArray pIfmPktBuf,
                                                                                            __in jbyteArray pRcvData,
                                                                                            __in jint ifmBufLen,
                                                                                            __in jint chkDataLen,
                                                                                            __in jboolean fInit)
{
    static byte     rStep=RCV_INIT;    // receive Step
    static byte     szPktBuf[MAX_PKT_BUF + 64]={0x00, };
    static int      arrayPnt=1, pktDataLen=0;

    jint            loopCnt=chkDataLen, i=0;
    byte            *pTmpBuf=(byte *)malloc(chkDataLen + 1);
    
    (*env)->GetByteArrayRegion(env, pRcvData, 0, chkDataLen, (jbyte *)pTmpBuf);
    
    if(fInit == JNI_TRUE)
        rStep = RCV_INIT;
    
    while(loopCnt > 0)
    {
        switch(rStep)
        {
            case RCV_INIT:
                memset(szPktBuf, 0x00, sizeof(szPktBuf));
                arrayPnt = 1;   // 첫번째 byte는 정상수신여부를 check하는 flag값(0xff=정상, 0xfe는 비정상(receive fail))
                rStep = RCV_CHK_STX;
            case RCV_CHK_STX:
                if(pTmpBuf[i] == 0x02)
                {
                    szPktBuf[arrayPnt++] = pTmpBuf[i];
                    rStep = RCV_CHK_LEN1;
                }
                break;
            case RCV_CHK_LEN1:
                pktDataLen = (pTmpBuf[i] << 8);
                szPktBuf[arrayPnt++] = pTmpBuf[i];
                rStep = RCV_CHK_LEN2;
                break;
            case RCV_CHK_LEN2:
                pktDataLen |= pTmpBuf[i];
                pktDataLen += 1;
                
                if(pktDataLen >= MAX_PKT_BUF)
                {/**< data length is too big... */
                    rStep = RCV_INIT;
                    break;
                }
                
                szPktBuf[arrayPnt++] = pTmpBuf[i];
                rStep = RCV_CHK_DATA;
                break;
            case RCV_CHK_DATA:
                szPktBuf[arrayPnt++] = pTmpBuf[i];
                pktDataLen--;
                
                if(!pktDataLen)
                {
                    if(szPktBuf[arrayPnt - 2] != 0x03) {
                        /**< packet format error...*/
                        rStep = RCV_INIT;
                        break;
                    }
    
                    if(ifmBufLen >= arrayPnt) {
                        szPktBuf[0] = 0xff;
                        /**< packet receive complete...*/
                        (*env)->SetByteArrayRegion(env, pIfmPktBuf, 0, arrayPnt,
                                                   (jbyte *) &szPktBuf[0]);
                    }
                    else {
                        szPktBuf[0] = 0xfe;
                        /**< IFM receive packet buffer is too small... */
                        (*env)->SetByteArrayRegion(env, pIfmPktBuf, 0, ifmBufLen,
                                                   (jbyte *) &szPktBuf[0]);
                    }
                    rStep = RCV_INIT;
                    loopCnt = 0;    // escape the while loop :: 나머지 data(padding data)는 버림...
                }
                break;
                
            default:
                rStep = RCV_INIT;
                break;
        }
        
        i++;
	    loopCnt--;
        continue;
    }
    
    /**< malloc memory free... */
    if(pTmpBuf != NULL)
        free(pTmpBuf);
    
    /**< check the packet receiving completion... */
    if(szPktBuf[0] != 0x00 && loopCnt == -1)
        return arrayPnt;
    
    return 0;
}

/**<
 *
 * @param pIfmPktBuf    :: final packet store buffer
 * @param pRcvData      :: received data
 * @param ifmBufLen     :: size of packet store buffer
 * @param chkDataLen    :: received data length
 * @param fInit         :: STEP init flag
 * @return
 */

JNIEXPORT jint JNICALL Java_device_apps_pmpos_TCPClient_checkReceivePacketFromVAN (JNIEnv *env, jobject this,
                                                                                            __out jbyteArray pVanPktBuf,
                                                                                            __in jbyteArray pRcvData,
                                                                                            __in jint vanBufLen,
                                                                                            __in jint chkDataLen,
                                                                                            __in jboolean fInit)
{
    static jboolean     fPktStart=JNI_FALSE;
    static byte         szPktBuf[MAX_PKT_BUF]={0x00, };
    static int          arrayPnt=1;
    int                 i=0;

    byte                *pTmpBuf=(byte *)malloc(chkDataLen + 1);

    if(fInit == JNI_TRUE)
        fPktStart = JNI_FALSE;

    (*env)->GetByteArrayRegion(env, pRcvData, 0, chkDataLen, (jbyte *)pTmpBuf);

    if(fPktStart == JNI_FALSE)
    {
        for(; i < chkDataLen; i++)
        {
            if (pTmpBuf[i] == 'A')
            {
                szPktBuf[arrayPnt++] = pTmpBuf[i];
                continue;
            }

            if(szPktBuf[1] == 'A')
            {
                if(pTmpBuf[i] == '1' || pTmpBuf[i] == '2')
                {
                    szPktBuf[arrayPnt++] = pTmpBuf[i];
                    fPktStart = JNI_TRUE;
                    break;
                }
                else
                {/**< reinitialize...*/
                    arrayPnt = 1;
                    szPktBuf[1] = 0x00;
                }
            }
            continue;
        }
    }

    for(; i < chkDataLen; i++)
    {
        szPktBuf[arrayPnt++] = pTmpBuf[i];

        /**< check the buffer overflow...*/
        if(arrayPnt >= vanBufLen)
        {
            szPktBuf[0] = 0xfe;
            (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, vanBufLen,
                                       (jbyte *) &szPktBuf[0]);
            break;
        }

        if(pTmpBuf[i] == 0x0d)
        {
            szPktBuf[0] = 0xff;
            (*env)->SetByteArrayRegion(env, pVanPktBuf, 0, arrayPnt,
                                       (jbyte *) &szPktBuf[0]);
            break;
        }
    }

    /**< malloc memory free... */
    if(pTmpBuf != NULL)
        free(pTmpBuf);

    /**< check the packet receiving completion... */
    if(szPktBuf[0] != 0x00)
    {
        fPktStart = JNI_FALSE;
        return arrayPnt;
    }

    return 0;
}

/**< end of file */
