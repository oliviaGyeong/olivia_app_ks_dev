//
// Created by mskl on 2017-10-24.
//

#ifndef PMPOS_PKTDEF_H
#define PMPOS_PKTDEF_H

#define LJFS        0x30    // Left Justified Following Space
#define RJLZ        0x31    // Right Just Leading Zero

#define __in
#define __out

typedef unsigned char       byte;
typedef unsigned short      word;
typedef unsigned long       dword;
typedef unsigned int        uint;


#define MAX_PKT_BUF     2560 + 64

//////////////////////////////////////////////////////////////////////
// RESPONSE
typedef struct RESP_EMV_DATA {
    byte	emvBrand[1];
    byte	posEntryMode[2];
    byte	cardSeqNum[2];
    byte	addPosInfo[6];
    byte	t9F5B[23];		// issuer script result
    byte	t9F26[11];	// Application Cryptogram(AC)
    byte	t9F27[4];	// Cryptogram Information
    byte	t9F10[35];	// issuer application data (IAD)
    byte	t9F37[7];	// unpredictable number
    byte	t9F36[5];	// application transaction counter (ATC)
    byte	t0095[7];	// terminal verification result (TVR)
    byte	t009A[5];	// transaction data
    byte	t009C[3];	// transaction type
    byte	t9F02[9];	// transaction amount
    byte	t5F2A[5];	// transaction currency code
    byte	t0082[4];	// application interchange profile (AIP)
    byte	t9F1A[5];	// terminal currency code
    byte	t9F03[9];	// amount other
    byte	t9F34[6];	// cardholder verification method (CVM) result
    byte	t9F33[6];	// terminal capabilities
    byte	t9F35[4];	// terminal type
    byte	t9F1E[11];	// interface device(IFD) serial number
    byte	t9F53[4];	// transaction category code
    byte	t0084[18];	// dedicated file name
    byte	t9F09[5];	// terminal application version number
    byte	t9F41[7];	// transaction sequence counter
} __attribute__ ((packed)) _kEMV_t;

/*
typedef struct RESP_IC_TRANS_REMAINDER{
    byte		feildSeparate[1];
    byte		pwdLen		[ 2];
    byte		indexWKey	[ 2];
    byte		encPinBlock	[16];
    byte        etx         [ 1];
    byte        lrc         [ 1];
} __attribute__ ((packed)) _kirRemainder_t;
*/

typedef struct RESP_IC_TRANS {
    byte        stx         [ 1];
    word		len;
    byte		cmd			[ 1];
    byte		tmnlCertiNum[32];
    byte		mTrack2		[40];
    byte		ksn			[10];
    byte		encTrack2	[48];
    _kEMV_t		eData;
    byte		mTrack2Ex	[40];
    byte		ksnEx		[10];
    byte		encTrans2Ex	[48];
    byte		respCode	[2];
    byte		productInfo	[16];	//
    word		signDataLen;        // 0으로 fix
    byte        variableBuf [1559]; // 1536 + 1 + 2 + 2 + 16 + ETX(1) + LRC(1)
/*
	byte		feildSeparate[1];
	byte		pwdLen		[ 2];
	byte		indexWKey	[ 2];
	byte		encPinBlock	[16];
*/
} __attribute__ ((packed)) _kirIC_TRANS_t;    // card read response from IFM

// Added by irene.choi - start
typedef struct RESP_FK_UPDATE {
    byte    stx                 [1];
    byte    dataLen             [2];
    byte    cmdId               [1];
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	verifData			[128];	// 검증요청데이터
    byte    verifDataResult     [2];
    byte    etx                 [1];
    byte    lrc                 [1];
} __attribute__ ((packed)) _kirFK_UPDATE_t;
// Added by irene.choi - end

