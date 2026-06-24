from flask import Flask, request, jsonify, Response
from PIL import Image
import pytesseract
import os
from pathlib import Path

app = Flask(__name__)

# ----------------------------
# BASE PATH (project safe)
# ----------------------------
BASE_DIR = Path(__file__).resolve().parent

# ----------------------------
# Tesseract ENGINE (portable)
# ----------------------------
TESSERACT_CANDIDATES = [
    BASE_DIR / "Teseract_Ocr_Data" / "tesseract.exe",
    BASE_DIR / "model" / "tesseract.exe",
]

for candidate in TESSERACT_CANDIDATES:
    if candidate.is_file():
        pytesseract.pytesseract.tesseract_cmd = str(candidate)
        break
else:
    raise FileNotFoundError(
        "Unable to locate tesseract.exe. Expected it under Backend/Teseract_Ocr_Data or Backend/model."
    )

# ----------------------------
# TESSDATA CONFIG (languages)
# ----------------------------
TESSDATA_DIR = TESSERACT_CANDIDATES[0].parent / "tessdata"
if not TESSDATA_DIR.is_dir():
    legacy_tessdata_dir = BASE_DIR / "model" / "tessdata"
    if legacy_tessdata_dir.is_dir():
        TESSDATA_DIR = legacy_tessdata_dir
os.environ["TESSDATA_PREFIX"] = str(TESSDATA_DIR)

# ----------------------------
# LANGUAGE MAP
# ----------------------------
LANGUAGE_MAP = {
    "english": "eng",
    "sinhala": "sin",
    "tamil": "tam",
    "mixed": "eng+sin+tam"
}

# ----------------------------
# OCR ENDPOINT
# ----------------------------
@app.route("/api/ocr", methods=["POST"])
def ocr():
    image_file = request.files.get("image")
    language = request.form.get("language", "eng")

    if image_file is None:
        return jsonify({"error": "Missing image file"}), 400

    try:
        image = Image.open(image_file.stream).convert("RGB")
    except Exception as exc:
        return jsonify({"error": f"Unable to read image: {exc}"}), 400

    # normalize language input
    language = LANGUAGE_MAP.get(language.strip().lower(), language.strip().lower())

    try:
        text = pytesseract.image_to_string(
            image,
            lang=language
        )
    except Exception as exc:
        return jsonify({"error": f"OCR extraction failed: {exc}"}), 500

    return Response(text, mimetype="text/plain; charset=utf-8")


# ----------------------------
# TTS ENDPOINT
# ----------------------------
@app.route("/api/tts", methods=["POST"])
def tts():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "Missing 'text' parameter"}), 400

    text = data["text"]
    language = data.get("language", "english")

    # Map frontend languages to gTTS language codes
    lang_map = {
        "sinhala": "si",
        "english": "en",
        "tamil": "ta",
        "mixed": "en"
    }
    gtts_lang = lang_map.get(language.strip().lower(), "en")

    try:
        from gtts import gTTS
        import io
        tts_obj = gTTS(text=text, lang=gtts_lang)
        fp = io.BytesIO()
        tts_obj.write_to_fp(fp)
        fp.seek(0)
        return Response(fp.read(), mimetype="audio/mpeg")
    except Exception as exc:
        return jsonify({"error": f"TTS failed: {exc}"}), 500


# ----------------------------
# HEALTH CHECK
# ----------------------------
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


# ----------------------------
# START SERVER
# ----------------------------
if __name__ == "__main__":
    # optional debug test
    print("Tesseract path:", pytesseract.pytesseract.tesseract_cmd)
    print("Tessdata dir:", TESSDATA_DIR)

    app.run(host="127.0.0.1", port=5000)