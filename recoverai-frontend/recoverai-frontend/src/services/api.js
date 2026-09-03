import axios from "axios";

const baseURL = (import.meta.env.VITE_API_URL || "http://localhost:8080")
    .replace(/\/$/, "");

const api = axios.create({
    baseURL,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
    },
});

/**
 * Keep RecoverAI's HTTP contract in one place.
 * Components can focus on the recovery experience instead of constructing
 * URLs and request payloads.
 */
export const recoverAiApi = {
    // Payments
    getPayments: (config) =>
        api.get("/api/payments", config),

    getPaymentDetails: (paymentId) =>
        api.get(`/api/payments/${paymentId}/details`),

    // Dashboard
    getDashboardStats: (config) =>
        api.get("/api/dashboard/stats", config),

    getRecentActivity: (config) =>
        api.get("/api/dashboard/recent-activity", config),

    getDashboardSummary: (config) =>
        api.get("/api/dashboard/summary", config),

    getDashboardAnalytics: (config) =>
        api.get("/api/dashboard/analytics", config),

    getRecoveryTrends: (config) =>
        api.get("/api/dashboard/recovery-trends", config),

    getFailureReasons: (config) =>
        api.get("/api/dashboard/failure-reasons", config),

    getRecoveryComparison: (config) =>
        api.get("/api/dashboard/recovery-comparison", config),

    // Recovery
    getRecoveryAttempts: (paymentId, config) =>
        api.get(`/api/recovery/payment/${paymentId}`, config),

    getRecoveryAttemptPage: (paymentId, config) =>
        api.get(`/api/recovery-attempts/payment/${paymentId}`, config),

    getRecoveryOperations: (config) =>
        api.get("/api/recovery-attempts", config),

    retryLater: (recoveryAttemptId, scheduledRetryAt) =>
        api.post(
            `/api/recovery/attempts/${recoveryAttemptId}/retry-later`,
            null,
            {
                params: { scheduledRetryAt },
            }
        ),

    promiseToPay: (recoveryAttemptId, promiseToPayDeadline) =>
        api.post(
            `/api/recovery/attempts/${recoveryAttemptId}/promise-to-pay`,
            {
                promiseToPayDeadline,
            }
        ),

    notifyCustomer: (recoveryAttemptId) =>
        api.post(
            `/api/recovery/attempts/${recoveryAttemptId}/notify`
        ),

    chooseAnotherPaymentMethod: (recoveryAttemptId) =>
        api.post(
            `/api/recovery/attempts/${recoveryAttemptId}/choose-payment-method`
        ),

    submitAlternativePaymentMethod: (recoveryAttemptId, payload) =>
        api.post(
            `/api/recovery/attempts/${recoveryAttemptId}/alternative-payment-method`,
            payload
        ),
};

export default api;