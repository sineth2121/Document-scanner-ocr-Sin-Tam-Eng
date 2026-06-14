from gtts import gTTS
import os
import sys



# Read text from file
try:
    with open("detectedtext.txt", "r", encoding="utf-8") as file:
        text = file.read()

except FileNotFoundError:
    print("Error: detectedtext.txt file not found!")
    exit()

# Get language from command line argument, default to "en"
language = sys.argv[1] if len(sys.argv) > 1 else "en"

# Switch statement 
match language.lower():

    case "en" | "english":
        tts = gTTS(text=text, lang='en')
        tts.save("speech.mp3")
        print("English speech generated.")

    case "si" | "sinhala":
        tts = gTTS(text=text, lang='si')
        tts.save("speech.mp3")
        print("Sinhala speech generated.")

    case "ta" | "tamil":
        tts = gTTS(text=text, lang='ta')
        tts.save("speech.mp3")
        print("Tamil speech generated.")

    case _:
        print("Invalid language code!")
        exit()

# Play the audio file
os.system("start speech.mp3")