package device.apps.pmpos.packet;

import java.util.Arrays;

import device.apps.pmpos.Utils;

public class VanApprovalCancelData {
    // Common header data
    private byte[] transType;
    private byte[] transDate;
    private byte[] transSeqNo;
    private byte[] mctData;

    // IC reader packet
    private byte[] maskedTrack2;
    private byte[] ksn;
    private byte[] encTrack2;

    // Input data
    private byte[] fSwipe;
    private byte[] inst;
    private byte[] transAmt;
    private byte[] feeAmt;
    private byte[] surtax;
    private byte[] approvalNo;
    private byte[] approvalDate;
    private byte[] fTmnlCancel;
    private byte[] fElectSign;
    private byte[] fES_length;
    private byte[] fES_Data;
    private byte[] fallbackCode;
    private byte[] isIndividual;
    private byte[] reasonOfCancelation;

    public VanApprovalCancelData() {
        transType = new byte[2];
        transDate = new byte[14];
        transSeqNo = new byte[10];
        mctData = new byte[20];

        maskedTrack2 = new byte[40];
        ksn = new byte[10];
        encTrack2 = new byte[48];

        fSwipe = new byte[1];
        inst = new byte[2];
        transAmt = new byte[12];
        feeAmt = new byte[9];
        surtax = new byte[9];
        approvalNo = new byte[12];
        approvalDate = new byte[8];
        fTmnlCancel = new byte[1];
        fElectSign = new byte[1];
        fES_length = new byte[4];
        fES_Data = new byte[1086];
        fallbackCode = new byte[2];
        isIndividual = new byte[1];
        reasonOfCancelation = new byte[1];

        initializeFields();
    }

    private void initializeFields() {
        Arrays.fill(transType, (byte)0x20);
        Arrays.fill(transDate, (byte)0x20);
        Arrays.fill(transSeqNo, (byte)0x30);
        Arrays.fill(mctData, (byte)0x20);

        Arrays.fill(maskedTrack2, (byte)0x20);
        Arrays.fill(ksn, (byte)0x20);
        Arrays.fill(encTrack2, (byte)0x20);

        Arrays.fill(fSwipe, (byte)0x20);
        Arrays.fill(inst, (byte)0x20);
        Arrays.fill(transAmt, (byte)0x30);
        Arrays.fill(feeAmt, (byte)0x30);
        Arrays.fill(surtax, (byte)0x30);
        Arrays.fill(approvalNo, (byte)0x20);
        Arrays.fill(approvalDate, (byte)0x20);
        Arrays.fill(fTmnlCancel, (byte)0x20);
        Arrays.fill(fElectSign, (byte)0x20);
        Arrays.fill(fES_length, (byte)0x20);
        Arrays.fill(fES_Data, (byte)0x20);
        Arrays.fill(fallbackCode, (byte)0x20);
        Arrays.fill(isIndividual, (byte)0x20);
        Arrays.fill(reasonOfCancelation, (byte)0x20);
    }

    private int getDataPktLength() {
        int length = transType.length + transDate.length + transSeqNo.length + mctData.length
                + maskedTrack2.length + ksn.length + encTrack2.length
                + fSwipe.length + inst.length + transAmt.length + feeAmt.length
                + surtax.length + approvalNo.length + approvalDate.length + fTmnlCancel.length
                + fElectSign.length + fES_length.length + fES_Data.length + fallbackCode.length
                + isIndividual.length + reasonOfCancelation.length;
        return length;
    }

    public byte[] getDataPktBuf() {
        byte[] dataPktBuf = new byte[getDataPktLength()];
        int startIndex = 0;

        System.arraycopy(transType, 0, dataPktBuf, startIndex, transType.length);
        startIndex += transType.length;

        System.arraycopy(transDate, 0, dataPktBuf, startIndex, transDate.length);
        startIndex += transDate.length;

        System.arraycopy(transSeqNo, 0, dataPktBuf, startIndex, transSeqNo.length);
        startIndex += transSeqNo.length;

        System.arraycopy(mctData, 0, dataPktBuf, startIndex, mctData.length);
        startIndex += mctData.length;

        System.arraycopy(maskedTrack2, 0, dataPktBuf, startIndex, maskedTrack2.length);
        startIndex += maskedTrack2.length;

        System.arraycopy(ksn, 0, dataPktBuf, startIndex, ksn.length);
        startIndex += ksn.length;

        System.arraycopy(encTrack2, 0, dataPktBuf, startIndex, encTrack2.length);
        startIndex += encTrack2.length;

        System.arraycopy(fSwipe, 0, dataPktBuf, startIndex, fSwipe.length);
        startIndex += fSwipe.length;

        System.arraycopy(inst, 0, dataPktBuf, startIndex, inst.length);
        startIndex += inst.length;

        System.arraycopy(transAmt, 0, dataPktBuf, startIndex, transAmt.length);
        startIndex += transAmt.length;

        System.arraycopy(feeAmt, 0, dataPktBuf, startIndex, feeAmt.length);
        startIndex += feeAmt.length;

        System.arraycopy(surtax, 0, dataPktBuf, startIndex, surtax.length);
        startIndex += surtax.length;

        System.arraycopy(approvalNo, 0, dataPktBuf, startIndex, approvalNo.length);
        startIndex += approvalNo.length;

        System.arraycopy(approvalDate, 0, dataPktBuf, startIndex, approvalDate.length);
        startIndex += approvalDate.length;

        System.arraycopy(fTmnlCancel, 0, dataPktBuf, startIndex, fTmnlCancel.length);
        startIndex += fTmnlCancel.length;

        System.arraycopy(fElectSign, 0, dataPktBuf, startIndex, fElectSign.length);
        startIndex += fElectSign.length;

        System.arraycopy(fES_length, 0, dataPktBuf, startIndex, fES_length.length);
        startIndex += fES_length.length;

        System.arraycopy(fES_Data, 0, dataPktBuf, startIndex, fES_Data.length);
        startIndex += fES_Data.length;

        System.arraycopy(fallbackCode, 0, dataPktBuf, startIndex, fallbackCode.length);
        startIndex += fallbackCode.length;

        System.arraycopy(isIndividual, 0, dataPktBuf, startIndex, isIndividual.length);
        startIndex += isIndividual.length;

        System.arraycopy(reasonOfCancelation, 0, dataPktBuf, startIndex, reasonOfCancelation.length);

        return dataPktBuf;
    }

