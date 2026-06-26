import asyncio
import edge_tts

async def main():
    try:
        communicate = edge_tts.Communicate("Hello, this is a test of Microsoft Edge Text to Speech voiceover.", "en-US-AvaNeural")
        await communicate.save("test_voiceover.mp3")
        print("SUCCESS")
    except Exception as e:
        print("FAILED:", e)

if __name__ == "__main__":
    asyncio.run(main())
