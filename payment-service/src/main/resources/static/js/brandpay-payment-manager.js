/**
 * Toss Brandpay 결제 수단 관리 핸들러
 * - 1단계: Brandpay 연결 (Access Token 발급)
 * - 2단계: 결제 수단 등록 (Method Key 저장)
 */

class BrandpayPaymentManager {
    constructor(clientKey, customerKey, userId) {
        this.clientKey = clientKey;
        this.customerKey = customerKey;
        this.userId = userId;
        this.tossPayments = null;
        this.brandpay = null;
        this.accessToken = null;
        this.redirectUrl = window.location.origin + "/brandpay/callback-auth";
        
        this.init();
    }

    /**
     * Toss Payments 초기화
     */
    init() {
        try {
            this.tossPayments = TossPayments(this.clientKey);
            this.brandpay = this.tossPayments.brandpay({
                customerKey: this.customerKey,
                redirectUrl: this.redirectUrl
            });
            console.log("BrandpayPaymentManager 초기화 완료");
        } catch (error) {
            console.error("Toss Payments 초기화 실패:", error);
        }
    }

    /**
     * 1단계: Brandpay 연결 핸들러
     * - "토스와 연결" 버튼 클릭 시 호출
     * - openSettings()에서 사용자가 토스 인증 후 콜백 처리
     */
    async handleConnectBrandpay() {
        console.log("🔗 Brandpay 연결 시작...");
        
        try {
            // Toss SDK의 openSettings() 호출
            // 사용자가 카드/계좌를 등록하면 redirectUrl로 콜백됨
            await this.brandpay.openSettings();
            console.log("✅ Brandpay 연결 완료");
            
        } catch (error) {
            if (error.code === "USER_CANCEL") {
                console.log("⚠️ 사용자가 연결을 취소했습니다");
                this.showNotification("연결을 취소했습니다", "info");
            } else {
                console.error("❌ Brandpay 연결 오류:", error);
                this.showNotification(`오류: ${error.message}`, "error");
            }
        }
    }

    /**
     * 콜백 핸들러 (서버에서 호출)
     * - 백엔드에서 Authorization Code를 Access Token으로 교환
     * - 결과를 paymentMethod에 저장
     * 
     * @param {string} code - Authorization Code (query parameter)
     */
    async handleBrandpayCallback(code) {
        console.log("📍 Brandpay 콜백 수신...");
        
        try {
            const response = await fetch('/api/v1/payment-methods/connect-brandpay', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: this.userId,
                    customerKey: this.customerKey,
                    code: code
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const result = await response.json();
            console.log("✅ Brandpay 연결 완료:", result);
            
            this.accessToken = result.data.accessToken;
            this.showNotification("✅ Toss와 연결되었습니다!", "success");
            
            return result.data;
            
        } catch (error) {
            console.error("❌ 콜백 처리 오류:", error);
            this.showNotification("연결 처리 중 오류가 발생했습니다", "error");
            throw error;
        }
    }

    /**
     * 2단계: 결제 수단 추가 핸들러
     * - "결제 수단 추가" 버튼 클릭 시 호출
     * - SDK의 addPaymentMethod()를 사용해 카드/계좌 등록 창 오픈
     */
    async handleAddPaymentMethod() {
        console.log("➕ 결제 수단 추가 시작...");
        
        if (!this.accessToken) {
            console.error("❌ Access Token이 없습니다. 먼저 Brandpay와 연결해주세요");
            this.showNotification("먼저 Toss와 연결해야 합니다", "warning");
            return;
        }

        try {
            // SDK의 addPaymentMethod() 호출
            // 사용자가 카드/계좌를 등록하면 methodKey 반환
            const methodResponse = await this.brandpay.addPaymentMethod({
                method: 'CARD' // 또는 'ACCOUNT'
            });

            console.log("📋 methodKey 획득:", methodResponse);
            
            // 3. 백엔드에 methodKey 저장 (registerPaymentMethod)
            await this.registerPaymentMethod(methodResponse);
            
        } catch (error) {
            if (error.code === "USER_CANCEL") {
                console.log("⚠️ 사용자가 결제 수단 추가를 취소했습니다");
                this.showNotification("결제 수단 추가를 취소했습니다", "info");
            } else {
                console.error("❌ 결제 수단 추가 오류:", error);
                this.showNotification(`오류: ${error.message}`, "error");
            }
        }
    }

    /**
     * 결제 수단 등록 (Method Key 저장)
     * @param {object} methodResponse - SDK에서 반환받은 결제 수단 정보
     * 
     * methodResponse 예:
     * {
     *   methodKey: "method_abcd1234...",
     *   displayName: "신한 카드",
     *   maskedNumber: "1234-****-****-5678",
     *   type: "CARD"  // 또는 "ACCOUNT"
     * }
     */
    async registerPaymentMethod(methodResponse) {
        console.log("💾 결제 수단 등록 중...", methodResponse);
        
        try {
            const response = await fetch('/api/v1/payment-methods/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: this.userId,
                    methodKey: methodResponse.methodKey,
                    type: methodResponse.type === 'ACCOUNT' ? 'BILLING' : 'NORMAL',
                    displayName: methodResponse.displayName,
                    maskedNumber: methodResponse.maskedNumber
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const result = await response.json();
            console.log("✅ 결제 수단 등록 완료:", result);
            
            this.showNotification("✅ 결제 수단이 등록되었습니다!", "success");
            
            // UI 업데이트 (결제 수단 목록 새로고침 등)
            this.onPaymentMethodRegistered(result.data);
            
            return result.data;
            
        } catch (error) {
            console.error("❌ 결제 수단 등록 오류:", error);
            this.showNotification("결제 수단 등록 중 오류가 발생했습니다", "error");
            throw error;
        }
    }

    /**
     * 결제 수단 등록 완료 후 콜백
     * @param {object} paymentMethod - 등록된 결제 수단 정보
     */
    onPaymentMethodRegistered(paymentMethod) {
        console.log("🎉 결제 수단 등록 완료:", paymentMethod);
        
        // 예: 결제 수단 목록 새로고침
        // this.refreshPaymentMethodList();
        
        // 예: 부모 컴포넌트에 이벤트 발송
        // window.dispatchEvent(new CustomEvent('paymentMethodRegistered', { detail: paymentMethod }));
    }

    /**
     * 알림 표시
     */
    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        
        // 예: Toast 또는 Alert 라이브러리 사용
        // toast.show({ message, type });
        
        // 간단한 alert 사용 (프로덕션에서는 토스트 라이브러리 권장)
        if (type === 'error') {
            alert(`❌ ${message}`);
        } else if (type === 'success') {
            alert(`✅ ${message}`);
        }
    }

    /**
     * 결제 수단 목록 조회 (선택사항)
     */
    async getPaymentMethods() {
        try {
            const response = await fetch(`/api/v1/payment-methods/${this.userId}`);
            const result = await response.json();
            console.log("결제 수단 목록:", result.data);
            return result.data;
        } catch (error) {
            console.error("결제 수단 목록 조회 오류:", error);
            return [];
        }
    }
}

// Export for use in HTML
if (typeof module !== 'undefined' && module.exports) {
    module.exports = BrandpayPaymentManager;
}
