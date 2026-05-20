from flask import Flask, request, jsonify, Response
from PIL import Image
import pytesseract
import io

app = Flask(__name__)

LANGUAGE_MAP = {
    "english": "eng",
    "sinhala": "sin",
    "tamil": "tam"
}


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

    language = LANGUAGE_MAP.get(language.strip().lower(), language.strip().lower())

    try:
        text = pytesseract.image_to_string(image, lang=language)
    except Exception as exc:
        return jsonify({"error": f"OCR extraction failed: {exc}"}), 500

    return Response(text, mimetype="text/plain; charset=utf-8")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