// TYPE DEFINE THE VAN PACKET
typedef struct COMMON_HEADER_FOR_VAN{
    byte	applVer		[ 2];	// 전문버전 ("A1"비암호화, "A2"암호화)
    byte	svcType		[ 2];	// 서비스 종류("TF", "DN"단밀기다운로드)
    byte	transType	[ 2];	// 업무 구분("F1"신용승인, "F2"신용취소, "FC"망취소)
    byte	sndType		[ 1];	// 전송구분 : 'S'(PG->VAN), 'R'(VAN->PG)
    byte	tmnlID		[10];	// 터미널 ID
    byte	transDate	[14];	// 거래일시 : YYMMDDhhmmss
    byte	transSeqNo	[10];	// 거래일련번호	- 응답시 반환
    byte	mctData		[20];	// Merchant Datas(가뱅점 정보) - 응답시 반환
    byte	respCode	[ 4];	// 응답코드 (요청시 space padding)
} __attribute__ ((packed)) common_header_t;	// total : 65-Byte

////////////////////////////////////////////////////////////////////
// VAN REQUEST
typedef struct EMV_ITEM_LIST_FOR_VAN {
    byte	brandType	[ 2];	// 'V'VISA, 'M'Master, 'J'JCB, 'A'AMEX, 'D'DFS, 'V'Local- VSDC,
    // 'M'Local-MChip, 'V'Local-CUP, 'C'PBOC, 'M'KS, 'F'Fallback(Brand우선, 구분이 안될때 'F')
    byte	POSEntry	[ 4];	// POS Entry Mode Code : IC에서 추출한 값. Fallback일때 "\x90\x10"으로 설정하고 기타 IC관련 field는 NULL
    byte	cardSeqNum	[ 4];	// 3BCD, Card Sequence Number
    byte	addsPOSInfo	[12];	// additional POS Information
    byte	t9F5B		[46];	// Issuer Script Result - req일때 SPACE처리? :: TLV로 만들고 Nibble값으로 변환 후 처리
    byte	t9F26		[22];	// Application Cryptogram (AC)
    byte	t9F27		[ 8];	// Cryptogram Information
    byte	t9F10		[70];	// Issuer Application Data (IAD)
    byte	t9F37		[14];	// Unpredictable Number
    byte	t9F36		[10];	// Application Transaction Counter(ATC)
    byte	t95			[14];	// Terminal Verification Result (TVR)
    byte	t9A			[10];	// Transaction Date
    byte	t9C			[ 6];	// Transaction Type
    byte	t9F02		[18];	// Transaction Amount
    byte	t5F2A		[10];	// Transaction Currency Code
    byte	t82			[ 8];	// Application Interchange Profile (AIP)
    byte	t9F1A		[10];	// Terminal Country Code
    byte	t9F03		[18];	// Amount Other
    byte	t9F34		[12];	// Cardholder Verification Method (CVM)
    byte	t9F33		[12];	// Terminal Capabilities
    byte	t9F35		[ 8];	// Terminal Type
    byte	t9F1E		[22];	// Interface Device(IFD) Serial Number
    byte	t9F53		[ 8];	// Transaction Category Code
    byte	t84			[36];	// Decicated File Name
    byte	t9F09		[10];	// Terminal Application Version Number
    byte	t9F41		[14];	// Transaction Sequence Counter
    byte	fallbackCode[ 2];	// Fallback 사유
} __attribute__ ((packed)) kvEMV_t;

