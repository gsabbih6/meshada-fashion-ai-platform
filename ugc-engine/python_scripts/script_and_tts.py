"""
DEPRECATED — This file is no longer used.

Script generation is now handled inline in orchestrator.py.
TTS voiceover was handled by ElevenLabs which required:
  - ELEVENLABS_API_KEY
  - OPENAI_API_KEY (for script generation)

Higgsfield's pipeline doesn't require separate TTS — the video
itself serves as the content. Scripts are used as metadata/captions.
"""

# Kept for reference only. See orchestrator.py for the active pipeline.
