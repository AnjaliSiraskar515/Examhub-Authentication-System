import React, { useState, useRef, useCallback } from "react";
import axios from "axios";

// ─────────────────────────────────────────────────────────────────────────────
// Inline styles (no external CSS dependency)
// ─────────────────────────────────────────────────────────────────────────────
const styles = {
    page: {
        minHeight: "100vh",
        background: "linear-gradient(135deg, #0f0c29, #302b63, #24243e)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "'Segoe UI', Roboto, sans-serif",
        padding: "2rem",
    },
    card: {
        background: "rgba(255,255,255,0.05)",
        backdropFilter: "blur(16px)",
        borderRadius: "1.5rem",
        border: "1px solid rgba(255,255,255,0.12)",
        padding: "2.5rem",
        width: "100%",
        maxWidth: "720px",
        color: "#ffffff",
        boxShadow: "0 25px 60px rgba(0,0,0,0.5)",
    },
    title: {
        fontSize: "1.75rem",
        fontWeight: 700,
        textAlign: "center",
        marginBottom: "2rem",
        background: "linear-gradient(90deg, #a78bfa, #60a5fa)",
        WebkitBackgroundClip: "text",
        WebkitTextFillColor: "transparent",
    },
    section: {
        marginBottom: "1.5rem",
    },
    label: {
        display: "block",
        marginBottom: "0.5rem",
        fontSize: "0.9rem",
        color: "#c4b5fd",
        fontWeight: 600,
        letterSpacing: "0.04em",
        textTransform: "uppercase",
    },
    fileInput: {
        width: "100%",
        padding: "0.6rem 0.8rem",
        borderRadius: "0.6rem",
        border: "1px solid rgba(167,139,250,0.4)",
        background: "rgba(255,255,255,0.07)",
        color: "#e0e0e0",
        cursor: "pointer",
        fontSize: "0.9rem",
    },
    previewImg: {
        marginTop: "0.75rem",
        width: "100%",
        maxHeight: "180px",
        objectFit: "cover",
        borderRadius: "0.8rem",
        border: "1px solid rgba(167,139,250,0.3)",
    },
    webcamArea: {
        position: "relative",
        marginTop: "0.5rem",
        borderRadius: "0.8rem",
        overflow: "hidden",
        border: "1px solid rgba(96,165,250,0.3)",
        background: "#0f0c29",
        minHeight: "180px",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
    },
    video: {
        width: "100%",
        display: "block",
        borderRadius: "0.8rem",
    },
    capturedImg: {
        width: "100%",
        display: "block",
        borderRadius: "0.8rem",
    },
    placeholderText: {
        color: "rgba(255,255,255,0.25)",
        fontSize: "0.9rem",
    },
    buttonRow: {
        display: "flex",
        gap: "0.75rem",
        flexWrap: "wrap",
        marginTop: "0.75rem",
    },
    btnPrimary: {
        flex: 1,
        padding: "0.65rem 1.2rem",
        borderRadius: "0.6rem",
        border: "none",
        background: "linear-gradient(135deg, #7c3aed, #2563eb)",
        color: "#fff",
        fontWeight: 600,
        cursor: "pointer",
        fontSize: "0.9rem",
        transition: "opacity 0.2s",
    },
    btnSecondary: {
        flex: 1,
        padding: "0.65rem 1.2rem",
        borderRadius: "0.6rem",
        border: "1px solid rgba(96,165,250,0.5)",
        background: "rgba(96,165,250,0.1)",
        color: "#93c5fd",
        fontWeight: 600,
        cursor: "pointer",
        fontSize: "0.9rem",
        transition: "opacity 0.2s",
    },
    btnVerify: {
        width: "100%",
        padding: "0.85rem",
        borderRadius: "0.8rem",
        border: "none",
        background: "linear-gradient(135deg, #059669, #0284c7)",
        color: "#fff",
        fontWeight: 700,
        fontSize: "1rem",
        cursor: "pointer",
        marginTop: "0.5rem",
        transition: "opacity 0.2s, transform 0.1s",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "0.5rem",
    },
    btnDisabled: {
        opacity: 0.45,
        cursor: "not-allowed",
    },
    divider: {
        borderColor: "rgba(255,255,255,0.1)",
        margin: "1.75rem 0",
    },
    resultBox: {
        borderRadius: "0.8rem",
        padding: "1.25rem 1.5rem",
        display: "flex",
        flexDirection: "column",
        gap: "0.4rem",
    },
    resultSuccess: {
        background: "rgba(16,185,129,0.15)",
        border: "1px solid rgba(16,185,129,0.4)",
        color: "#6ee7b7",
    },
    resultFail: {
        background: "rgba(239,68,68,0.15)",
        border: "1px solid rgba(239,68,68,0.4)",
        color: "#fca5a5",
    },
    resultTitle: {
        fontWeight: 700,
        fontSize: "1rem",
        marginBottom: "0.2rem",
    },
    resultMeta: {
        fontSize: "0.88rem",
        opacity: 0.85,
    },
    spinner: {
        width: "18px",
        height: "18px",
        border: "3px solid rgba(255,255,255,0.3)",
        borderTop: "3px solid #fff",
        borderRadius: "50%",
        animation: "spin 0.75s linear infinite",
        display: "inline-block",
    },
};