typedef struct 	REQ_IC_CREDIT_TO_VAN {
    common_header_t		cHead;
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	maskedTrackII		[40];
    byte	KSN					[10];
    byte	encTrackII			[96];	// 암호화 안되었을 경우는 나머지는 space padding.
    byte	fSwipe				[ 1];	// flag of MS Swipe
    byte	inst				[ 2];	// 할부개월
    byte	transAmt			[12];	// 거래금액
    byte	feeAmt				[ 9];	// 봉사료
    byte	surtax				[ 9];	// 세금(부가세)
    byte	crcCode				[ 3];	// currency Code : 원화(410), Dollar(840), 엔화(392)...
    byte	keyIndex			[ 2];	// working key index
    byte	pinBlock			[16];	//
    byte	cardholder			[10];	// information of cardholder
    byte	ECI_LEVEL			[ 2];	// ?????
    byte	HOSTdomain			[30];	// ???
    byte	mxID				[30];	// ???
    byte	mbizNo				[10];	//
    byte	approvalNo			[12];	// 승인번호(왼쪽정렬)
    byte	approvalDate		[ 8];	// 승인일자(YYYYMMDD) - 취소, 전화등록시 사용
    byte	pntCardCode			[ 2];	// point card code
    byte	pntCardNum			[40];	// point card number
    byte	connectCode			[ 3];	// 접속업체 코드
    byte	fTmpApproval		[ 1];	// 가승인 구분(1 : 가승인, 0 or ' ' : 승인)
    byte	fTmnlCancel			[ 1];	// 단말취소 구분
    byte	pedEncIndex			[ 2];	// Encryption Key-Index for PIN
    byte	fBCPaperless		[ 1];	// BC Paperless 구분 코드
    byte	discountedAmt		[ 7];	// 할인금액
    byte	svcAffiliate		[ 2];	// 제휴서비스 코드
    byte	fCardComType		[ 1];	// contact or contactless (RF)
    byte	signPadVer			[16];	// sign-pad serial number
    byte	fElectSign			[ 1];	// 전자서명 사용여부(sign-pad 사용여부)
    byte	fES_length			[ 4];	// 전자서명 data 길이
    byte	fES_Data			[1086];	// 전자서명 data
    byte	fSeed				[ 1];	// POS security 사용 유무
    byte	posSecurSN			[16];	// POS security Serial Number
    byte	cryptData			[32];	// enciphered card data
    byte	cryptKey			[128];	// be used a key for encryption
    byte	taxFreeAmt			[ 9];	// 비과세 금액
    ///////////////////////////////////////////////////////////////////////
    kvEMV_t	eIC;						// EMV IC Card 관련항목
    ///////////////////////////////////////////////////////////////////////
    byte	fDDC				[ 1];	// DDC Flag (환율 관련)
    byte	ddcExcRateDate		[ 8];	// DDC 환율 조회 일자
    byte	ddcExcRateKey		[12];	// DDC 환율 조회 Key
    byte	ddcExcRateInfo		[37];	// DDC 환율 조회 정보
    byte	oilCode				[ 1];	// 유종코드
    byte	oilAmount			[10];	// 유량
    byte	oilUnitPrice		[ 7];	// 주유단가
    byte	tFreeOilCode		[ 1];	// tax-free : 유종코드
    byte	tFreeOilAmount		[ 6];	// tax-free : 유량
    byte	supplyCategory		[ 1];	// 공급구분
    byte	debtTradeRepayDate	[ 8];	// 외상거래결제 일자
    byte	isIndividual		[ 1];	// 개인(Individual).법인(Company) 구분
    byte	reasonOfCancelation	[ 1];	// 취소 사유
    byte	serviceType			[ 2];	// 서비스 구분
    byte	repayType			[ 2];	// 결제구분
    byte	RFU					[44];	// filler(여유필드)
    byte	cCR					[ 1];	// carriage return : 요청전문의 끝
} __attribute__ ((packed)) kvqIC_TRANS_t;	// IC신용 요청 전문 : total = 2285-Byte

// Added by irene.choi - start
typedef struct 	REQ_FK_UPDATE_TO_VAN {
    common_header_t		cHead;
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	verifData			[256];	// 검증요청데이터
    byte	RFU					[50];	// filler(여유필드)
    byte	cCR					[ 1];	// carriage return : 요청전문의 끝
} __attribute__ ((packed)) kvqFK_UPDATE_t;

