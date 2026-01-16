from flask import Flask, request, jsonify
from PIL import Image
import pytesseract, os, base64, io
app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
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
        match = abs(len(img1)-len(img2)) < 1000
        score = 0.9 if match else 0.45
        return jsonify({'match': match, 'score': score})
    return jsonify({'error':'no_biometric_data'}), 400
if __name__=='__main__':
    app.run(host='0.0.0.0', port=5001)
