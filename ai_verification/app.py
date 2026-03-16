from flask import Flask, request, jsonify
from PIL import Image
import pytesseract, os, base64, io
from deepface import DeepFace
import numpy as np
import cv2

app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Helper function to decode base64
def process_base64_image(b64_str):
    if "," in b64_str:
        b64_str = b64_str.split(",")[1]
    img_data = base64.b64decode(b64_str)
    # Convert to PIL Image
    pil_image = Image.open(io.BytesIO(img_data)).convert('RGB')
    # Convert to OpenCV format (numpy array)
    open_cv_image = np.array(pil_image)
    # Convert RGB to BGR as DeepFace uses OpenCV backend internally
    open_cv_image = open_cv_image[:, :, ::-1].copy() 
    return open_cv_image

@app.route('/verify_document', methods=['POST'])
def verify_document():
    if 'document' not in request.files:
        return jsonify({'error':'no file'}),400
    f = request.files['document']; path = os.path.join(UPLOAD_FOLDER, f.filename); f.save(path)
    try:
        text = pytesseract.image_to_string(Image.open(path))
    except Exception as e:
        text = ''
    status='flagged'; confidence=0.45
    if 'name' in text.lower() or len(text)>20:
        status='verified'; confidence=0.9
    return jsonify({'ocr_text': text, 'status': status, 'confidence': confidence})

@app.route('/biometric_verify', methods=['POST'])
def biometric_verify():
    payload = request.get_json() or {}
    if 'fingerprint_template' in payload:
        fp = payload.get('fingerprint_template','')
        match = 'SAMPLE' in fp
        score = 0.95 if match else 0.1
        return jsonify({'match': match, 'score': score})
    img1 = payload.get('img1'); img2 = payload.get('img2')
    if img1 and img2:
        try:
             # Basic length check kept for backward compatibility if needed, 
             # but ideally this should use DeepFace too if it was real.
             # Keeping mock logic as per instructions "Do not touch other endpoints".
            match = abs(len(img1)-len(img2)) < 1000
            score = 0.9 if match else 0.45
            return jsonify({'match': match, 'score': score})
        except:
            return jsonify({'error': 'processing error'}), 500
    return jsonify({'error':'no_biometric_data'}), 400

@app.route('/face_verify', methods=['POST'])
def face_verify():
    try:
        data = request.get_json()
        if not data:
             return jsonify({"error": "No JSON data provided"}), 400

        stored_b64 = data.get("storedImage")
        live_b64 = data.get("liveImage")

        if not stored_b64 or not live_b64:
             return jsonify({"error": "Missing image data"}), 400

        # Process images
        img1 = process_base64_image(stored_b64)
        img2 = process_base64_image(live_b64)

        # Verify using DeepFace (Facenet512 improves accuracy on face embeddings)
        result = DeepFace.verify(
            img1_path=img1,
            img2_path=img2,
            model_name="Facenet512",
            detector_backend="retinaface",
            enforce_detection=True
        )

        distance = result["distance"]

        # Manual threshold keeps verification stable across model updates
        THRESHOLD = 0.35
        verified = distance < THRESHOLD

        # Calculate confidence
        confidence = max(0, (1 - distance) * 100)

        message = "Face matched" if verified else "Face not matched"

        return jsonify({
            "verified": bool(verified),
            "confidence": float(round(confidence, 2)),
            "message": message
        })

    except Exception as e:
        return jsonify({"error": str(e), "verified": False, "confidence": 0.0, "message": "Error processing verification"}), 500

if __name__=='__main__':
    app.run(host='0.0.0.0', port=5001)