typedef struct REQ_TERMINAL_DOWNLOAD{
    byte	applVer				[ 2];	// 전문버전
    byte	srvType				[ 2];	// Service 구분
    byte	taskType			[ 2];	// 업무구분 : T1 (단말기 다운로드)
    byte	sndType				[ 1];	// 전송구분 ('S' : PG->VAN, 'R' : VAN->PG)
    byte	tmnlID				[10];	// VAN에서 부여한 단말기 번호(Terminal ID)
    byte	transDateTime		[14];	// 거래일시 : YYYYMMDDhhmmss
    byte	transSeqNum			[10];	// 거래 일련번호 (Terminal에서 Unique하게 생성)
    byte	merchantData		[20];	// 가맹점 사용영역
    byte	respCode			[ 4];	// 응답코드 : 요청시에는 Space Padding
    byte	tmnlSwVer			[ 5];	// ????? serial number 체계 확인 후 부여요망
    byte	tmnlSN				[20];	// Terminal Serial Number
    byte	businessNum			[10];	// 사업자 번호
    byte	cCR					[ 1];
} __attribute__ ((packed)) kvqTmnlDown_t;

/**< flash memory에  구조체 그래로 저장 후 사용... */
typedef struct RESP_TERMINAL_DOWNLOAD{
    byte	applVer				[ 2];	// 전문버전
    byte	srvType				[ 2];	// Service 구분 : DN
    byte	taskType			[ 2];	// 업무구분 : T1 (단말기 다운로드)
    byte	sndType				[ 1];	// 전송구분 ('S' : PG->VAN, 'R' : VAN->PG)
    byte	tmnlID				[10];	// VAN에서 부여한 단말기 번호(Terminal ID)
    byte	transDateTime		[14];	// 거래일시 : YYYYMMDDhhmmss
    byte	transSeqNum			[10];	// 거래 일련번호 (Terminal에서 Unique하게 생성)
    byte	merchantData		[20];	// 가맹점 사용영역
    byte	respCode			[ 4];	// 응답코드 : 요청시에는 Space Padding
    byte	tmnlSN				[20];	// Terminal Serial Number
    byte	respMsg				[40];	// 응답메시지
    byte	tmnlSwVer			[ 5];	// ????? serial number 체계 확인 후 부여요망
    byte	merchantName		[40];	// 가맹점명
    byte	businessNum			[10];	// 사업자 번호
    byte	representName		[20];	// 대표자 명 (representative name)
    byte	merchantAddr		[50];	// 가맹점 주소
    byte	merchantPhoneNum	[15];	// 가맹점 전화 번호
    byte	eSerialNum			[15];	// electronic serial number
    byte	workingKeyIndex		[ 2];	// working key index
    byte	workingKey			[16];	// working key
    byte	workingKeyMaster	[16];	// working key master
    byte	pntCardName_01		[14];	// point card name 1
    byte	pntCardName_02		[14];
    byte	pntCardName_03		[14];
    byte	pntCardName_04		[14];
    byte	pntCardName_05		[14];
    byte	pntCardName_06		[14];
    byte	pntCardName_07		[14];
    byte	pntCardName_08		[14];
    byte	pntCardName_09		[14];
    byte	pntCardName_10		[14];
    byte	pntCardName_11		[14];
    byte	pntCardName_12		[14];
    byte	pntCardName_13		[14];
    byte	pntCardName_14		[14];
    byte	pntCardName_15		[14];
    byte	pntCardName_16		[14];
    byte	pntCardName_17		[14];
    byte	pntCardName_18 		[14];
    byte	pntCardName_19		[14];
    byte	pntCardName_20		[14];
    byte	cCR					[ 1];
    byte    tmnlCertiNum        [32];
} __attribute__ ((packed)) kvrTmnlDown_t;

typedef struct REQ_MUTUAL_AUTHENTICATION {
    common_header_t		cHead;
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	tmnlSerialNum		[10];	// 단말기 Serial Number
    byte	reqDataType			[ 4];	// 요청데이터 구분 : "0001"-최신Firmware, "0002"-Firmware List
    byte	securityKey			[256];	// 보안키
    byte	emvKey				[10];	// IC 인증에 필요한 EMV Key
    byte	RFU					[40];	//
    byte	cCR					[ 1];
} __attribute__ ((packed)) kvqMutualAuth_t;

