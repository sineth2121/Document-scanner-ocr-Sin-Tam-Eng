import argparse
from datetime import datetime
from pathlib import Path
import os
import pytesseract
from PIL import Image

# Config portable tesseract
BASE_DIR = Path(__file__).resolve().parent
TESSERACT_CANDIDATES = [
    BASE_DIR / "Teseract_Ocr_Data" / "tesseract.exe",
    BASE_DIR / "model" / "tesseract.exe",
]

for candidate in TESSERACT_CANDIDATES:
    if candidate.is_file():
        pytesseract.pytesseract.tesseract_cmd = str(candidate)
        os.environ["TESSDATA_PREFIX"] = str(candidate.parent / "tessdata")
        break
else:
    raise FileNotFoundError(
        "Unable to locate tesseract.exe. Expected it under Backend/Teseract_Ocr_Data or Backend/model."
    )

def extract_text_from_image(image: Image.Image, language: str) -> str:
    return pytesseract.image_to_string(image, lang=language)


def normalize_language(language: str) -> str:
    selected = language.strip().lower()
    return {
        "english": "eng",
        "sinhala": "sin",
        "tamil": "tam",
        "mixed": "eng+sin+tam",
    }.get(selected, selected)

def temp_display(text: str) -> None:
    print("Extracted Text:\n")
    print("-" * 20)
    print(text)
    print("-" * 20)

def write_output(text: str, source_path: Path, output_dir: Path) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_name = f"{source_path.stem}_{timestamp}.txt"
    output_path = output_dir / output_name
    output_path.write_text(text, encoding="utf-8")
    return output_path

def main() -> None:
    # --- SETUP TERMINAL COMMANDS ---
    parser = argparse.ArgumentParser(description="Scan an image and extract text using OCR.")
    
    # Require the user to provide the image name
    parser.add_argument("image_name", help="Name of the image inside the 'samples' folder (e.g., sample_tam.png)")
    
    # Require the user to provide the language
    parser.add_argument("language", help="Language code to use (e.g., eng, tam, sin_best)")
    
    # Read what the user typed in the terminal
    args = parser.parse_args()
    # -------------------------------

    # Path to sample image (using the name provided in the terminal)
    image_path = BASE_DIR / "samples" / args.image_name

    if not image_path.is_file():
        raise FileNotFoundError(f"Image not found at: {image_path}")

    # Load image
    img = Image.open(image_path)

    # Perform OCR using the language provided in the terminal
    print(f"Scanning '{args.image_name}' for language '{args.language}'...")
    text = extract_text_from_image(img, normalize_language(args.language))

    temp_display(text)

    output_dir = BASE_DIR / "testdata"
    output_file = write_output(text, image_path, output_dir)
    print(f"Saved results to: {output_file.name}")

if __name__ == "__main__":
    main()