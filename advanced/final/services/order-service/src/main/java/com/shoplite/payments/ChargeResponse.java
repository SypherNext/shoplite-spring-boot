package com.shoplite.payments;

/** The JSON body the payment provider answers with. Status is APPROVED or DECLINED. */
public record ChargeResponse(String paymentId, String status) {

    public boolean approved() {
        return "APPROVED".equals(status);
    }

}