typedef struct RESP_MUTUAL_AUTHENTICATION {
    common_header_t		cHead;
    byte	tmnlCertiNum		[32];	// 단말기 인증번호
    byte	tmnlSerialNum		[10];	// 단말기 Serial Number
    byte	respMsg				[80];	// 응답메시지 - TMS Server에  생성된 인증키(token_cs) 경로 : /home/TMS/token_cs/
    byte	connectSecurityKey	[96];	// 접속보안키
    byte	authKeyPathName		[256];	// 인증키 경로명
    byte	numOfData			[ 4];	// 데이터 개수
    byte	sndProtocol			[ 5];	// 전송 Protocol
    byte	downURLorIP			[80];	// Download Server Address (server의 URL or IP Address)
    byte	downSvrPort			[ 5];	// Download Server COM Port (URL의 경우는  없음)
    byte	accountID			[16];	// Download Server's Account Name (user ID)
    byte	accountPWD			[16];	// Download Server's Account Password
    byte	fwVERorDataType		[ 5];	// Firmware Version or Data 구분
    byte	fwVERorDataExpl		[80];	// Explanation of Version or Data
    byte	fileName		    [256];	// 데이터 파일명(경로포함) 혹은 파일 번호
    byte	fileSize		    [10];	// 데이터 파일크기
    byte	fileChkType		    [ 5];	// 파일 체크방식 ("SHA2")
    byte	fileChkSum		    [64];	// Data of File's Check Sum (SHA-2)
    byte	fileDecKey			[32];	// Decryption Key of File
    byte	cCR					[ 1];	//
} __attribute__ ((packed)) kvrMutualAuth_t;