// Inject keyframe animation for spinner
const spinnerKeyframes = `@keyframes spin { to { transform: rotate(360deg); } }`;
if (typeof document !== "undefined") {
    const styleTag = document.createElement("style");
    styleTag.innerHTML = spinnerKeyframes;
    document.head.appendChild(styleTag);
}

// ─────────────────────────────────────────────────────────────────────────────
// Component
// ─────────────────────────────────────────────────────────────────────────────
const PROFILE_ID = 1; // Static profile ID (test mode — no auth)
const API_URL = "http://localhost:8081/api/student-profile/verify";

const StudentFaceVerification = () => {
    // State
    const [idCardBase64, setIdCardBase64] = useState(null);
    const [idCardPreview, setIdCardPreview] = useState(null);
    const [liveBase64, setLiveBase64] = useState(null);
    const [livePreview, setLivePreview] = useState(null);
    const [webcamActive, setWebcamActive] = useState(false);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null); // { verified, confidence, message, error }

    // Refs
    const videoRef = useRef(null);
    const canvasRef = useRef(null);
    const streamRef = useRef(null);

    // ── ID Card Upload ──────────────────────────────────────────────────────────
    const handleIdCardChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onloadend = () => {
            const dataUrl = reader.result; // full data URL (includes prefix)
            setIdCardPreview(dataUrl);
            setIdCardBase64(dataUrl); // send full data URL; backend strips prefix
        };
        reader.readAsDataURL(file);
    };

    // ── Webcam ──────────────────────────────────────────────────────────────────
    const startWebcam = useCallback(async () => {
        try {
            setLiveBase64(null);
            setLivePreview(null);
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: "user" },
                audio: false,
            });
            streamRef.current = stream;
            setWebcamActive(true);
            // Assign stream to video element after state update
            setTimeout(() => {
                if (videoRef.current) {
                    videoRef.current.srcObject = stream;
                    videoRef.current.play();
                }
            }, 50);
        } catch (err) {
            alert("Could not access webcam: " + err.message);
        }
    }, []);

    const stopWebcam = useCallback(() => {
        if (streamRef.current) {
            streamRef.current.getTracks().forEach((t) => t.stop());
            streamRef.current = null;
        }
        setWebcamActive(false);
    }, []);

    const captureFrame = useCallback(() => {
        const video = videoRef.current;
        const canvas = canvasRef.current;
        if (!video || !canvas) return;

        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        const dataUrl = canvas.toDataURL("image/jpeg", 0.9);
        setLiveBase64(dataUrl);
        setLivePreview(dataUrl);
        stopWebcam(); // stop stream after capture
    }, [stopWebcam]);

    // ── Verify ──────────────────────────────────────────────────────────────────
    const handleVerify = async () => {
        if (!idCardBase64 || !liveBase64) {
            alert("Please upload an ID card image and capture a live photo first.");
            return;
        }
        setLoading(true);
        setResult(null);
        try {
            const response = await axios.post(API_URL, {
                profileId: PROFILE_ID,
                idCardImage: idCardBase64,
                liveImage: liveBase64,
            });
            setResult(response.data);
        } catch (err) {
            const errorMsg =
                err.response?.data?.error || err.message || "Unexpected error";
            setResult({ verified: false, error: errorMsg, message: errorMsg });
        } finally {
            setLoading(false);
        }
    };

    // ── Render ──────────────────────────────────────────────────────────────────
    const canVerify = !!idCardBase64 && !!liveBase64 && !loading;

    return (
        <div style={styles.page}>
            <div style={styles.card}>
                {/* Title */}
                <h1 style={styles.title}>Student Face Verification</h1>

                {/* ── ID Card Section ── */}
                <div style={styles.section}>
                    <label style={styles.label}>📄 Upload ID Card Image</label>
                    <input
                        type="file"
                        accept="image/*"
                        style={styles.fileInput}
                        onChange={handleIdCardChange}
                    />
                    {idCardPreview && (
                        <img src={idCardPreview} alt="ID Card Preview" style={styles.previewImg} />
                    )}
                </div>

                <hr style={styles.divider} />

                {/* ── Webcam Section ── */}
                <div style={styles.section}>
                    <label style={styles.label}>📷 Live Webcam Capture</label>

                    <div style={styles.webcamArea}>
                        {webcamActive && (
                            // eslint-disable-next-line jsx-a11y/media-has-caption
                            <video ref={videoRef} style={styles.video} autoPlay playsInline />
                        )}
                        {!webcamActive && livePreview && (
                            <img src={livePreview} alt="Captured" style={styles.capturedImg} />
                        )}
                        {!webcamActive && !livePreview && (
                            <span style={styles.placeholderText}>Webcam preview will appear here</span>
                        )}
                    </div>

                    {/* Hidden canvas for frame capture */}
                    <canvas ref={canvasRef} style={{ display: "none" }} />

                    <div style={styles.buttonRow}>
                        {!webcamActive ? (
                            <button style={styles.btnSecondary} onClick={startWebcam}>
                                🎥 Open Webcam
                            </button>
                        ) : (
                            <>
                                <button style={styles.btnPrimary} onClick={captureFrame}>
                                    📸 Capture Photo
                                </button>
                                <button style={styles.btnSecondary} onClick={stopWebcam}>
                                    ✖ Cancel
                                </button>
                            </>
                        )}
                        {livePreview && !webcamActive && (
                            <button style={styles.btnSecondary} onClick={startWebcam}>
                                🔄 Retake
                            </button>
                        )}
                    </div>
                </div>

                <hr style={styles.divider} />

                {/* ── Verify Button ── */}
                <button
                    style={{
                        ...styles.btnVerify,
                        ...(canVerify ? {} : styles.btnDisabled),
                    }}
                    onClick={handleVerify}
                    disabled={!canVerify}
                >
                    {loading ? (
                        <>
                            <span style={styles.spinner} />
                            Verifying...
                        </>
                    ) : (
                        "✅ Verify Identity"
                    )}
                </button>

                {/* ── Result Display ── */}
                {result && (
                    <div
                        style={{
                            ...styles.resultBox,
                            ...(result.verified ? styles.resultSuccess : styles.resultFail),
                            marginTop: "1.5rem",
                        }}
                    >
                        <p style={styles.resultTitle}>
                            {result.verified ? "✅ Verification Successful" : "❌ Verification Failed"}
                        </p>
                        {result.message && (
                            <p style={styles.resultMeta}>
                                <strong>Message:</strong> {result.message}
                            </p>
                        )}
                        {result.confidence !== undefined && result.confidence !== null && (
                            <p style={styles.resultMeta}>
                                <strong>Confidence:</strong>{" "}
                                {typeof result.confidence === "number"
                                    ? result.confidence.toFixed(2) + "%"
                                    : result.confidence}
                            </p>
                        )}
                        {result.error && !result.message && (
                            <p style={styles.resultMeta}>
                                <strong>Error:</strong> {result.error}
                            </p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default StudentFaceVerification;
