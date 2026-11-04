package com.shreefintech.paytouchconsumer.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R

/**
 * Bill Pay categories. [value] is the backend `category` (`/billpay/categories`, `utility-billers`, `/transactions`).
 * [defaultParamName] keys the single account field when a biller sends `input_params: []`; null = no default
 * (the biller can't be paid until the backend sends its params).
 */
enum class CategoryType(
    val value: String,
    @StringRes val titleRes: Int,
    @StringRes val fieldLabelRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val smsBodyRes: Int,
    @StringRes val receiptTitleRes: Int,
    val defaultParamName: String? = null,
    val isPlanBased: Boolean = false,
    val needsCircle: Boolean = false,
    val isMobileCategory: Boolean = false,
    val isVehicleCategory: Boolean = false
) {
    ELECTRICITY("electricity", R.string.titleElectricityBill, R.string.labelConsumerNumber, R.drawable.img_electricity, R.string.msgSmsBody, R.string.titleElectricityPaymentConfirmation, Constant.BILLPAY_PARAM_CONSUMER_NUMBER),
    GAS("gas", R.string.titleGasBill, R.string.labelConsumerNumber, R.drawable.img_gas, R.string.msgGasSmsBody, R.string.titleGasPaymentConfirmation, Constant.BILLPAY_PARAM_CONSUMER_NUMBER),
    PREPAID(
        "prepaid", R.string.titleMobilePrepaid, R.string.labelMobileNumber, R.drawable.img_prepaid, R.string.msgPrepaidSmsBody, R.string.titlePrepaidPaymentConfirmation, Constant.BILLPAY_PARAM_MOBILE_NUMBER,
        isPlanBased = true, needsCircle = true, isMobileCategory = true
    ),
    POSTPAID(
        "mobile_postpaid", R.string.titleMobilePostpaid, R.string.labelMobileNumber, R.drawable.img_postpaid, R.string.msgPostpaidSmsBody, R.string.titlePostpaidPaymentConfirmation, Constant.BILLPAY_PARAM_MOBILE_NUMBER,
        isMobileCategory = true
    ),
    DTH(
        "dth", R.string.categoryDth, R.string.labelMobileNumber, R.drawable.img_dth, R.string.msgDthSmsBody, R.string.titleDthPaymentConfirmation, Constant.BILLPAY_PARAM_MOBILE_NUMBER,
        isPlanBased = true, isMobileCategory = true
    ),
    FASTAG("fastag", R.string.titleFastagRecharge, R.string.labelVehicleNumber, R.drawable.img_fastag, R.string.msgFastagSmsBody, R.string.titleFastagPaymentConfirmation, isVehicleCategory = true),
    LOAN("loan_repayment", R.string.titleLoanRepayment, R.string.labelLoanAccountNumber, R.drawable.img_loan, R.string.msgLoanSmsBody, R.string.titleLoanPaymentConfirmation),
    MUNICIPAL_TAX("municipal_taxes", R.string.titleMunicipalTax, R.string.labelConsumerNumber, R.drawable.img_municipal_tax, R.string.msgMunicipalTaxSmsBody, R.string.titleMunicipalTaxPaymentConfirmation),
    UNKNOWN("", R.string.categoryElectricity, R.string.labelConsumerNumber, R.drawable.img_electricity, R.string.msgSmsBody, R.string.titleElectricityPaymentConfirmation);

    companion object {
        fun from(value: String?): CategoryType = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