typedef struct 	RESP_IC_CREDIT_FROM_VAN{
    common_header_t		cHead;
    byte	vanUniqNum			[12];	// VAN에서 부여하는 거래 고유번호 (LJFS<Left Justified Following Space>)
    byte	approvalNo			[12];	// 승인번호
    byte	approvalDate		[14];	// 승인시간(YYMMDDhhmmss)
    byte	brandRespCode		[ 2];	// 카드사 응답코드
    byte	respMsg				[32];	// 응답메시지(거절 시 응답 message)
    byte	kindOfCard			[12];	// 카드종류명
    byte	issuerCode			[ 4];	// 발급사 코드
    byte	issuerName			[12];	// 발급사 명
    byte	acquirerCode		[ 4];	// 매입사 코드
    byte	acquirerName		[ 8];	// 매입사 명
    byte	merchantNum			[16];	// 가맹점 번호
    byte	workingKeyIndex		[ 2];	//
    byte	TMK					[16];	// working key 비밀번호 사용시 TMK(Terminal Master Key, [TPK]Terminal Protection Key)
    byte	pntCardCode			[ 2];	// Point Card Code
    byte	pntCardName			[ 8];	// Point Card' Name
    byte	savedPnt			[ 9];	// 적립 Point
    byte	availablePnt		[ 9];	// 가용 Point
    byte	accumulatePnt		[ 9];	// 누적 Point
    byte	discountRate		[ 9];	// 할인율
    byte	savedApprovalNum	[20];	// 적립 승인 번호
    byte	savedMerchantNum	[15];	// 적립가맹점 번호
    byte	savedRespCode		[ 4];	// 적립 응답코드
    byte	savedRespMsg		[48];	// 적립 응답메시지
    byte	isDDC				[ 1];	// DDC 여부
    byte	isEDI				[ 1];	// EDI 여부
    byte	cardType			[ 1];	// 카드구분 : 1-신용, 2-체크, 3-키프트, 4-선불
    byte	publicKey			[140];	//
    byte	transKey			[12];	// 거래키
    byte	additionalRespData	[52];	//
    byte	issuerAuthData		[24];	// Issuer Authentication Data
    byte	issuerScript		[520];	// Tag 71, Tag 72
    byte	ncCode				[ 3];	// 자국 통화코드 (National Currency Code) : "KRW", "USD"
    byte	ncNum				[ 3];	// 자국통화번호 (National Currency Number) : "410", "840" : RJLZ(Right Justified Leading Zero)
    byte	ncAmount			[12];	// 자국통화금액 (National Currency Amount)
    byte	ncMinorUnit			[ 1];	// 자국통화 Minor Unit
    byte	ddcExcRate			[ 9];	// 환율 (Exchange Rate) : DDC 승인 거래 시 전송
    byte	ddcExcRateMinorUnit	[ 1];	// 환율 소수점 표기 (Exchange Rate Minor Unit) : DDC 승인거래 시 전송
    byte	ivtRate				[ 9];	// Inverted Rate
    byte	ivtRateMinorUnit	[ 1];	// Inverted Rate Minor Unit
    byte	ivtRateDispUnit		[ 1];	// Inverted Rate Display Unit : RJLZ : "0" : 1단위 : USD 1 = KRW xxxx.xxx, "2" : 100단위 JPY 100 = KRW xxxx.xx
    byte	markUpPctg			[ 8];	// Mark Up Percentage
    byte	markUpPctgDispUnit	[ 1];	// Mark Up Percentage Display Unit
    byte	cmsPctg				[ 8];	// Commission Percentage
    byte	cmsPctgMinorUnit	[ 1];	// Commission Percentage Minor Unit
    byte	cmsCrncNum			[ 3];	// Commission Currency Number
    byte	cmsAmount			[12];	// Commission Amount
    byte	cmsAmtMinorUnit		[ 1];	// Commission Amount Minor Unit
    byte	rateID				[20];	//
    byte	excRateExpirTime	[22];	// 환율만료 시간 (Exchange Rate Expiration Time) : "20170803111700GMT+0900"
    byte	excRateLookupDate	[ 8];	// 환율조회 일자 (Exchange Rate Lookup Date) : YYYYMMDD
    byte	excRateLoopupKey	[12];	// 환율조회 거래키 (Exchange Rate Lookup Transaction Key)
    byte	carNumber			[12];	// 차량번호
    byte	companyName			[20];	// 법인명
    byte	freightCompanyCode	[ 4];	// 화물운송사 코드
    byte	dukptKeyExpRDs		[ 2];	// 보안키만료잔여일 (DUKPT Key Expiration Remaining Days)
    byte	RFU					[50];	//
    byte	cCR					[ 1];	// carriage return : 응답전문의 끝
} __attribute__ ((packed)) kvrIC_TRANS_t;	// total 1320-Byte

////////////////////////////////////////////////////////////////////////////////////////////////////
// IFM packet
typedef struct REQ_IC_CREDIT_APPROVAL {
    byte    stx             [ 1];
    word	len;
    byte	cmd				[ 1];	// request command
    byte	transType		[ 2];	// kind of transaction
    byte	amount			[10];	// transaction amount
    byte	date			[14];	// transaction date
    byte	fSignPad		[ 1];	// 서명패드 사용여부
    byte	fCashSimp		[ 1];	// 현금IC 간소화
    byte	cntReceipt		[ 4];	// 전표출력 카운터
    byte	signDataChange	[ 1];	// 서명데이터 치환방식
    byte	entryMin		[ 2];	// entry min
    byte	entryMax		[ 2];	// entry max
    byte	indexWKey		[ 2];	// working key index
    byte	workingKey		[16];	// working key
    byte	randNum			[32];	// random number
    byte    etx             [ 1];
    byte    lrc             [ 1];
} __attribute__ ((packed)) _kiqIcCreditA_t;

typedef struct REQ_IC_CREDIT_APPROVAL_RESULT{
    byte    stx             [ 1];
    word	len;
    byte	cmd				[ 1];
    byte	date			[14];	//
    byte	addRespData		[26];	// additional response data
    byte	iAuthData		[12];	// issuer authenticaton data
    byte	iScript			[260];	// issuer script
    byte	approvResult	[ 2];	// 승인여부
    byte    etx             [ 1];
    byte    lrc             [ 1];
} __attribute__ ((packed)) _kiqIcCreditAR_t;    // 신용 승인결과 요청 to IFM

