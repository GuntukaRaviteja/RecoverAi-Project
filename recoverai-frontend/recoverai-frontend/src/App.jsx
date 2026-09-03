import { useEffect, useState } from "react";
import { recoverAiApi } from "./services/api";
import "./App.css";

const isFutureDate = (dateTime) =>
    new Date(dateTime).getTime() > Date.now();

const matchesSearch = (values, query) => {
    const normalizedQuery = String(query || "").trim().toLowerCase();

    if (!normalizedQuery) {
        return true;
    }

    return values.some((value) =>
        String(value ?? "").trim().toLowerCase().includes(normalizedQuery)
    );
};



function App() {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [dashboardStats, setDashboardStats] = useState(null);
    const [dashboardAnalytics, setDashboardAnalytics] = useState(null);
    const [recoveryTrends, setRecoveryTrends] = useState([]);
    const [, setDashboardFailureReasons] = useState([]);
    const [recentActivity, setRecentActivity] = useState([]);
    const [recoveryComparison, setRecoveryComparison] = useState(null);

    const [activeView, setActiveView] = useState("dashboard");

    const [selectedPaymentId, setSelectedPaymentId] = useState("");
    const [recoveryAttempts, setRecoveryAttempts] = useState([]);
    const [recoveryOperations, setRecoveryOperations] = useState([]);
    const [operationFilters, setOperationFilters] = useState({
        failureCategory: "",
        recoveryMethod: "",
        attempts: "",
        status: "",
        customerAction: "",
    });
    const [paymentSearch, setPaymentSearch] = useState("");
    const [showAllRecoveryOperations, setShowAllRecoveryOperations] = useState(false);
    const [showAllPaymentSearchResults, setShowAllPaymentSearchResults] = useState(false);
    const [showAllDetailSearchResults, setShowAllDetailSearchResults] = useState(false);
    const [attemptsLoading, setAttemptsLoading] = useState(false);
    const [attemptsError, setAttemptsError] = useState("");
    const [totalAttempts, setTotalAttempts] = useState(0);

    const [selectedDetailsPaymentId, setSelectedDetailsPaymentId] =
        useState("");
    const [detailsPaymentSearch, setDetailsPaymentSearch] = useState("");
    const [paymentDetails, setPaymentDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState("");

    const [scheduledRetryAt, setScheduledRetryAt] = useState("");
    const [promiseToPayDeadline, setPromiseToPayDeadline] = useState("");
    const [alternativePaymentMethod, setAlternativePaymentMethod] =
        useState("UPI");
    const [
        alternativePaymentMethodReference,
        setAlternativePaymentMethodReference,
    ] = useState("");
    const [recoveryActionLoading, setRecoveryActionLoading] =
        useState(false);
    const [recoveryActionError, setRecoveryActionError] = useState("");
    const [recoveryActionMessage, setRecoveryActionMessage] =
        useState("");

    const loadPayments = async () => {
        try {
            setLoading(true);
            setError("");

            const response = await recoverAiApi.getPayments();
            setPayments(response.data);
        } catch (err) {
            console.error("Failed to load payments:", err);

            setError(
                "Unable to connect to the RecoverAI backend. Please make sure the Spring Boot application is running on port 8080."
            );
        } finally {
            setLoading(false);
        }
    };

    const loadRecoveryOperations = async (config = {}) => {
        try {
            const response = await recoverAiApi.getRecoveryOperations(config);
            const operations = Array.isArray(response.data)
                ? response.data
                : response.data?.content ||
                  response.data?.operations ||
                  [];

            setRecoveryOperations(
                Array.isArray(operations) ? operations : []
            );
        } catch (err) {
            if (err.code !== "ERR_CANCELED") {
                console.error("Failed to load recovery operations:", err);
            }
        }
    };

    const loadDashboardExtras = async (config = {}) => {
        const [
            statsResult,
            activityResult,
            analyticsResult,
            trendsResult,
            failureReasonsResult,
            comparisonResult,
        ] = await Promise.allSettled([
            recoverAiApi.getDashboardStats(config),
            recoverAiApi.getRecentActivity(config),
            recoverAiApi.getDashboardAnalytics(config),
            recoverAiApi.getRecoveryTrends(config),
            recoverAiApi.getFailureReasons(config),
            recoverAiApi.getRecoveryComparison(config),
        ]);

        if (statsResult.status === "fulfilled") {
            setDashboardStats(statsResult.value.data);
        }

        if (activityResult.status === "fulfilled") {
            setRecentActivity(activityResult.value.data.activities || []);
        }

        if (analyticsResult.status === "fulfilled") {
            setDashboardAnalytics(analyticsResult.value.data);
        }

        if (trendsResult.status === "fulfilled") {
            setRecoveryTrends(
                Array.isArray(trendsResult.value.data)
                    ? trendsResult.value.data
                    : []
            );
        }

        if (failureReasonsResult.status === "fulfilled") {
            setDashboardFailureReasons(
                Array.isArray(failureReasonsResult.value.data)
                    ? failureReasonsResult.value.data
                    : []
            );
        }

        if (comparisonResult.status === "fulfilled") {
            setRecoveryComparison(comparisonResult.value.data);
        }
    };

    useEffect(() => {
        const controller = new AbortController();

        recoverAiApi.getPayments({ signal: controller.signal })
            .then((response) => {
                setPayments(response.data);
            })
            .catch((err) => {
                if (err.code === "ERR_CANCELED") {
                    return;
                }

                console.error("Failed to load payments:", err);
                setError(
                    "Unable to connect to the RecoverAI backend. Please make sure the Spring Boot application is running on port 8080."
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        Promise.resolve()
            .then(() => loadRecoveryOperations({ signal: controller.signal }))
            .catch((err) => {
                if (err.code !== "ERR_CANCELED") {
                    console.error("Failed to load recovery operations:", err);
                }
            });

        Promise.resolve()
            .then(() => loadDashboardExtras({ signal: controller.signal }))
            .catch((err) => {
                if (err.code !== "ERR_CANCELED") {
                    console.error("Failed to load dashboard data:", err);
                }
            });

        return () => controller.abort();
    }, []);

    const loadRecoveryAttempts = async (paymentId) => {
        if (!paymentId) {
            setRecoveryAttempts([]);
            setTotalAttempts(0);
            return;
        }

        try {
            setAttemptsLoading(true);
            setAttemptsError("");

            const response = await recoverAiApi.getRecoveryAttempts(paymentId);
            const attempts = Array.isArray(response.data)
                ? response.data
                : response.data.content || [];

            setRecoveryAttempts(attempts);
            setTotalAttempts(
                Array.isArray(response.data)
                    ? attempts.length
                    : response.data.totalElements ?? attempts.length
            );
        } catch (err) {
            console.error("Failed to load recovery attempts:", err);

            setAttemptsError(
                "Unable to load recovery attempts for the selected payment."
            );

            setRecoveryAttempts([]);
            setTotalAttempts(0);
        } finally {
            setAttemptsLoading(false);
        }
    };

    const loadPaymentDetails = async (paymentId) => {
        if (!paymentId) {
            setPaymentDetails(null);
            return;
        }

        try {
            setDetailsLoading(true);
            setDetailsError("");
            setPaymentDetails(null);

            const response = await recoverAiApi.getPaymentDetails(paymentId);

            setPaymentDetails(response.data);
        } catch (err) {
            console.error("Failed to load payment details:", err);

            setDetailsError(
                "Unable to load complete details for the selected payment."
            );
        } finally {
            setDetailsLoading(false);
        }
    };

    const openPaymentDetails = (paymentId) => {
        const id = String(paymentId);

        setActiveView("details");
        setSelectedPaymentId(id);
        setSelectedDetailsPaymentId(id);
        setRecoveryActionError("");
        setRecoveryActionMessage("");
        setScheduledRetryAt("");
        setPromiseToPayDeadline("");
        setAlternativePaymentMethodReference("");
        loadPaymentDetails(id);
    };

    const getApiErrorMessage = (
        err,
        fallbackMessage
    ) => {
        return (
            err.response?.data?.message ||
            err.message ||
            fallbackMessage
        );
    };

    const refreshSelectedPaymentData = async () => {
        const paymentId = selectedDetailsPaymentId;

        await loadPayments();
        await loadRecoveryOperations();
        await loadDashboardExtras();

        if (paymentId) {
            await loadPaymentDetails(paymentId);
        }

        if (
            selectedPaymentId &&
            String(selectedPaymentId) === String(paymentId)
        ) {
            await loadRecoveryAttempts(selectedPaymentId);
        }
    };

    const handleRetryLater = async (recoveryAttemptId) => {
        if (!scheduledRetryAt) {
            setRecoveryActionError(
                "Please choose a future date and time for the retry."
            );
            return;
        }

        const selectedDate = new Date(scheduledRetryAt);

        if (
            Number.isNaN(selectedDate.getTime()) ||
            !isFutureDate(scheduledRetryAt)
        ) {
            setRecoveryActionError(
                "Please choose a retry time in the future."
            );
            return;
        }

        try {
            setRecoveryActionLoading(true);
            setRecoveryActionError("");
            setRecoveryActionMessage("");

            await recoverAiApi.retryLater(recoveryAttemptId, scheduledRetryAt);

            setRecoveryActionMessage(
                "Your retry has been scheduled successfully."
            );
            await refreshSelectedPaymentData();
        } catch (err) {
            console.error("Failed to schedule retry:", err);
            setRecoveryActionError(
                getApiErrorMessage(
                    err,
                    "Unable to schedule the recovery retry."
                )
            );
        } finally {
            setRecoveryActionLoading(false);
        }
    };

    const handlePromiseToPay = async (recoveryAttemptId) => {
        if (!promiseToPayDeadline || !isFutureDate(promiseToPayDeadline)) {
            setRecoveryActionError(
                "Please choose a future promise-to-pay deadline."
            );
            return;
        }

        try {
            setRecoveryActionLoading(true);
            setRecoveryActionError("");
            setRecoveryActionMessage("");

            await recoverAiApi.promiseToPay(
                recoveryAttemptId,
                promiseToPayDeadline
            );

            setRecoveryActionMessage(
                "Promise to pay recorded. RecoverAI will respect this deadline."
            );
            await refreshSelectedPaymentData();
        } catch (err) {
            console.error("Failed to record promise to pay:", err);
            setRecoveryActionError(
                getApiErrorMessage(err, "Unable to record the promise-to-pay deadline.")
            );
        } finally {
            setRecoveryActionLoading(false);
        }
    };

    const handleNotifyCustomer = async (recoveryAttemptId) => {
        try {
            setRecoveryActionLoading(true);
            setRecoveryActionError("");
            setRecoveryActionMessage("");

            await recoverAiApi.notifyCustomer(recoveryAttemptId);
            setRecoveryActionMessage(
                "Recovery messages were queued for email, WhatsApp, and phone outreach."
            );
            await refreshSelectedPaymentData();
        } catch (err) {
            console.error("Failed to queue customer outreach:", err);
            setRecoveryActionError(
                getApiErrorMessage(err, "Unable to queue customer outreach.")
            );
        } finally {
            setRecoveryActionLoading(false);
        }
    };

    const handleChooseAnotherPaymentMethod = async (
        recoveryAttemptId
    ) => {
        try {
            setRecoveryActionLoading(true);
            setRecoveryActionError("");
            setRecoveryActionMessage("");

            await recoverAiApi.chooseAnotherPaymentMethod(recoveryAttemptId);

            setRecoveryActionMessage(
                "Please provide the alternative payment method below."
            );
            await refreshSelectedPaymentData();
        } catch (err) {
            console.error(
                "Failed to select another payment method:",
                err
            );

            setRecoveryActionError(
                getApiErrorMessage(
                    err,
                    "Unable to update the recovery option."
                )
            );
        } finally {
            setRecoveryActionLoading(false);
        }
    };

    const handleAlternativePaymentMethod = async (
        recoveryAttemptId
    ) => {
        if (!alternativePaymentMethodReference.trim()) {
            setRecoveryActionError(
                "Please enter the reference for the alternative payment method."
            );
            return;
        }

        try {
            setRecoveryActionLoading(true);
            setRecoveryActionError("");
            setRecoveryActionMessage("");

            await recoverAiApi.submitAlternativePaymentMethod(recoveryAttemptId, {
                paymentMethod: alternativePaymentMethod,
                paymentMethodReference: alternativePaymentMethodReference.trim(),
            });

            setAlternativePaymentMethodReference("");
            setRecoveryActionMessage(
                "The alternative payment method was submitted for recovery."
            );
            await refreshSelectedPaymentData();
        } catch (err) {
            console.error(
                "Failed to submit alternative payment method:",
                err
            );

            setRecoveryActionError(
                getApiErrorMessage(
                    err,
                    "Unable to process the alternative payment method."
                )
            );
        } finally {
            setRecoveryActionLoading(false);
        }
    };

    const totalPayments = payments.length;

    const recoveredPayments = payments.filter(
        (payment) => payment.status === "RECOVERED"
    ).length;

    const failedPayments = payments.filter(
        (payment) => payment.status === "FAILED"
    ).length;

    const successfulPayments = payments.filter(
        (payment) => payment.status === "SUCCESS"
    ).length;

    // Recovery Rate comes from the backend Dashboard Stats API.
    // It measures successful recovery attempts against completed recovery attempts.
    const recoveryRate =
        dashboardStats?.recoverySuccessRate != null
            ? Number(dashboardStats.recoverySuccessRate).toFixed(1)
            : "0.0";

    const successfulRecoveryAttempts =
        dashboardStats?.successfulRecoveryAttempts ?? 0;

    const failedRecoveryAttempts =
        dashboardStats?.failedRecoveryAttempts ??
        dashboardAnalytics?.failedRecoveryAttempts ??
        0;

    const analyticsPendingRecoveryAttempts =
        dashboardAnalytics?.pendingRecoveryAttempts ??
        dashboardStats?.pendingRecoveryAttempts ??
        0;

    const completedRecoveryAttempts =
        successfulRecoveryAttempts + failedRecoveryAttempts;

    const recoveredAmount = payments
        .filter((payment) => payment.status === "RECOVERED")
        .reduce(
            (total, payment) =>
                total + Number(payment.amount || 0),
            0
        );

    const failedAmount = payments
        .filter((payment) => payment.status === "FAILED")
        .reduce(
            (total, payment) =>
                total + Number(payment.amount || 0),
            0
        );

    const activeRecoveryCases =
        dashboardStats?.activeRecoveryCases ?? failedPayments;

    const recoveryFunnel = [
        {
            label: "Payments failed",
            count: failedPayments,
            description: "Revenue detected at risk",
            tone: "risk",
        },
        {
            label: "AI analyzed",
            count: dashboardStats?.totalAiDecisions ?? 0,
            description: "Best next action selected",
            tone: "analysis",
        },
        {
            label: "Recovery in motion",
            count: activeRecoveryCases,
            description: "Customer or system action pending",
            tone: "active",
        },
        {
            label: "Revenue recovered",
            count: recoveredPayments,
            description: "Payments successfully saved",
            tone: "success",
        },
    ];

    const formatActivityAction = (activity) =>
        String(activity.action || "RECOVERY_UPDATE")
            .replaceAll("_", " ")
            .toLowerCase()
            .replace(/\b\w/g, (letter) => letter.toUpperCase());

    const recentFailedPayments = payments
        .filter((payment) => payment.status === "FAILED")
        .sort((a, b) => {
            const dateA = a.createdAt
                ? new Date(a.createdAt).getTime()
                : 0;

            const dateB = b.createdAt
                ? new Date(b.createdAt).getTime()
                : 0;

            return dateB - dateA;
        })
        .slice(0, 5);

    const statusBreakdown = [
        {
            label: "Successful",
            count: successfulPayments,
            status: "SUCCESS",
        },
        {
            label: "Recovered",
            count: recoveredPayments,
            status: "RECOVERED",
        },
        {
            label: "Failed",
            count: failedPayments,
            status: "FAILED",
        },
    ];

    const analyticsStatusBreakdown = [
        {
            label: "Total payments",
            count: totalPayments,
            status: "TOTAL",
        },
        ...statusBreakdown.filter((item) => item.status !== "SUCCESS"),
    ];

    const failureCategoryMap = payments
        .filter((payment) => payment.status === "FAILED")
        .reduce((categories, payment) => {
            const category =
                payment.failureCategory || "UNKNOWN";

            categories[category] =
                (categories[category] || 0) + 1;

            return categories;
        }, {});

    const knownFailureCategories = [
        "INSUFFICIENT_FUNDS",
        "EXPIRED_CARD",
        "INVALID_CARD",
        "AUTHENTICATION_FAILED",
        "NETWORK_TECHNICAL_FAILURE",
        "SUSPECTED_FRAUD",
        "PAYMENT_TIMEOUT",
        "LIMIT_EXCEEDED",
        "BANK_ISSUER_UNAVAILABLE",
        "CARD_DECLINED",
    ];

    const failureCategories = [...new Set([
        ...knownFailureCategories,
        ...Object.keys(failureCategoryMap),
    ])]
        .map((category) => ({
            category,
            count: failureCategoryMap[category] || 0,
        }))
        .sort((a, b) => b.count - a.count);

    const failureImpactMap = payments
        .filter((payment) => payment.status === "FAILED")
        .reduce((impact, payment) => {
            const category = payment.failureCategory || "UNKNOWN";
            const current = impact[category] || { category, count: 0, amount: 0 };
            current.count += 1;
            current.amount += Number(payment.amount) || 0;
            impact[category] = current;
            return impact;
        }, {});

    const failureImpact = Object.values(failureImpactMap)
        .sort((a, b) => b.amount - a.amount);

    const recentRecoveryTrends = [...recoveryTrends]
        .sort((a, b) => String(a.date).localeCompare(String(b.date)))
        .slice(-3);

    const recoveryTrendMax = Math.max(
        ...recentRecoveryTrends.map((item) => Number(item.totalAttempts) || 0),
        1
    );

    const formatAmount = (amount) => {
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const formatDateTime = (dateTime) => {
        if (!dateTime) {
            return "Not available";
        }

        return new Date(dateTime).toLocaleString();
    };

    const formatConfidence = (confidenceScore) => {
        if (
            confidenceScore === null ||
            confidenceScore === undefined
        ) {
            return "Not available";
        }

        const score = Number(confidenceScore);

        return score <= 1
            ? `${(score * 100).toFixed(1)}%`
            : `${score.toFixed(1)}%`;
    };

    const formatLabel = (value) =>
        String(value || "Not available")
            .replaceAll("_", " ")
            .toLowerCase()
            .replace(/\b\w/g, (letter) => letter.toUpperCase());

    const getAttemptTone = (status) => {
        const normalized = String(status || "").toUpperCase();
        if (["SUCCESS", "RECOVERED"].includes(normalized)) return "success";
        if (["FAILED", "STOPPED"].includes(normalized)) return "failed";
        return "active";
    };

    const renderDashboard = () => (
        <>
            <div className="page-heading">
                <div>
          <span className="eyebrow">
            RECOVERY OPERATIONS
          </span>

                    <h1>Recovery Dashboard</h1>

                    <p className="subtitle">
                        Monitor failed payments and AI-powered recovery
                        performance.
                    </p>

                </div>

                <div className="dashboard-live-indicator">
                    <span className="live-dot"></span>
                    Live data
                </div>
            </div>


            {loading && (
                <p className="status-message">
                    Loading payment data...
                </p>
            )}

            {error && (
                <p className="error-message">{error}</p>
            )}

            {!loading && !error && (
                <>
                    <div className="stats-grid">
                        <div className="stat-card stat-card-total">
                            <div className="stat-card-top">
                                <span className="stat-icon">₹</span>

                                <span className="stat-label">
                  Revenue at Risk
                </span>
                            </div>

                            <strong className="stat-value stat-amount-value">
                                {formatAmount(failedAmount)}
                            </strong>

                            <span className="stat-description">
                {failedPayments} failed payment{failedPayments !== 1 ? "s" : ""} need attention
              </span>
                        </div>

                        <div className="stat-card stat-card-recovered">
                            <div className="stat-card-top">
                                <span className="stat-icon">✓</span>

                                <span className="stat-label">
                  Revenue Recovered
                </span>
                            </div>

                            <strong className="stat-value stat-amount-value">
                                {formatAmount(recoveredAmount)}
                            </strong>

                            <span className="stat-description">
                {recoveredPayments} payment{recoveredPayments !== 1 ? "s" : ""} successfully recovered
              </span>
                        </div>

                        <div className="stat-card stat-card-failed">
                            <div className="stat-card-top">
                                <span className="stat-icon">↻</span>

                                <span className="stat-label">
                  Active Recovery Cases
                </span>
                            </div>

                            <strong className="stat-value">
                                {activeRecoveryCases}
                            </strong>

                            <span className="stat-description">
                AI and customer actions in progress
              </span>
                        </div>

                        <div className="stat-card stat-card-rate">
                            <div className="stat-card-top">
                                <span className="stat-icon">↗</span>

                                <span className="stat-label">
                  Recovery Rate
                </span>
                            </div>

                            <strong className="stat-value">
                                {recoveryRate}%
                            </strong>

                            <span className="stat-description">
                Successful recovery attempts / completed attempts
              </span>
                        </div>
                    </div>

                    <div className="dashboard-insights-grid">
                        <section className="dashboard-section recovery-performance-card">
                            <div className="section-header">
                                <div>
                  <span className="section-eyebrow">
                    PERFORMANCE
                  </span>

                                    <h2>Recovery Performance</h2>
                                </div>

                                <strong className="recovery-rate-highlight">
                                    {recoveryRate}%
                                </strong>
                            </div>

                            <p className="section-description">
                                The percentage of completed recovery attempts
                                that successfully recovered a payment.
                            </p>

                            <div className="progress-track">
                                <div
                                    className="progress-fill"
                                    style={{
                                        width: `${Math.min(
                                            Number(recoveryRate),
                                            100
                                        )}%`,
                                    }}
                                ></div>
                            </div>

                            <div className="performance-summary">
                                <div>
                                    <span>Successful</span>
                                    <strong>{successfulRecoveryAttempts}</strong>
                                </div>

                                <div>
                                    <span>Failed</span>
                                    <strong>{failedRecoveryAttempts}</strong>
                                </div>

                                <div>
                                    <span>Completed</span>
                                    <strong>{completedRecoveryAttempts}</strong>
                                </div>
                            </div>
                        </section>

                        <section className="dashboard-section payment-health-card">
                            <div>
                <span className="section-eyebrow">
                  PAYMENT HEALTH
                </span>

                                <h2>Status Distribution</h2>
                            </div>

                            <div className="status-distribution">
                                {statusBreakdown.map((item) => {
                                    const percentage =
                                        totalPayments > 0
                                            ? (item.count / totalPayments) * 100
                                            : 0;

                                    return (
                                        <div
                                            className="status-distribution-item"
                                            key={item.status}
                                        >
                                            <div className="status-distribution-header">
                                                <span>{item.label}</span>

                                                <strong>
                                                    {item.count}
                                                    <small>
                                                        {" "}
                                                        ({percentage.toFixed(0)}%)
                                                    </small>
                                                </strong>
                                            </div>

                                            <div className="status-bar-track">
                                                <div
                                                    className={`status-bar-fill status-bar-${item.status.toLowerCase()}`}
                                                    style={{
                                                        width: `${percentage}%`,
                                                    }}
                                                ></div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </section>
                    </div>


                    <section className="dashboard-section guardrails-card">
                        <div className="section-header">
                            <div>
                                <span className="section-eyebrow">AI SAFETY & CONTROL</span>
                                <h2>Recovery Guardrails</h2>
                                <p className="section-description compact-description">
                                    RecoverAI uses deterministic policy controls to keep AI-powered recovery safe, bounded, and customer-aware.
                                </p>
                            </div>

                            <div className="guardrails-status">
                                <span className="guardrails-status-dot"></span>
                                Policy Active
                            </div>
                        </div>

                        <div className="guardrails-grid">
                            <div className="guardrail-item">
                                <div className="guardrail-icon">01</div>
                                <div>
                                    <strong>Maximum 5 Attempts</strong>
                                    <span>
                                        Recovery automatically stops after five attempts for the same payment.
                                    </span>
                                </div>
                                <b>ENFORCED</b>
                            </div>

                            <div className="guardrail-item">
                                <div className="guardrail-icon">02</div>
                                <div>
                                    <strong>Duplicate Protection</strong>
                                    <span>
                                        RecoverAI prevents multiple active recovery attempts from running for the same payment.
                                    </span>
                                </div>
                                <b>ENFORCED</b>
                            </div>

                            <div className="guardrail-item">
                                <div className="guardrail-icon">03</div>
                                <div>
                                    <strong>Customer-First Recovery</strong>
                                    <span>
                                        Insufficient-funds cases require customer action instead of uncontrolled automatic retries.
                                    </span>
                                </div>
                                <b>ENFORCED</b>
                            </div>

                            <div className="guardrail-item">
                                <div className="guardrail-icon">04</div>
                                <div>
                                    <strong>Non-Recoverable Failures</strong>
                                    <span>
                                        Fraud, closed-account, and invalid-account failures are blocked from automatic recovery.
                                    </span>
                                </div>
                                <b>BLOCKED</b>
                            </div>

                            <div className="guardrail-item">
                                <div className="guardrail-icon">05</div>
                                <div>
                                    <strong>Customer Promise Protection</strong>
                                    <span>
                                        Promise-to-pay deadlines pause recovery until the agreed customer deadline.
                                    </span>
                                </div>
                                <b>PROTECTED</b>
                            </div>

                            <div className="guardrail-item">
                                <div className="guardrail-icon">06</div>
                                <div>
                                    <strong>Full Audit Trail</strong>
                                    <span>
                                        AI decisions, recovery actions, successes, failures, and policy events are recorded for traceability.
                                    </span>
                                </div>
                                <b>LOGGED</b>
                            </div>
                        </div>

                        <div className="guardrails-flow">
                            <span>Payment Failure</span>
                            <strong>→</strong>
                            <span>AI Decision</span>
                            <strong>→</strong>
                            <span>Policy Validation</span>
                            <strong>→</strong>
                            <span>Bounded Recovery</span>
                            <strong>→</strong>
                            <span>Audit Log</span>
                        </div>
                    </section>

                    <div className="dashboard-command-grid">
                        <section className="dashboard-section recovery-funnel-card">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">AUTONOMOUS WORKFLOW</span>
                                    <h2>Recovery Funnel</h2>
                                </div>
                                <span className="attempt-count">
                                    {totalPayments} payment{totalPayments !== 1 ? "s" : ""} monitored
                                </span>
                            </div>

                            <p className="section-description compact-description">
                                Follow every failed payment from risk detection to a recovered outcome.
                            </p>

                            <div className="recovery-funnel">
                                {recoveryFunnel.map((stage, index) => (
                                    <div className="funnel-stage" key={stage.label}>
                                        <div className={`funnel-marker funnel-${stage.tone}`}>
                                            {index + 1}
                                        </div>
                                        <div className="funnel-stage-copy">
                                            <span>{stage.label}</span>
                                            <strong>{stage.count}</strong>
                                            <small>{stage.description}</small>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </section>

                        <section className="dashboard-section live-timeline-card">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">LIVE RECOVERY TIMELINE</span>
                                    <h2>Autonomous Activity</h2>
                                </div>
                                <span className="dashboard-live-indicator compact-live-indicator">
                                    <span className="live-dot"></span> Live
                                </span>
                            </div>

                            {recentActivity.length === 0 ? (
                                <p className="empty-message">
                                    Recovery activity will appear here as RecoverAI analyzes and acts on payments.
                                </p>
                            ) : (
                                <div className="activity-timeline">
                                    {recentActivity.slice(0, 5).map((activity) => (
                                        <button
                                            className="activity-item"
                                            key={`${activity.type}-${activity.referenceId}`}
                                            type="button"
                                            onClick={() => openPaymentDetails(activity.paymentId)}
                                        >
                                            <span className={`activity-icon activity-${String(activity.type || "").toLowerCase()}`}>
                                                {activity.type === "AI_DECISION" ? "✦" : activity.type === "AUDIT_LOG" ? "•" : "↻"}
                                            </span>
                                            <span className="activity-copy">
                                                <strong>{formatActivityAction(activity)}</strong>
                                                <small>{activity.details || "Recovery status updated"}</small>
                                            </span>
                                            <time>{formatDateTime(activity.createdAt)}</time>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </section>
                    </div>

                    <section className="dashboard-section">
                        <div className="section-header">
                            <div>
                                <span className="section-eyebrow">RECOVERY TRENDS</span>
                                <h2>Recovery Activity by Day</h2>
                            </div>
                            <span className="attempt-count">
                                {recentRecoveryTrends.reduce(
                                    (total, item) => total + (Number(item.totalAttempts) || 0),
                                    0
                                )} total attempts
                            </span>
                        </div>

                        <p className="section-description compact-description">
                            Daily recovery attempts and completed outcomes from the backend recovery analytics API.
                        </p>

                        {recentRecoveryTrends.length === 0 ? (
                            <p className="empty-message">
                                Recovery trend data is not available yet.
                            </p>
                        ) : (
                            <div className="category-bar-list">
                                {recentRecoveryTrends.map((trend) => {
                                    const total = Number(trend.totalAttempts) || 0;
                                    const successful = Number(trend.successfulAttempts) || 0;
                                    const failed = Number(trend.failedAttempts) || 0;
                                    const width = Math.round((total / recoveryTrendMax) * 100);

                                    return (
                                        <div className="category-bar-item" key={trend.date}>
                                            <div className="category-bar-header">
                                                <span>{trend.date}</span>
                                                <strong>{total} attempts</strong>
                                            </div>
                                            <div className="category-bar-track">
                                                <div
                                                    className="category-bar-fill"
                                                    style={{ width: `${width}%` }}
                                                />
                                            </div>
                                            <div className="performance-summary">
                                                <div>
                                                    <span>Successful</span>
                                                    <strong>{successful}</strong>
                                                </div>
                                                <div>
                                                    <span>Failed</span>
                                                    <strong>{failed}</strong>
                                                </div>
                                                <div>
                                                    <span>Open</span>
                                                    <strong>{Math.max(total - successful - failed, 0)}</strong>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </section>

                    <section className="dashboard-section">
                        <div className="section-header">
                            <div>
                <span className="section-eyebrow">
                  RECENT ACTIVITY
                </span>

                                <h2>Recent Failed Payments</h2>
                            </div>

                            <span className="attempt-count">
                Latest {recentFailedPayments.length} failed
                payment
                                {recentFailedPayments.length !== 1
                                    ? "s"
                                    : ""}
              </span>
                        </div>

                        {recentFailedPayments.length === 0 ? (
                            <p className="empty-message">
                                No failed payments are available.
                            </p>
                        ) : (
                            <div className="table-wrapper">
                                <table className="recovery-table">
                                    <thead>
                                    <tr>
                                        <th>Payment ID</th>
                                        <th>Customer</th>
                                        <th>Amount</th>
                                        <th>Failure Category</th>
                                        <th>Created At</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    {recentFailedPayments.map((payment) => (
                                        <tr key={payment.id}>
                                            <td className="payment-id-cell">
                                                {payment.paymentId ||
                                                    `Payment #${payment.id}`}
                                            </td>

                                            <td>
                                                {payment.customerId ||
                                                    "Not available"}
                                            </td>

                                            <td className="amount-cell">
                                                {formatAmount(
                                                    payment.amount || 0
                                                )}
                                            </td>

                                            <td>
                          <span className="failure-category">
                            {payment.failureCategory
                                ? payment.failureCategory.replaceAll(
                                    "_",
                                    " "
                                )
                                : "Not available"}
                          </span>
                                            </td>

                                            <td>
                                                {formatDateTime(
                                                    payment.createdAt
                                                )}
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className="details-button"
                                                    onClick={() =>
                                                        openPaymentDetails(
                                                            payment.id
                                                        )
                                                    }
                                                >
                                                    View Details →
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                </>
            )}
        </>
    );

    const renderRecoveryAttempts = () => {
        const selectedPayment = payments.find(
            (payment) => String(payment.id) === String(selectedPaymentId)
        );
        const filteredPayments = payments.filter((payment) =>
            matchesSearch([
                payment.paymentId,
                payment.id,
                payment.customerId,
                payment.customerEmail,
                payment.status,
                payment.failureCategory,
                payment.failureReason,
            ], paymentSearch)
        );
        const filteredOperations = recoveryOperations.filter((operation) => {
            const filters = operationFilters;
            const relatedPayment = payments.find(
                (payment) => String(payment.id) === String(operation.paymentId)
            );
            const searchMatches = matchesSearch([
                operation.paymentReference,
                operation.paymentId,
                operation.failureCategory,
                operation.aiDecision,
                operation.recoveryAction,
                operation.recoveryMethod,
                operation.status,
                operation.customerAction,
                relatedPayment?.paymentId,
                relatedPayment?.amount,
                relatedPayment?.customerId,
                relatedPayment?.customerEmail,
                relatedPayment?.status,
                relatedPayment?.failureCategory,
                relatedPayment?.failureReason,
                relatedPayment?.paymentMethod,
                relatedPayment?.paymentMethodReference,
            ], paymentSearch);
            return searchMatches
                && (!filters.failureCategory || String(operation.failureCategory || "") === filters.failureCategory)
                && (!filters.recoveryMethod || String(operation.recoveryMethod || "") === filters.recoveryMethod)
                && (!filters.attempts || String(operation.attempts) === filters.attempts)
                && (!filters.status || String(operation.status || "") === filters.status)
                && (!filters.customerAction || (filters.customerAction === "REQUIRED"
                    ? operation.customerActionRequired
                    : !operation.customerActionRequired));
        });
        const filterOptions = (field) => [...new Set(
            recoveryOperations.map((operation) => operation[field]).filter(Boolean)
        )];
        const hasOperationFilter = paymentSearch.trim() || Object.values(operationFilters).some(Boolean);
        const visibleOperations = hasOperationFilter || showAllRecoveryOperations
            ? filteredOperations
            : filteredOperations.slice(0, 10);
        const visiblePaymentResults = showAllPaymentSearchResults
            ? filteredPayments
            : filteredPayments.slice(0, 8);

        const successfulAttempts = recoveryAttempts.filter(
            (attempt) =>
                ["SUCCESS", "RECOVERED"].includes(
                    String(attempt.status || "").toUpperCase()
                )
        ).length;

        const failedAttempts = recoveryAttempts.filter(
            (attempt) =>
                String(attempt.status || "").toUpperCase() === "FAILED"
        ).length;

        const latestAttempt = recoveryAttempts[0];
        const currentAttemptStatus = latestAttempt?.status ||
            selectedPayment?.status ||
            "No activity";

        return (
            <>
                <div className="page-heading">
                    <div>
                        <span className="eyebrow">RECOVERY OPERATIONS</span>
                        <h1>Recovery Attempts</h1>
                        <p className="subtitle">
                            Inspect the complete AI-assisted recovery journey for any payment.
                        </p>
                    </div>

                </div>

                {loading && (
                    <p className="status-message">Loading payments...</p>
                )}

                {error && (
                    <p className="error-message">{error}</p>
                )}

                {!loading && !error && (
                    <>
                        <section className="dashboard-section recovery-selector-card">
                            <div className="recovery-selector-heading">
                                <div>
                                    <span className="section-eyebrow">PAYMENT LOOKUP</span>
                                    <h2>Select a Payment</h2>
                                    <p className="section-description compact-description">
                                        Choose a payment to review every recovery attempt, response,
                                        and outcome in one place.
                                    </p>
                                </div>
                                <span className="attempt-count">
                                    {payments.length} payment{payments.length !== 1 ? "s" : ""} available
                                </span>
                            </div>
                            <input
                                className="payment-search-input"
                                type="search"
                                value={paymentSearch}
                                onChange={(event) => setPaymentSearch(event.target.value)}
                                placeholder="Search payments by ID, customer, status, or failure..."
                                aria-label="Search payments"
                            />
                            {paymentSearch.trim() && filteredPayments.length > 0 && (
                                <div className="payment-search-results">
                                    {visiblePaymentResults.map((payment) => (
                                        <button
                                            type="button"
                                            key={payment.id}
                                            onClick={() => {
                                                setSelectedPaymentId(String(payment.id));
                                                setPaymentSearch("");
                                                setShowAllPaymentSearchResults(false);
                                                loadRecoveryAttempts(payment.id);
                                            }}
                                        >
                                            {payment.paymentId || `Payment #${payment.id}`} — {formatLabel(payment.status)}
                                        </button>
                                    ))}
                                </div>
                            )}
                            {paymentSearch.trim() && filteredPayments.length > 8 && (
                                <button
                                    type="button"
                                    className="show-more-button"
                                    onClick={() => setShowAllPaymentSearchResults((current) => !current)}
                                >
                                    {showAllPaymentSearchResults ? "Show less" : `Show more (${filteredPayments.length - 8})`}
                                </button>
                            )}
                            {paymentSearch.trim() && filteredPayments.length === 0 && (
                                <p className="search-empty-message">
                                    No payment found for “{paymentSearch.trim()}”.
                                </p>
                            )}
                            {paymentSearch.trim() && (
                                <button
                                    type="button"
                                    className="clear-filters-button"
                                    onClick={() => {
                                        setPaymentSearch("");
                                        setShowAllPaymentSearchResults(false);
                                    }}
                                >
                                    Clear payment search
                                </button>
                            )}
                            {selectedPaymentId && (
                                <button
                                    type="button"
                                    className="clear-filters-button"
                                    onClick={() => {
                                        setSelectedPaymentId("");
                                        setRecoveryAttempts([]);
                                        setTotalAttempts(0);
                                        setPaymentSearch("");
                                        setShowAllPaymentSearchResults(false);
                                    }}
                                >
                                    Clear selected payment
                                </button>
                            )}
                        </section>

                        <div className="recovery-attempts-sections">
                        {selectedPayment && (
                            <section className="dashboard-section selected-payment-top-panel">
                                <div>
                                    <span className="section-eyebrow">SELECTED PAYMENT</span>
                                    <h2>{selectedPayment.paymentId || `Payment #${selectedPayment.id}`}</h2>
                                    <p className="section-description compact-description">
                                        {formatAmount(selectedPayment.amount || 0)}
                                        {" · "}
                                        {formatLabel(selectedPayment.status || "No status")}
                                        {" · "}
                                        {formatLabel(selectedPayment.failureCategory || "No failure category")}
                                    </p>
                                </div>
                                <button
                                    type="button"
                                    className="details-button"
                                    onClick={() => openPaymentDetails(selectedPayment.id)}
                                >
                                    Open Full Details →
                                </button>
                            </section>
                        )}

                        <section className="dashboard-section recovery-operations-card">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">AI RECOVERY OPERATIONS</span>
                                    <h2>Active and Completed Recovery Cases</h2>
                                </div>
                                <span className="attempt-count">
                                    {filteredOperations.length} case{filteredOperations.length !== 1 ? "s" : ""}
                                </span>
                            </div>
                            <p className="section-description compact-description">
                                Every row is assembled from the payment, AI decision, and recovery-attempt records returned by the backend.
                            </p>
                            <div className="operation-filters">
                                {[
                                    ["failureCategory", "Failure category"],
                                    ["recoveryMethod", "Recovery method"],
                                    ["attempts", "Attempts"],
                                    ["status", "Status"],
                                ].map(([field, label]) => (
                                    <select
                                        key={field}
                                        value={operationFilters[field]}
                                        onChange={(event) => setOperationFilters((current) => ({
                                            ...current,
                                            [field]: event.target.value,
                                        }))}
                                    >
                                        <option value="">All {label.toLowerCase()}s</option>
                                        {filterOptions(field).map((value) => (
                                            <option key={value} value={value}>{formatLabel(value)}</option>
                                        ))}
                                    </select>
                                ))}
                                <select
                                    value={operationFilters.customerAction}
                                    onChange={(event) => setOperationFilters((current) => ({
                                        ...current,
                                        customerAction: event.target.value,
                                    }))}
                                >
                                    <option value="">All customer actions</option>
                                    <option value="REQUIRED">Required</option>
                                    <option value="NOT_REQUIRED">Not required</option>
                                </select>
                                <button
                                    type="button"
                                    className="clear-filters-button"
                                    onClick={() => {
                                        setOperationFilters({
                                            failureCategory: "",
                                            recoveryMethod: "",
                                            attempts: "",
                                            status: "",
                                            customerAction: "",
                                        });
                                        setShowAllRecoveryOperations(false);
                                    }}
                                >
                                    Clear operation filters
                                </button>
                            </div>
                            {filteredOperations.length === 0 ? (
                                <p className="empty-message">
                                    {paymentSearch.trim()
                                        ? `No recovery case matches “${paymentSearch.trim()}”.`
                                        : "No analyzed recovery cases are available."}
                                </p>
                            ) : (
                                <div className="table-wrapper">
                                    <table className="recovery-table recovery-operations-table">
                                        <thead>
                                        <tr>
                                            <th>Payment</th>
                                            <th>Amount</th>
                                            <th>Failure Category</th>
                                            <th>AI Decision</th>
                                            <th>Recovery Method</th>
                                            <th>Attempts</th>
                                            <th>Status</th>
                                            <th>Customer Action</th>
                                            <th>Action</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {visibleOperations.map((operation) => (
                                            <tr key={operation.paymentId}>
                                                <td className="payment-id-cell">
                                                    {operation.paymentReference || `Payment #${operation.paymentId}`}
                                                </td>
                                                <td className="amount-cell">{formatAmount(operation.amount || 0)}</td>
                                                <td><span className="failure-category">{formatLabel(operation.failureCategory)}</span></td>
                                                <td>
                                                    <strong className="operation-decision">{formatLabel(operation.aiDecision)}</strong>
                                                    <small className="operation-action">{formatLabel(operation.recoveryAction)}</small>
                                                </td>
                                                <td>{formatLabel(operation.recoveryMethod)}</td>
                                                <td><strong>{operation.attempts} / {operation.maxAttempts}</strong></td>
                                                <td><span className={`status-badge status-${String(operation.status || "").toLowerCase()}`}>{formatLabel(operation.status)}</span></td>
                                                <td>
                                                    <span className={operation.customerActionRequired ? "customer-action-required" : "customer-action-not-required"}>
                                                        {operation.customerActionRequired ? "REQUIRED" : "NOT REQUIRED"}
                                                    </span>
                                                    {operation.customerAction && <small className="operation-action">{formatLabel(operation.customerAction)}</small>}
                                                </td>
                                                <td>
                                                    <button type="button" className="details-button" onClick={() => openPaymentDetails(operation.paymentId)}>
                                                        Open Details →
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                    {!hasOperationFilter && filteredOperations.length > 10 && (
                                        <button
                                            type="button"
                                            className="show-more-button"
                                            onClick={() => setShowAllRecoveryOperations((current) => !current)}
                                        >
                                            {showAllRecoveryOperations
                                                ? "Show less"
                                                : `Show more (${filteredOperations.length - 10})`}
                                        </button>
                                    )}
                                </div>
                            )}
                        </section>

                        {attemptsLoading && (
                            <p className="status-message">
                                Loading recovery attempts...
                            </p>
                        )}

                        {attemptsError && (
                            <p className="error-message">{attemptsError}</p>
                        )}

                        {!attemptsLoading && !attemptsError && selectedPaymentId && (
                            <div className="selected-recovery-content">
                                <div className="recovery-summary-grid">
                                    <div className="recovery-summary-card summary-total">
                                        <span className="summary-icon">↻</span>
                                        <div>
                                            <span className="summary-label">Total Attempts</span>
                                            <strong>{totalAttempts}</strong>
                                            <small>Recorded recovery actions</small>
                                        </div>
                                    </div>

                                    <div className="recovery-summary-card summary-success">
                                        <span className="summary-icon">✓</span>
                                        <div>
                                            <span className="summary-label">Successful</span>
                                            <strong>{successfulAttempts}</strong>
                                            <small>Attempts that recovered the payment</small>
                                        </div>
                                    </div>

                                    <div className="recovery-summary-card summary-failed">
                                        <span className="summary-icon">!</span>
                                        <div>
                                            <span className="summary-label">Failed</span>
                                            <strong>{failedAttempts}</strong>
                                            <small>Attempts requiring further action</small>
                                        </div>
                                    </div>

                                    <div className="recovery-summary-card summary-status">
                                        <span className="summary-icon">●</span>
                                        <div>
                                            <span className="summary-label">Latest Status</span>
                                            <strong className="summary-status-value">
                                                {currentAttemptStatus}
                                            </strong>
                                            <small>Most recent recovery state</small>
                                        </div>
                                    </div>
                                </div>

                                <section className="dashboard-section recovery-history-card">
                                    <div className="section-header">
                                        <div>
                                            <span className="section-eyebrow">RECOVERY TIMELINE</span>
                                            <h2>Recovery History</h2>
                                        </div>

                                        <span className="attempt-count">
                            {totalAttempts} attempt{totalAttempts !== 1 ? "s" : ""}
                          </span>
                                    </div>

                                    <p className="section-description compact-description">
                                        Review each recovery action in chronological history and track
                                        how the payment moved toward its current outcome.
                                    </p>

                                    <div className="attempts-journey">
                                        <div className="journey-step journey-risk">
                                            <strong>Failure</strong>
                                            <small>
                                                {selectedPayment?.failureCategory
                                                    ? formatLabel(selectedPayment.failureCategory)
                                                    : selectedPayment?.status === "RECOVERED"
                                                        ? "Recovered payment"
                                                        : "No failure category recorded"}
                                            </small>
                                        </div>
                                        <div className="journey-line" />
                                        <div className="journey-step journey-analysis">
                                            <strong>AI decision</strong>
                                            <small>
                                                {selectedPayment
                                                    ? recoveryAttempts.length > 0
                                                        ? "Attempt exists; AI decision not recorded"
                                                        : "No AI decision recorded"
                                                    : "Select a payment"}
                                            </small>
                                        </div>
                                        <div className="journey-line" />
                                        <div className="journey-step journey-active">
                                            <strong>Recovery</strong>
                                            <small>
                                                {totalAttempts > 0
                                                    ? `${totalAttempts} attempt${totalAttempts !== 1 ? "s" : ""} recorded`
                                                    : "No attempts recorded"}
                                            </small>
                                        </div>
                                        <div className="journey-line" />
                                        <div className={`journey-step journey-${getAttemptTone(selectedPayment?.status)}`}>
                                            <strong>Result</strong>
                                            <small>
                                                {selectedPayment?.status
                                                    ? formatLabel(selectedPayment.status)
                                                    : "No result recorded"}
                                            </small>
                                        </div>
                                    </div>

                                    {recoveryAttempts.length === 0 ? (
                                        <p className="empty-message">
                                            No recovery attempts found for this payment.
                                        </p>
                                    ) : (
                                        <div className="table-wrapper">
                                            <table className="recovery-table recovery-history-table">
                                                <thead>
                                                <tr>
                                                    <th>Attempt</th>
                                                    <th>Attempted At</th>
                                                    <th>Method</th>
                                                    <th>Status</th>
                                                    <th>Decision / Response</th>
                                                </tr>
                                                </thead>

                                                <tbody>
                                                {recoveryAttempts.map((attempt, index) => (
                                                    <tr key={attempt.id}>
                                                        <td>
                                                            <div className="attempt-id-cell">
                                                                <span className="attempt-number">{index + 1}</span>
                                                                <strong>#{attempt.id}</strong>
                                                            </div>
                                                        </td>

                                                        <td>{formatDateTime(attempt.attemptedAt)}</td>

                                                        <td>
                                        <span className="method-badge">
                                          {attempt.recoveryMethod || "Not available"}
                                        </span>
                                                        </td>

                                                        <td>
                                        <span
                                            className={`status-badge status-${attempt.status?.toLowerCase()}`}
                                        >
                                          {attempt.status || "UNKNOWN"}
                                        </span>
                                                        </td>

                                                        <td className="attempt-response-cell">
                                                            <strong>{formatLabel(attempt.customerAction || attempt.recoveryMethod)}</strong>
                                                            <span>{attempt.response || "No response available"}</span>
                                                            {attempt.scheduledRetryAt && (
                                                                <small>Scheduled: {formatDateTime(attempt.scheduledRetryAt)}</small>
                                                            )}
                                                        </td>
                                                    </tr>
                                                ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </section>
                            </div>
                        )}
                        </div>
                    </>
                )}
            </>
        );
    };

    const renderAnalytics = () => {
        const categoryMax = Math.max(
            ...failureCategories.map((item) => Number(item.count) || 0),
            1
        );

        const recoveryProgress = Math.max(0, Math.min(Number(recoveryRate) || 0, 100));

        return (
            <>
                <div className="page-heading analytics-page-heading">
                    <div>
                        <span className="eyebrow">RECOVERY INTELLIGENCE</span>
                        <h1>Analytics</h1>
                        <p className="subtitle">
                            Analyze payment failures, recovery performance, and the patterns driving risk.
                        </p>
                    </div>
                </div>

                {loading && (
                    <p className="status-message">
                        Loading analytics data...
                    </p>
                )}

                {error && (
                    <p className="error-message">{error}</p>
                )}

                {!loading && !error && (
                    <>
                        <div className="stats-grid analytics-stats-grid">
                            <div className="stat-card stat-card-rate analytics-highlight-card">
                                <span className="stat-label">Recovery Rate</span>
                                <strong className="stat-value">{recoveryRate}%</strong>
                                <span className="stat-description">
                      {successfulRecoveryAttempts} of {completedRecoveryAttempts} completed recovery attempts succeeded
                    </span>
                            </div>

                            <div className="stat-card stat-card-recovered analytics-highlight-card">
                                <span className="stat-label">Recovered Amount</span>
                                <strong className="stat-value amount-value">
                                    {formatAmount(recoveredAmount)}
                                </strong>
                                <span className="stat-description">Total value successfully recovered</span>
                            </div>

                            <div className="stat-card stat-card-failed analytics-highlight-card">
                                <span className="stat-label">Failed Amount</span>
                                <strong className="stat-value amount-value">
                                    {formatAmount(failedAmount)}
                                </strong>
                                <span className="stat-description">Value currently associated with failed payments</span>
                            </div>

                            <div className="stat-card analytics-highlight-card">
                                <span className="stat-label">Total Failed</span>
                                <strong className="stat-value">{failedPayments}</strong>
                                <span className="stat-description">Payments requiring recovery attention</span>
                            </div>
                        </div>

                        <section className="dashboard-section analytics-status-section">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">PAYMENT OUTCOMES</span>
                                    <h2>Payments by Status</h2>
                                </div>
                            </div>

                            <div className="analytics-grid">
                                {analyticsStatusBreakdown.map((item) => (
                                    <div className="analytics-card status-analytics-card" key={item.status}>
                                        <span>{item.label}</span>
                                        <strong>{item.count}</strong>
                                        <small>
                                            {totalPayments > 0
                                                ? `${Math.round((item.count / totalPayments) * 100)}% of monitored payments`
                                                : "No payments available"}
                                        </small>
                                    </div>
                                ))}
                            </div>
                        </section>

                        <section className="dashboard-section analytics-status-section">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">RECOVERY OPERATIONS</span>
                                    <h2>Backend Recovery Analytics</h2>
                                </div>
                            </div>

                            <div className="analytics-grid">
                                <div className="analytics-card status-analytics-card">
                                    <span>Total recovered attempts</span>
                                    <strong>{successfulRecoveryAttempts}</strong>
                                    <small>Recovery attempts completed successfully</small>
                                </div>
                                <div className="analytics-card status-analytics-card">
                                    <span>Successful recovery attempts</span>
                                    <strong>{successfulRecoveryAttempts}</strong>
                                    <small>Recovery executions completed successfully</small>
                                </div>
                                <div className="analytics-card status-analytics-card">
                                    <span>Open recovery attempts</span>
                                    <strong>{analyticsPendingRecoveryAttempts}</strong>
                                    <small>Pending, processing, scheduled, or awaiting customer action</small>
                                </div>
                            </div>
                        </section>

                        <section className="dashboard-section comparison-card">
                            <div className="section-header">
                                <div>
                                    <span className="section-eyebrow">BUILDATHON BENCHMARK</span>
                                    <h2>RecoverAI vs Blind Retry</h2>
                                    <p className="section-description compact-description">
                                        Same failed-payment cohort, compared with a baseline that retries every payment once without AI or policy controls.
                                    </p>
                                </div>
                                {recoveryComparison && (
                                    <span className="comparison-improvement">
                                        +{recoveryComparison.recoveryLiftPercentage}% lift
                                    </span>
                                )}
                            </div>

                            {!recoveryComparison ? (
                                <p className="empty-message">Comparison data is not available yet.</p>
                            ) : (
                                <>
                                    <div className="comparison-grid">
                                        <div className="comparison-metric">
                                            <span>Recovery rate</span>
                                            <div className="comparison-values">
                                                <strong>{recoveryComparison.recoverAiRecoveryRate}%</strong>
                                                <small>vs {recoveryComparison.blindRetryRecoveryRate}% blind retry</small>
                                            </div>
                                            <div className="comparison-bar-track">
                                                <div className="comparison-bar-ai" style={{ width: `${Math.min(recoveryComparison.recoverAiRecoveryRate, 100)}%` }} />
                                                <div className="comparison-bar-blind" style={{ width: `${Math.min(recoveryComparison.blindRetryRecoveryRate, 100)}%` }} />
                                            </div>
                                        </div>
                                        <div className="comparison-metric">
                                            <span>Revenue recovered</span>
                                            <div className="comparison-values">
                                                <strong>{formatAmount(recoveryComparison.recoverAiRevenueRecovered)}</strong>
                                                <small>vs {formatAmount(recoveryComparison.blindRetryRevenueRecovered)}</small>
                                            </div>
                                            <small className="comparison-positive">
                                                +{formatAmount(recoveryComparison.additionalRevenueRecovered)} incremental revenue
                                            </small>
                                        </div>
                                        <div className="comparison-metric">
                                            <span>Attempts</span>
                                            <div className="comparison-values">
                                                <strong>{recoveryComparison.recoverAiAttempts}</strong>
                                                <small>vs {recoveryComparison.blindRetryAttempts} blind retries</small>
                                            </div>
                                            <small>Bounded, policy-aware execution</small>
                                        </div>
                                        <div className="comparison-metric">
                                            <span>Unnecessary retries</span>
                                            <div className="comparison-values">
                                                <strong>{recoveryComparison.unnecessaryBlindRetries}</strong>
                                                <small>blind retry baseline</small>
                                            </div>
                                            <small>RecoverAI filters ineligible failures before recovery</small>
                                        </div>
                                        <div className="comparison-metric">
                                            <span>Customer-action cases</span>
                                            <div className="comparison-values">
                                                <strong>{recoveryComparison.customerActionCases}</strong>
                                                <small>RecoverAI</small>
                                            </div>
                                            <small>Customer control replaces uncontrolled retries</small>
                                        </div>
                                        <div className="comparison-metric">
                                            <span>Policy-stopped cases</span>
                                            <div className="comparison-values">
                                                <strong>{recoveryComparison.policyStoppedCases}</strong>
                                                <small>RecoverAI</small>
                                            </div>
                                            <small>Non-recoverable or unsafe recovery blocked</small>
                                        </div>
                                    </div>
                                    <div className="comparison-footer">
                                        <span>{recoveryComparison.paymentCohort} failed payments in the benchmark cohort</span>
                                        <span>Revenue at risk: {formatAmount(recoveryComparison.revenueAtRisk)}</span>
                                    </div>
                                    <div className="analytics-subsection-grid">
                                        <div>
                                            <span>Recovery funnel</span>
                                            <strong>{recoveryComparison.paymentCohort} → {recoveryComparison.aiAnalyzed} → {recoveryComparison.policyApproved} → {recoveryComparison.recoveryAttempted} → {recoveryComparison.recoverAiRecoveredPayments}</strong>
                                            <small>Failed → AI analyzed → policy approved → attempted → recovered</small>
                                        </div>
                                        <div>
                                            <span>AI safety outcomes</span>
                                            <strong>{recoveryComparison.customerActionCases + recoveryComparison.policyStoppedCases + recoveryComparison.maxAttemptsReached}</strong>
                                            <small>{recoveryComparison.customerActionCases} customer action · {recoveryComparison.policyStoppedCases} policy blocked · {recoveryComparison.maxAttemptsReached} max-attempt cases</small>
                                        </div>
                                    </div>
                                </>
                            )}
                        </section>

                        <div className="analytics-two-column">
                            <section className="dashboard-section analytics-category-section">
                                <div className="section-header">
                                    <div>
                                        <span className="section-eyebrow">RISK BREAKDOWN</span>
                                        <h2>Failure Categories</h2>
                                    </div>
                                </div>

                                {failureCategories.length === 0 ? (
                                    <p className="empty-message">
                                        No failed payment categories are available.
                                    </p>
                                ) : (
                                    <div className="category-bar-list">
                                        {failureCategories.map((item) => {
                                            const percentage = Math.round(
                                                ((Number(item.count) || 0) / categoryMax) * 100
                                            );

                                            return (
                                                <div className="category-bar-item" key={item.category}>
                                                    <div className="category-bar-header">
                                                        <span>{item.category.replaceAll("_", " ")}</span>
                                                        <strong>{item.count}</strong>
                                                    </div>
                                                    <div className="category-bar-track">
                                                        <div
                                                            className="category-bar-fill"
                                                            style={{ width: `${percentage}%` }}
                                                        />
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}
                            </section>

                            <section className="dashboard-section analytics-reasons-section">
                                <div className="section-header">
                                    <div>
                                        <span className="section-eyebrow">RISK PRIORITIZATION</span>
                                        <h2>Failure Impact Analysis</h2>
                                        <p className="section-description compact-description">
                                            Prioritize failed-payment categories by payment count and revenue currently at risk.
                                        </p>
                                    </div>
                                </div>

                                {failureImpact.length === 0 ? (
                                    <p className="empty-message">
                                        No failed-payment impact data is available.
                                    </p>
                                ) : (
                                    <div className="failure-impact-list">
                                        {failureImpact.map((item, index) => (
                                            <div className="failure-impact-item" key={item.category}>
                                                <div>
                                                    <span className="reason-rank">{index + 1}</span>
                                                    <strong>{formatLabel(item.category)}</strong>
                                                </div>
                                                <span>{item.count} payment{item.count !== 1 ? "s" : ""}</span>
                                                <strong>{formatAmount(item.amount)}</strong>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </section>
                        </div>

                        <section className="dashboard-section recovery-performance-card">
                            <div className="section-header recovery-performance-header">
                                <div>
                                    <span className="section-eyebrow">RECOVERY EFFECTIVENESS</span>
                                    <h2>Recovery Performance</h2>
                                    <p className="section-description compact-description">
                                        Measure how effectively completed recovery attempts result in successful payment recovery.
                                    </p>
                                </div>
                                <div className="recovery-rate-highlight">
                                    {recoveryRate}%
                                </div>
                            </div>

                            <div className="analytics-performance-track progress-track">
                                <div
                                    className="progress-fill"
                                    style={{ width: `${recoveryProgress}%` }}
                                />
                            </div>

                            <div className="performance-summary analytics-performance-summary">
                                <div>
                                    <span>Successful Attempts</span>
                                    <strong>{successfulRecoveryAttempts}</strong>
                                </div>
                                <div>
                                    <span>Failed Attempts</span>
                                    <strong>{failedRecoveryAttempts}</strong>
                                </div>
                                <div>
                                    <span>Completed Attempts</span>
                                    <strong>{completedRecoveryAttempts}</strong>
                                </div>
                            </div>
                        </section>
                    </>
                )}
            </>
        );
    };

    const renderPaymentDetails = () => {
        const payment = paymentDetails?.payment;
        const filteredDetailPayments = payments.filter((paymentItem) =>
            matchesSearch([
                paymentItem.paymentId,
                paymentItem.id,
                paymentItem.customerId,
                paymentItem.customerEmail,
                paymentItem.status,
                paymentItem.failureCategory,
                paymentItem.failureReason,
            ], detailsPaymentSearch)
        );
        const visibleDetailResults = showAllDetailSearchResults
            ? filteredDetailPayments
            : filteredDetailPayments.slice(0, 8);

        const aiDecisions =
            paymentDetails?.aiDecisions || [];

        const detailsRecoveryAttempts =
            paymentDetails?.recoveryAttempts || [];

        const auditLogs =
            paymentDetails?.auditLogs || [];

        const fallbackFailureCategory =
            payment?.failureCategory ||
            detailsRecoveryAttempts.find((attempt) => attempt.failureReason)
                ?.failureReason;

        const fallbackFailureReason =
            payment?.failureReason ||
            (fallbackFailureCategory
                ? `Original failure category: ${formatLabel(fallbackFailureCategory)}`
                : null);

        return (
            <>
                <h1>Payment Details</h1>

                <p className="subtitle">
                    View complete payment information and AI-powered
                    recovery insights.
                </p>

                {loading && (
                    <p className="status-message">
                        Loading payments...
                    </p>
                )}

                {error && (
                    <p className="error-message">{error}</p>
                )}

                {!loading && !error && (
                    <>
                        <section className="dashboard-section">
                            <h2>Select Payment</h2>

                            <input
                                className="payment-search-input"
                                type="search"
                                value={detailsPaymentSearch}
                                onChange={(event) => setDetailsPaymentSearch(event.target.value)}
                                placeholder="Search payments by ID, customer, status, or failure..."
                                aria-label="Search payments for details"
                            />
                            {detailsPaymentSearch.trim() && (
                                <button
                                    type="button"
                                    className="clear-filters-button"
                                    onClick={() => {
                                        setDetailsPaymentSearch("");
                                        setShowAllDetailSearchResults(false);
                                    }}
                                >
                                    Clear search
                                </button>
                            )}
                            {selectedDetailsPaymentId && (
                                <button
                                    type="button"
                                    className="clear-filters-button"
                                    onClick={() => {
                                        setSelectedDetailsPaymentId("");
                                        setPaymentDetails(null);
                                        setDetailsPaymentSearch("");
                                        setShowAllDetailSearchResults(false);
                                    }}
                                >
                                    Clear selected payment
                                </button>
                            )}
                            {detailsPaymentSearch.trim() && filteredDetailPayments.length === 0 && (
                                <p className="search-empty-message">
                                    No payment found for “{detailsPaymentSearch.trim()}”.
                                </p>
                            )}
                            {detailsPaymentSearch.trim() && filteredDetailPayments.length > 0 && (
                                <div className="payment-search-results">
                                    {visibleDetailResults.map((paymentItem) => (
                                        <button
                                            type="button"
                                            key={paymentItem.id}
                                            onClick={() => {
                                                setSelectedDetailsPaymentId(String(paymentItem.id));
                                                setDetailsPaymentSearch("");
                                                setShowAllDetailSearchResults(false);
                                                loadPaymentDetails(paymentItem.id);
                                            }}
                                        >
                                            {paymentItem.paymentId || `Payment #${paymentItem.id}`} — {formatLabel(paymentItem.status)}
                                        </button>
                                    ))}
                                </div>
                            )}
                            {detailsPaymentSearch.trim() && filteredDetailPayments.length > 8 && (
                                <button
                                    type="button"
                                    className="show-more-button"
                                    onClick={() => setShowAllDetailSearchResults((current) => !current)}
                                >
                                    {showAllDetailSearchResults
                                        ? "Show less"
                                        : `Show more (${filteredDetailPayments.length - 8})`}
                                </button>
                            )}
                        </section>

                        {detailsLoading && (
                            <p className="status-message">
                                Loading complete payment details...
                            </p>
                        )}

                        {detailsError && (
                            <p className="error-message">
                                {detailsError}
                            </p>
                        )}

                        {!detailsLoading &&
                            paymentDetails &&
                            payment && (
                                <>
                                    <section className="dashboard-section">
                                        <h2>Payment Information</h2>

                                        <div className="details-grid">
                                            <div className="detail-item">
                                                <span>Payment ID</span>

                                                <strong>
                                                    {payment.paymentId ||
                                                        "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Database ID</span>

                                                <strong>#{payment.id}</strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Customer ID</span>

                                                <strong>
                                                    {payment.customerId ||
                                                        "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Amount</span>

                                                <strong>
                                                    {formatAmount(
                                                        payment.amount || 0
                                                    )}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Status</span>

                                                <strong>
                          <span
                              className={`status-badge status-${payment.status?.toLowerCase()}`}
                          >
                            {payment.status}
                          </span>
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Failure Category</span>

                                                <strong>
                                                    {fallbackFailureCategory
                                                        ? formatLabel(fallbackFailureCategory)
                                                        : "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Failure Reason</span>

                                                <strong>
                                                    {fallbackFailureReason ||
                                                        "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Payment Method</span>

                                                <strong>
                                                    {payment.paymentMethod ||
                                                        "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Payment Method Reference</span>

                                                <strong>
                                                    {payment.paymentMethodReference ||
                                                        "Not available"}
                                                </strong>
                                            </div>

                                            <div className="detail-item">
                                                <span>Created At</span>

                                                <strong>
                                                    {formatDateTime(
                                                        payment.createdAt
                                                    )}
                                                </strong>
                                            </div>
                                        </div>
                                    </section>

                                    <section className="dashboard-section lifecycle-card">
                                        <div className="section-header">
                                            <div>
                                                <span className="section-eyebrow">END-TO-END JOURNEY</span>
                                                <h2>What happened to this payment?</h2>
                                            </div>
                                            <span className={`status-badge status-${payment.status?.toLowerCase()}`}>
                                                {formatLabel(payment.status)}
                                            </span>
                                        </div>
                                        <div className="lifecycle-steps">
                                            <div className="lifecycle-step complete">
                                                <span>1</span><strong>Failure</strong>
                                                <small>{formatLabel(payment.failureCategory)}</small>
                                            </div>
                                            <div className="lifecycle-connector" />
                                            <div className={`lifecycle-step ${aiDecisions.length ? "complete" : ""}`}>
                                                <span>2</span><strong>AI analysis</strong>
                                                <small>{aiDecisions[0] ? formatLabel(aiDecisions[0].decision) : "Pending"}</small>
                                            </div>
                                            <div className="lifecycle-connector" />
                                            <div className={`lifecycle-step ${aiDecisions.length ? "complete" : ""}`}>
                                                <span>3</span><strong>Policy</strong>
                                                <small>{aiDecisions[0]?.recoveryAction ? formatLabel(aiDecisions[0].recoveryAction) : "Not evaluated"}</small>
                                            </div>
                                            <div className="lifecycle-connector" />
                                            <div className={`lifecycle-step ${detailsRecoveryAttempts.length ? "complete" : ""}`}>
                                                <span>4</span><strong>Recovery</strong>
                                                <small>{detailsRecoveryAttempts.length} attempt{detailsRecoveryAttempts.length !== 1 ? "s" : ""}</small>
                                            </div>
                                            <div className="lifecycle-connector" />
                                            <div className={`lifecycle-step ${payment.status === "RECOVERED" || auditLogs.length ? "complete" : ""}`}>
                                                <span>5</span><strong>Result + audit</strong>
                                                <small>{auditLogs.length} logged event{auditLogs.length !== 1 ? "s" : ""}</small>
                                            </div>
                                        </div>
                                    </section>

                                    <section className="dashboard-section compact-policy-card">
                                        <div className="section-header">
                                            <div>
                                                <span className="section-eyebrow">POLICY CHECK</span>
                                                <h2>Recovery Policy</h2>
                                            </div>
                                            <span className="guardrails-status"><span className="guardrails-status-dot"></span>Active</span>
                                        </div>
                                        <div className="compact-policy-grid">
                                            <span>✓ Maximum attempts: 5</span>
                                            <span>✓ Duplicate recovery protection</span>
                                            <span>✓ Customer-first recovery</span>
                                            <span>✓ Non-recoverable failure protection</span>
                                            <span>✓ Promise-to-pay protection</span>
                                            <span>✓ Audit logging</span>
                                        </div>
                                    </section>

                                    <section className="dashboard-section">
                                        <h2>AI Recovery Insights</h2>

                                        {aiDecisions.length === 0 ? (
                                            <div className="detail-empty-state">
                                                <strong>No AI decision recorded</strong>
                                                <span>
                                                    This payment has a recovery record but no AI analysis entry. It was likely created directly through the recovery endpoint or seeded without calling the AI decision endpoint. The details below reflect the records returned by the backend.
                                                </span>
                                            </div>
                                        ) : (
                                            <div className="ai-decisions-list">
                                                {aiDecisions.map((decision) => (
                                                    <div
                                                        className="ai-decision-card"
                                                        key={decision.id}
                                                    >
                                                        <div className="ai-decision-header">
                                                            <strong>
                                                                {decision.decision ||
                                                                    "AI Decision"}
                                                            </strong>

                                                            <span className="confidence-score">
                                Confidence:{" "}
                                                                {formatConfidence(
                                                                    decision.confidenceScore
                                                                )}
                              </span>
                                                        </div>

                                                        <div className="ai-decision-grid">
                                                            <div>
                                <span>
                                  Recovery Action
                                </span>

                                                                <strong>
                                                                    {decision.recoveryAction ||
                                                                        "Not available"}
                                                                </strong>
                                                            </div>

                                                            <div>
                                <span>
                                  Decision Source
                                </span>

                                                                <strong>
                                                                    {decision.decisionSource ||
                                                                        "Not available"}
                                                                </strong>
                                                            </div>

                                                            <div>
                                                                <span>Created At</span>

                                                                <strong>
                                                                    {formatDateTime(
                                                                        decision.createdAt
                                                                    )}
                                                                </strong>
                                                            </div>
                                                        </div>

                                                        <div className="ai-reason">
                                                            <span>AI Reasoning</span>

                                                            <p>
                                                                {decision.reason ||
                                                                    "No reasoning was provided."}
                                                            </p>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </section>

                                    {(() => {
                                        const waitingForCustomerAttempt =
                                            detailsRecoveryAttempts.find(
                                                (attempt) =>
                                                    attempt.status ===
                                                    "WAITING_FOR_CUSTOMER"
                                            );

                                        const waitingForPaymentMethodAttempt =
                                            detailsRecoveryAttempts.find(
                                                (attempt) =>
                                                    attempt.status ===
                                                    "WAITING_FOR_PAYMENT_METHOD"
                                            );

                                        const scheduledAttempt =
                                            detailsRecoveryAttempts.find(
                                                (attempt) =>
                                                    attempt.status === "SCHEDULED"
                                            );

                                        const successfulAttempt =
                                            detailsRecoveryAttempts.find(
                                                (attempt) =>
                                                    attempt.status === "SUCCESS"
                                            );

                                        if (!waitingForCustomerAttempt &&
                                            !waitingForPaymentMethodAttempt &&
                                            !scheduledAttempt &&
                                            !successfulAttempt) {
                                            return null;
                                        }

                                        return (
                                            <section className="dashboard-section customer-recovery-card">
                                                <div className="section-header">
                                                    <div>
                                                        <span className="section-eyebrow">
                                                            CUSTOMER RECOVERY ACTION
                                                        </span>
                                                        <h2>Complete Your Recovery</h2>
                                                    </div>

                                                    <span
                                                        className={`status-badge status-${(
                                                            waitingForCustomerAttempt?.status ||
                                                            waitingForPaymentMethodAttempt?.status ||
                                                            scheduledAttempt?.status ||
                                                            successfulAttempt?.status ||
                                                            ""
                                                        ).toLowerCase()}`}
                                                    >
                                                        {waitingForCustomerAttempt?.status ||
                                                            waitingForPaymentMethodAttempt?.status ||
                                                            scheduledAttempt?.status ||
                                                            successfulAttempt?.status}
                                                    </span>
                                                </div>

                                                {recoveryActionError && (
                                                    <p className="recovery-action-error">
                                                        {recoveryActionError}
                                                    </p>
                                                )}

                                                {recoveryActionMessage && (
                                                    <p className="recovery-action-success">
                                                        {recoveryActionMessage}
                                                    </p>
                                                )}

                                                {waitingForCustomerAttempt && (
                                                    <div className="customer-action-content">
                                                        <div className="customer-action-intro">
                                                            <div className="customer-action-icon">!</div>
                                                            <div>
                                                                <h3>
                                                                    Your payment needs attention
                                                                </h3>
                                                                <p>
                                                                    We detected a temporary payment issue. Choose when to retry or use another payment method.
                                                                </p>
                                                            </div>
                                                        </div>

                                                        <div className="outreach-status">
                                                            <span>✉</span> Email
                                                            <span>◉</span> WhatsApp
                                                            <span>☎</span> Phone call
                                                            <button
                                                                type="button"
                                                                disabled={recoveryActionLoading}
                                                                onClick={() => handleNotifyCustomer(waitingForCustomerAttempt.id)}
                                                            >
                                                                Resend recovery message
                                                            </button>
                                                        </div>

                                                        <div className="recovery-guardrails">
                                                            <strong>Customer protection is on</strong>
                                                            <span>• Promise deadlines pause recovery</span>
                                                            <span>• Outreach has a 30-minute cooldown</span>
                                                            <span>• Recovery is capped at 5 attempts</span>
                                                        </div>

                                                        <div className="recovery-option-grid">
                                                            <div className="recovery-option">
                                                                <span className="option-label">
                                                                    RETRY LATER
                                                                </span>
                                                                <h3>
                                                                    Schedule another attempt
                                                                </h3>
                                                                <p>
                                                                    Choose a future date and time when funds may be available.
                                                                </p>

                                                                <input
                                                                    type="datetime-local"
                                                                    value={scheduledRetryAt}
                                                                    min={new Date(
                                                                        Date.now() + 60 * 1000
                                                                    )
                                                                        .toISOString()
                                                                        .slice(0, 16)}
                                                                    onChange={(event) =>
                                                                        setScheduledRetryAt(
                                                                            event.target.value
                                                                        )
                                                                    }
                                                                />

                                                                <button
                                                                    type="button"
                                                                    className="recovery-action-button primary-action"
                                                                    disabled={recoveryActionLoading}
                                                                    onClick={() =>
                                                                        handleRetryLater(
                                                                            waitingForCustomerAttempt.id
                                                                        )
                                                                    }
                                                                >
                                                                    {recoveryActionLoading
                                                                        ? "Scheduling..."
                                                                        : "Schedule Retry"}
                                                                </button>
                                                            </div>

                                                            <div className="recovery-option promise-to-pay-option">
                                                                <span className="option-label">
                                                                    PROMISE TO PAY
                                                                </span>
                                                                <h3>
                                                                    I will complete payment by
                                                                </h3>
                                                                <p>
                                                                    Set a deadline. RecoverAI pauses retries and reminders until then.
                                                                </p>

                                                                <input
                                                                    type="datetime-local"
                                                                    value={promiseToPayDeadline}
                                                                    min={new Date(Date.now() + 60 * 1000)
                                                                        .toISOString()
                                                                        .slice(0, 16)}
                                                                    onChange={(event) =>
                                                                        setPromiseToPayDeadline(event.target.value)
                                                                    }
                                                                />

                                                                <button
                                                                    type="button"
                                                                    className="recovery-action-button promise-action"
                                                                    disabled={recoveryActionLoading}
                                                                    onClick={() =>
                                                                        handlePromiseToPay(waitingForCustomerAttempt.id)
                                                                    }
                                                                >
                                                                    {recoveryActionLoading
                                                                        ? "Saving..."
                                                                        : "Record Promise"}
                                                                </button>
                                                            </div>

                                                            <div className="recovery-option">
                                                                <span className="option-label">
                                                                    ANOTHER PAYMENT METHOD
                                                                </span>
                                                                <h3>
                                                                    Use another account or method
                                                                </h3>
                                                                <p>
                                                                    Switch to UPI, another card, or another available payment account.
                                                                </p>

                                                                <button
                                                                    type="button"
                                                                    className="recovery-action-button secondary-action"
                                                                    disabled={recoveryActionLoading}
                                                                    onClick={() =>
                                                                        handleChooseAnotherPaymentMethod(
                                                                            waitingForCustomerAttempt.id
                                                                        )
                                                                    }
                                                                >
                                                                    {recoveryActionLoading
                                                                        ? "Updating..."
                                                                        : "Choose Payment Method"}
                                                                </button>
                                                            </div>
                                                        </div>
                                                    </div>
                                                )}

                                                {waitingForPaymentMethodAttempt && (
                                                    <div className="alternative-payment-form">
                                                        <div className="customer-action-intro">
                                                            <div className="customer-action-icon">↻</div>
                                                            <div>
                                                                <h3>
                                                                    Enter your alternative payment method
                                                                </h3>
                                                                <p>
                                                                    We will use the selected method only for this recovery attempt.
                                                                </p>
                                                            </div>
                                                        </div>

                                                        <div className="alternative-form-grid">
                                                            <label>
                                                                Payment Method
                                                                <select
                                                                    value={alternativePaymentMethod}
                                                                    onChange={(event) =>
                                                                        setAlternativePaymentMethod(
                                                                            event.target.value
                                                                        )
                                                                    }
                                                                >
                                                                    <option value="UPI">
                                                                        UPI
                                                                    </option>
                                                                    <option value="CARD">
                                                                        Card
                                                                    </option>
                                                                    <option value="BANK_ACCOUNT">
                                                                        Bank Account
                                                                    </option>
                                                                </select>
                                                            </label>

                                                            <label>
                                                                Method / Account Reference
                                                                <input
                                                                    type="text"
                                                                    placeholder={
                                                                        alternativePaymentMethod === "UPI"
                                                                            ? "example@bank"
                                                                            : alternativePaymentMethod === "CARD"
                                                                                ? "CARD_****5678"
                                                                                : "Account reference"
                                                                    }
                                                                    value={
                                                                        alternativePaymentMethodReference
                                                                    }
                                                                    onChange={(event) =>
                                                                        setAlternativePaymentMethodReference(
                                                                            event.target.value
                                                                        )
                                                                    }
                                                                />
                                                            </label>
                                                        </div>

                                                        <button
                                                            type="button"
                                                            className="recovery-action-button primary-action"
                                                            disabled={recoveryActionLoading}
                                                            onClick={() =>
                                                                handleAlternativePaymentMethod(
                                                                    waitingForPaymentMethodAttempt.id
                                                                )
                                                            }
                                                        >
                                                            {recoveryActionLoading
                                                                ? "Processing..."
                                                                : "Submit Alternative Method"}
                                                        </button>
                                                    </div>
                                                )}

                                                {scheduledAttempt && (
                                                    <div className="recovery-state-card scheduled-state">
                                                        <div>
                                                            <span className="state-icon">◷</span>
                                                        </div>
                                                        <div>
                                                            <h3>
                                                                {scheduledAttempt.customerAction === "PROMISE_TO_PAY"
                                                                    ? "Promise to pay recorded"
                                                                    : "Retry scheduled"}
                                                            </h3>
                                                            <p>
                                                                {scheduledAttempt.customerAction === "PROMISE_TO_PAY"
                                                                    ? "RecoverAI will respect the customer promise and resume recovery at "
                                                                    : "RecoverAI will automatically retry this payment at "}
                                                                <strong>
                                                                    {formatDateTime(scheduledAttempt.promiseToPayDeadline || scheduledAttempt.scheduledRetryAt)}
                                                                </strong>
                                                                .
                                                            </p>
                                                        </div>
                                                    </div>
                                                )}

                                                {successfulAttempt && (
                                                    <div className="recovery-state-card success-state">
                                                        <div>
                                                            <span className="state-icon">✓</span>
                                                        </div>
                                                        <div>
                                                            <h3>
                                                                Payment recovered successfully
                                                            </h3>
                                                            <p>
                                                                This recovery journey has completed successfully.
                                                            </p>
                                                        </div>
                                                    </div>
                                                )}
                                            </section>
                                        );
                                    })()}

                                    <section className="dashboard-section">
                                        <div className="section-header">
                                            <h2>Recovery History</h2>

                                            <span className="attempt-count">
                        {detailsRecoveryAttempts.length}{" "}
                                                attempt
                                                {detailsRecoveryAttempts.length !== 1
                                                    ? "s"
                                                    : ""}
                      </span>
                                        </div>

                                        {detailsRecoveryAttempts.length === 0 ? (
                                            <p className="empty-message">
                                                No recovery attempts are available.
                                            </p>
                                        ) : (
                                            <div className="table-wrapper">
                                                <table className="recovery-table">
                                                    <thead>
                                                    <tr>
                                                        <th>Attempt ID</th>
                                                        <th>Attempted At</th>
                                                        <th>Method</th>
                                                        <th>Status</th>
                                                        <th>Response</th>
                                                    </tr>
                                                    </thead>

                                                    <tbody>
                                                    {detailsRecoveryAttempts.map(
                                                        (attempt) => (
                                                            <tr key={attempt.id}>
                                                                <td>#{attempt.id}</td>

                                                                <td>
                                                                    {formatDateTime(
                                                                        attempt.attemptedAt
                                                                    )}
                                                                </td>

                                                                <td>
                                                                    {attempt.recoveryMethod}
                                                                </td>

                                                                <td>
                                    <span
                                        className={`status-badge status-${attempt.status?.toLowerCase()}`}
                                    >
                                      {attempt.status}
                                    </span>
                                                                </td>

                                                                <td>
                                                                    {attempt.response}
                                                                </td>
                                                            </tr>
                                                        )
                                                    )}
                                                    </tbody>
                                                </table>
                                            </div>
                                        )}
                                    </section>

                                    <section className="dashboard-section">
                                        <h2>Audit Log</h2>

                                        {auditLogs.length === 0 ? (
                                            <p className="empty-message">
                                                No audit log entries are available.
                                            </p>
                                        ) : (
                                            <div className="audit-list">
                                                {auditLogs.map((log) => (
                                                    <div
                                                        className="audit-item"
                                                        key={log.id}
                                                    >
                                                        <div>
                                                            <strong>{log.action}</strong>

                                                            <p>{log.details}</p>
                                                        </div>

                                                        <span>
                              {formatDateTime(
                                  log.createdAt
                              )}
                            </span>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </section>
                                </>
                            )}
                    </>
                )}
            </>
        );
    };

    return (
        <div className="app-container">
            <header className="topbar">
                <div className="brand">RecoverAI</div>

                <div className="user-info">
                    AI Revenue Recovery
                </div>
            </header>

            <div className="dashboard-layout">
                <aside className="sidebar">
                    <nav>
                        <button
                            className={
                                activeView === "dashboard"
                                    ? "active"
                                    : ""
                            }
                            onClick={() =>
                                setActiveView("dashboard")
                            }
                        >
                            Dashboard
                        </button>

                        <button
                            className={
                                activeView === "attempts"
                                    ? "active"
                                    : ""
                            }
                            onClick={() =>
                                setActiveView("attempts")
                            }
                        >
                            Recovery Attempts
                        </button>

                        <button
                            className={
                                activeView === "details"
                                    ? "active"
                                    : ""
                            }
                            onClick={() =>
                                setActiveView("details")
                            }
                        >
                            Payment Details
                        </button>

                        <button
                            className={
                                activeView === "analytics"
                                    ? "active"
                                    : ""
                            }
                            onClick={() =>
                                setActiveView("analytics")
                            }
                        >
                            Analytics
                        </button>
                    </nav>
                </aside>

                <main className="main-content">
                    {activeView === "dashboard" &&
                        renderDashboard()}

                    {activeView === "attempts" &&
                        renderRecoveryAttempts()}

                    {activeView === "details" &&
                        renderPaymentDetails()}

                    {activeView === "analytics" &&
                        renderAnalytics()}
                </main>
            </div>
        </div>
    );
}

export default App;
