import base64
import os
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
import json

# Same key as in application.properties
AES_KEY_B64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="

def encrypt_token(student_id, exam_id):
    key = base64.b64decode(AES_KEY_B64)
    payload = json.dumps({
        "studentId": student_id,
        "examId": exam_id,
        "sid": student_id, 
        "eid": exam_id,
        "seat": "A-1"
    })
    
    iv = get_random_bytes(12)
    cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
    ciphertext, tag = cipher.encrypt_and_digest(payload.encode('utf-8'))
    
    # Format: IV + Ciphertext (Tag is usually appended in Java default GCM, let's check Java logic)
    # My Java AESGcmUtil does: IV + Ciphertext (It relies on Java GCM internals which handle tag automatically? 
    # Wait, Java GCM `doFinal` returns Ciphertext + Tag appeneded.
    # Python `encrypt_and_digest` returns separate tag.
    
    final_bytes = iv + ciphertext + tag
    return base64.urlsafe_b64encode(final_bytes).decode('utf-8')

print(encrypt_token(2, 1))