//////////////////////////////////////// Added by irene.choi - start
typedef struct REQ_IC_CANCEL_PAYMENT{
    byte    stx             [ 1];
    word	len;
    byte	cmd				[ 1];
    byte    etx             [ 1];
    byte    lrc             [ 1];
} __attribute__ ((packed)) _kiqIcCancelP_t;    // 결제 취소 요청 to IFM
//////////////////////////////////////// Added by irene.choi - stop

typedef struct REQ_MUTUAL_AUTH_RESULT {
    byte    stx             [ 1];
    word	len;
    byte	cmd             [ 1];
    byte	dateTime        [14];
    byte	tmnlCertNum     [32];
    byte	tmnlSerialNum   [10];
    byte	respCode        [ 4];
    byte	respMsg         [80];
    byte	connSecKey      [48];	// 접속 보안키
    byte	numOfData       [ 4];
    byte	transProtocol   [ 5];
    byte	downSrvIP       [80];
    byte	downSvrPort     [ 5];
    byte	accountID       [16];
    byte	accountPWD      [16];
    byte	fwVERorDataType [ 5];
    byte	fwVERorDataExpl [80];
    byte	fileName        [256];
    byte	fileSize        [10];
    byte	fileChkType     [ 5];
    byte	fileChkSum      [64];
    byte	fileDecKey      [32];
    byte    etx             [ 1];
    byte    lrc             [ 1];
} __attribute__ ((packed)) _kiqMutualAuthReault_t;

typedef struct PKT_FROM_DEMON {
	byte    transType       [ 2];
	byte    amount          [10];
	byte    fee             [ 9];
	byte    tax             [ 9];
	byte    inst            [ 2];
	byte    orgApprovalNo   [12];
	byte    orgApprovalDate [ 8];
} __attribute__ ((packed)) reqFromDemon_t;

typedef struct PKT_TO_DEMON {
	byte    transType       [ 2];       // 업무구분
	byte    amount          [10];       // 결제금액에서 거래금액(세금, 봉사료를 제외한 금액
	byte    fee             [ 9];       // 봉사료
	byte    surTax          [ 9];       // 세금
//	byte    totalAmount     [10];       // 결제금액(세금, 봉사료 포함)
	byte    instalment      [ 2];       // 할부개월
	byte    cardNo          [40];       // masked card number (BIN 6-Byte만 처리할 것)
	byte    swipeType       [ 1];       // Swipe여부
	byte    orgApprovalDate [ 8];       // 원승인일자
	byte    orgApprovalNo   [12];       // 원승인번호
	////////////////////////////////////////////////////////////////////////////////// 요청전문에서 추출
	byte    respCode        [ 4];       // 응답코드
	byte    respMsg         [32];       // 응답메시지
	byte    approvalDate    [14];       // 승인실시
	byte    approvalNo      [12];       // 승인번호
	byte    acquirerCode    [ 4];       // 매입사 코드
	byte    acquirerName    [ 8];       // 매입사명
	byte    transSeqNum     [10];       // 거래일련번호
	byte    issuerCode      [ 4];       // 발급사코드
	byte    issuerName      [12];       // 발급사명
    byte    cardType        [ 1];       // 카드구분
	////////////////////////////////////////////////////////////////////////////////// 응답전문에서 추출
	byte    terminalID      [10];       // 터미널ID
	byte    businessNo      [10];       // 사업자 번호
	byte    shopName        [40];       // 가맹점명
	byte    shopOwonerName  [20];       // 대표자명
	byte    shopAddress     [50];       // 가맹점주소
	byte    shopTelNo       [15];       // 가맹점 전화번호
	////////////////////////////////////////////////////////////////////////////////// 단말기 다운로드에서 추출
} __attribute__ ((packed)) receiptToDemon_t;


#endif //PMPOS_PKTDEF_H

/**< end of file */
