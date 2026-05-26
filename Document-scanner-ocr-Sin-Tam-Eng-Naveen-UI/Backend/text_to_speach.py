from gtts import gTTS
import os



# Read text from file
try:
    with open("detectedtext.txt", "r", encoding="utf-8") as file:
        text = file.read()

except FileNotFoundError:
    print("Error: detectedtext.txt file not found!")
    exit()

# Switch statement 
match language:

    case "en":
        tts = gTTS(text=text, lang='en')
        tts.save("speech.mp3")
        print("English speech generated.")

    case "si":
        tts = gTTS(text=text, lang='si')
        tts.save("speech.mp3")
        print("Sinhala speech generated.")

    case "ta":
        tts = gTTS(text=text, lang='ta')
        tts.save("speech.mp3")
        print("Tamil speech generated.")

    case _:
        print("Invalid language code!")
        exit()

# Play the audio file
os.system("start speech.mp3")