    public byte[] getTransType() {
        return transType;
    }

    public void setTransType(byte[] transType) {
        System.arraycopy(transType, 0, this.transType, 0, transType.length);
    }

    public byte[] getTransDate() {
        return transDate;
    }

    public void setTransDate(byte[] transDate) {
        System.arraycopy(transDate, 0, this.transDate, 0, transDate.length);
    }

    public byte[] getTransSeqNo() {
        return transSeqNo;
    }

    public void setTransSeqNo(byte[] transSeqNo) {
        System.arraycopy(transSeqNo, 0, this.transSeqNo, 0, transSeqNo.length);
    }

    public byte[] getMctData() {
        return mctData;
    }

    public void setMctData(byte[] mctData) {
        System.arraycopy(mctData, 0, this.mctData, 0, mctData.length);
    }

    public byte[] getMaskedTrack2() {
        return maskedTrack2;
    }

    public void setMaskedTrack2(byte[] maskedTrack2) {
        System.arraycopy(maskedTrack2, 0, this.maskedTrack2, 0, maskedTrack2.length);
    }

    public byte[] getKsn() {
        return ksn;
    }

    public void setKsn(byte[] ksn) {
        System.arraycopy(ksn, 0, this.ksn, 0, ksn.length);
    }

    public byte[] getEncTrack2() {
        return encTrack2;
    }

    public void setEncTrack2(byte[] encTrack2) {
        System.arraycopy(encTrack2, 0, this.encTrack2, 0, encTrack2.length);
    }

    public byte[] getfSwipe() {
        return fSwipe;
    }

    public void setfSwipe(byte[] fSwipe) {
        System.arraycopy(fSwipe, 0, this.fSwipe, 0, fSwipe.length);
    }

    public byte[] getInst() {
        return inst;
    }

    public void setInst(byte[] inst) {
        System.arraycopy(inst, 0, this.inst, 0, inst.length);
    }

    public byte[] getTransAmt() {
        return transAmt;
    }

    public void setTransAmt(byte[] transAmt) {
        System.arraycopy(transAmt, 0, this.transAmt, 0, transAmt.length);
    }

    public byte[] getFeeAmt() {
        return feeAmt;
    }

    public void setFeeAmt(byte[] feeAmt) {
        System.arraycopy(feeAmt, 0, this.feeAmt, 0, feeAmt.length);
    }

    public byte[] getSurtax() {
        return surtax;
    }

    public void setSurtax(byte[] surtax) {
        System.arraycopy(surtax, 0, this.surtax, 0, surtax.length);
    }

    public byte[] getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(byte[] approvalNo) {
        System.arraycopy(approvalNo, 0, this.approvalNo, 0, approvalNo.length);
    }

    public byte[] getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(byte[] approvalDate) {
        System.arraycopy(approvalDate, 0, this.approvalDate, 0, approvalDate.length);
    }

    public byte[] getfTmnlCancel() {
        return fTmnlCancel;
    }

    public void setfTmnlCancel(byte[] fTmnlCancel) {
        System.arraycopy(fTmnlCancel, 0, this.fTmnlCancel, 0, fTmnlCancel.length);
    }

    public byte[] getfElectSign() {
        return fElectSign;
    }

    public void setfElectSign(byte[] fElectSign) {
        System.arraycopy(fElectSign, 0, this.fElectSign, 0, fElectSign.length);
    }

    public byte[] getfES_length() {
        return fES_length;
    }

    public void setfES_length(byte[] fES_length) {
        System.arraycopy(fES_length, 0, this.fES_length, 0, fES_length.length);
    }

    public byte[] getfES_Data() {
        return fES_Data;
    }

    public void setfES_Data(byte[] fES_Data) {
        System.arraycopy(fES_Data, 0, this.fES_Data, 0, fES_Data.length);
    }

    public byte[] getFallbackCode() {
        return fallbackCode;
    }

    public void setFallbackCode(byte[] fallbackCode) {
        System.arraycopy(fallbackCode, 0, this.fallbackCode, 0, fallbackCode.length);
    }

    public byte[] getIsIndividual() {
        return isIndividual;
    }

    public void setIsIndividual(byte[] isIndividual) {
        System.arraycopy(isIndividual, 0, this.isIndividual, 0, isIndividual.length);
    }

    public byte[] getReasonOfCancelation() {
        return reasonOfCancelation;
    }

    public void setReasonOfCancelation(byte[] reasonOfCancelation) {
        System.arraycopy(reasonOfCancelation, 0, this.reasonOfCancelation, 0, reasonOfCancelation.length);
    }
}
