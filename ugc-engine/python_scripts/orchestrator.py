"""
Meshada Fashion UGC Pipeline v2 — Production-Grade Fashion Video Generation.

Implements the proper 6-step fashion video creation process:
  1. Virtual Try-On (VTON) — real product on AI model
  2. Fashion Photography Prompting — structured 20-line prompts
  3. Storyboard Planning — 9-panel grid with timing
  4. Video Generation — task-specific model selection
  5. Character Consistency — persona sheets as reference
  6. UGC Assembly — final video with metadata

Supports:
  - Higgsfield (Soul + DoP) — text-to-image + image-to-video
  - EachLabs (Kolors VTON + Pixverse) — virtual try-on + runway video
  - Fallback mock data when no API keys are set

Setup:
  pip install higgsfield-client python-dotenv requests
"""

import os
import json
import argparse
import traceback
import time
import subprocess
import shutil
import random

from dotenv import load_dotenv
load_dotenv()

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def get_absolute_path(relative_path: str) -> str:
    """Resolve a path relative to the script's directory, ensuring it is absolute."""
    return os.path.abspath(os.path.join(SCRIPT_DIR, relative_path))

# Dynamically resolve FFmpeg binary path
FFMPEG_BIN = os.getenv("FFMPEG_PATH") or shutil.which("ffmpeg") or "ffmpeg"

try:
    import higgsfield_client
    HAS_HIGGSFIELD = True
except ImportError:
    HAS_HIGGSFIELD = False

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False


# ─── Model Configuration ───────────────────────────────────────────────────────

# Higgsfield models
HF_IMAGE_MODEL = "higgsfield-ai/soul/standard"
HF_VIDEO_MODEL = "higgsfield-ai/dop/standard"

# EachLabs models
EACHLABS_VTON_MODEL = "kling-v1-5-kolors-virtual-try-on"
EACHLABS_VIDEO_MODEL = "pixverse-v5-6-image-to-video"
EACHLABS_MODELSHOOT = "product-photo-to-modelshoot"
EACHLABS_IMG2IMG_MODEL = "nano-banana-2-edit"
EACHLABS_BASE_URL = "https://api.eachlabs.ai/v1"


# ─── Character Personas ────────────────────────────────────────────────────────

AI_MODELS = {
    "model_1": {
        "name": "Aria",
        "style_direction": "editorial",
        "human_image_url": "https://d3u0tzju9qaucj.cloudfront.net/0004fdf7-03ab-4d48-a4db-d8b39d40b6f2/da094f88-abd8-45f1-9bc1-f6d7a5a901bd.png",
        "demographics": {
            "origin": "Mediterranean European (Italian/Spanish features)",
            "gender": "Female",
            "age": "26-28 years",
            "body_type": "Slim, elegant proportions, model stature",
            "height": "5'9\", commanding runway presence",
        },
        "appearance": {
            "skin_tone": "Light olive with warm golden undertones, natural healthy glow",
            "hair": "Dark brunette, long sleek hair with natural movement, center-parted",
            "features": "Defined cheekbones, strong jawline, almond eyes, refined angular beauty",
            "expression": "Serene confidence, contemplative intensity, editorial mid-breath poise",
            "makeup": "Elevated natural — subtle luminous foundation, defined brows, smoky-neutral eye, nude-rose lip",
        },
        "pose": (
            "Asymmetrical weight shift with left hip out, right hand adjusting garment collar. "
            "Head at three-quarter angle turned slightly left, gaze directed just past camera. "
            "Shoulders angled creating dynamic diagonal line. Left arm relaxed at side. "
            "Implies paused motion, as if caught mid-stride on a runway."
        ),
        "environment": "Modern minimalist studio with textured warm plaster wall, soft diffused window light creating gentle gradient",
        "photography": {
            "camera": "Medium format digital",
            "lens": "85mm portrait lens",
            "aperture": "f/2.8 for subject isolation with soft background",
            "lighting": "Dramatic directional key light from left at 45 degrees, subtle fill from right, creating dimension and contouring",
            "composition": "Full body shot, model fills 65% of frame, positioned on right third",
            "angle": "Eye-level, slight low angle adding authority",
        },
        "mood": "Sophisticated, confident, refined, editorial luxury, serene power, timeless elegance",
        "voice_style": "warm, sophisticated, editorial",
        "motion_prompt": "Fashion model strikes editorial pose then slowly turns, dramatic lighting catches silhouette, smooth cinematic camera movement, professional fashion film aesthetic",
    },
    "model_2": {
        "name": "Luna",
        "style_direction": "streetwear",
        "human_image_url": "https://d3u0tzju9qaucj.cloudfront.net/0004fdf7-03ab-4d48-a4db-d8b39d40b6f2/f03eb6f0-1bdd-430e-9b73-550945ae8e42.png",
        "demographics": {
            "origin": "African American",
            "gender": "Female",
            "age": "22-24 years",
            "body_type": "Athletic, strong build with defined shoulders",
            "height": "5'8\", confident urban stance",
        },
        "appearance": {
            "skin_tone": "Deep brown with warm undertones, natural healthy skin, visible texture",
            "hair": "Natural textured curls, shoulder-length, full volume and movement",
            "features": "Strong features, natural brows, clear skin, authentic beauty",
            "expression": "Confident direct gaze with subtle attitude, genuine self-assurance",
            "makeup": "Minimal — natural brows, clear skin, bold lip color accent",
        },
        "pose": (
            "Mid-stride walking towards camera, left foot forward, right arm swinging naturally. "
            "Dynamic movement captured mid-motion creating energy. Head slightly tilted, "
            "direct eye contact with camera. Asymmetrical stance with weight shifting forward. "
            "Hands emerging from oversized sleeves, casual confidence."
        ),
        "environment": "Urban street setting with graffiti wall and neon signs, natural overcast daylight creating even soft lighting",
        "photography": {
            "camera": "Professional DSLR, street photography style",
            "lens": "35mm for environmental context",
            "aperture": "f/4 for subject focus with urban background context",
            "lighting": "Natural overcast daylight, even soft illumination, subtle neon color spill",
            "composition": "Medium-full shot, model fills 70% of frame, dynamic diagonal",
            "angle": "Eye-level, straight-on with slight tilt for energy",
        },
        "mood": "Urban, energetic, authentic street style, confident, unapologetic, raw energy, contemporary",
        "voice_style": "energetic, casual, trendy",
        "motion_prompt": "Fashion model walks confidently toward camera on urban street, dynamic stride, natural arm swing, street style energy, camera follows with slight handheld movement",
    },
    "model_3": {
        "name": "Nova",
        "style_direction": "authentic_ugc",
        "human_image_url": "https://d3u0tzju9qaucj.cloudfront.net/0004fdf7-03ab-4d48-a4db-d8b39d40b6f2/6df52784-0ac1-421c-b558-534a4dadb85d.png",
        "demographics": {
            "origin": "East Asian (Korean features)",
            "gender": "Female",
            "age": "23-25 years",
            "body_type": "Petite, approachable proportions",
            "height": "5'5\", friendly presence",
        },
        "appearance": {
            "skin_tone": "Fair with neutral-cool undertones, dewy fresh complexion",
            "hair": "Dark brown-black, soft waves, side-parted, messy bun",
            "features": "Soft rounded features, bright eyes, natural smile lines",
            "expression": "Excited genuine smile, talking directly to camera, expressive eyebrows",
            "makeup": "No-makeup makeup look, slightly glossy lips, natural glow",
        },
        "pose": (
            "Selfie-style pose, left arm extended as if holding the phone camera. "
            "Right hand gesturing animatedly while holding the garment to show it off. "
            "Leaning slightly forward towards the lens, intimate and energetic."
        ),
        "environment": "Slightly messy Gen-Z bedroom, unmade bed in background, LED strip lights, clothing rack, cozy and lived-in aesthetic",
        "photography": {
            "camera": "iPhone 15 Pro front-facing camera",
            "lens": "Wide angle selfie perspective",
            "aperture": "f/1.8 deep depth of field (smartphone style)",
            "lighting": "Ring light reflecting in eyes, mixed with natural window light",
            "composition": "Close-up selfie framing, slight distortion on edges, vertical 9:16",
            "angle": "Slightly high angle, pointing down at model (classic selfie angle)",
        },
        "mood": "Authentic, raw, excited, viral TikTok haul energy, relatable, unfiltered",
        "voice_style": "fast-talking, excited, casual",
        "motion_prompt": "UGC TikTok haul video, young woman holding phone in selfie mode in her messy bedroom, talking excitedly to camera and gesturing with hands, ring light reflection, authentic smartphone video aesthetic",
    },
    "model_4": {
        "name": "Sasha",
        "style_direction": "curvy-chic",
        "human_image_url": "https://d3u0tzju9qaucj.cloudfront.net/0004fdf7-03ab-4d48-a4db-d8b39d40b6f2/9d1b5d99-3876-4c5b-815f-6caa44334fd7.png",
        "demographics": {
            "origin": "African American",
            "gender": "Female",
            "age": "24-26 years",
            "body_type": "Plus-size model, curvy full-figured proportions, volumetric chest",
            "height": "5'9\", confident modeling stance",
        },
        "appearance": {
            "skin_tone": "Deep brown with warm undertones, natural healthy skin, visible texture",
            "hair": "Natural textured curls, shoulder-length, full volume and movement",
            "features": "Strong beautiful features, natural brows, clear skin, authentic beauty",
            "expression": "Serene confidence, warm smile, self-assured modeling pose",
            "makeup": "Minimal — natural brows, clear skin, bold lip color accent",
        },
        "pose": (
            "Asymmetrical standing pose with right hand resting on hip, left arm relaxed. "
            "Slightly angled stance highlighting curves with confidence. "
            "Direct eye contact with the camera, warm smile, poised and elegant."
        ),
        "environment": "Modern minimalist studio with textured warm plaster wall, soft diffused window light creating gentle gradient",
        "photography": {
            "camera": "Medium format digital",
            "lens": "85mm portrait lens",
            "aperture": "f/2.8 for subject isolation with soft background",
            "lighting": "Dramatic directional key light from left at 45 degrees, subtle fill from right, creating dimension and contouring",
            "composition": "Full body shot, model centered, positioned on right third",
            "angle": "Eye-level, straight-on with slight tilt for energy",
        },
        "mood": "Sophisticated, confident, curvy chic, self-assured, elegant, contemporary fashion",
        "voice_style": "confident, warm, styling expert",
        "motion_prompt": "Plus-size fashion model turns slowly, showcasing her outfit with confidence and poise, elegant cinematic camera movement, professional fashion film",
    },
}


# ─── Category Detection & Scene Library ────────────────────────────────────────

def detect_product_category(product_name: str, product_description: str) -> str:
    text = f"{product_name} {product_description}".lower()
    if any(w in text for w in ["dress", "gown", "maxi", "midi", "skirt"]):
        return "dress"
    elif any(w in text for w in ["jacket", "coat", "hoodie", "cardigan", "blazer", "sweater", "outerwear"]):
        return "outerwear"
    elif any(w in text for w in ["pant", "jean", "trouser", "legging", "short"]):
        return "pants"
    elif any(w in text for w in ["shoe", "sneaker", "boot", "heel", "sandal", "footwear"]):
        return "shoes"
    elif any(w in text for w in ["bag", "handbag", "purse", "backpack", "accessory", "hat", "belt"]):
        return "accessories"
    else:
        return "tops" # Default to tops (t-shirt, shirt, blouse, etc.)

SCENE_TEMPLATES = {
    "streetwear": {
        "dress": [
            {"name": "Detail", "duration": 2, "motion_prompt": "extreme close-up of dress detail, slow pan highlighting fabric weave texture, photorealistic organic folds, handheld camera micro-drift, soft city lighting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "street style video, fashion model walking towards camera in busy city alley, dynamic confident stride, natural camera shake, ambient street reflections, realistic depth blur"},
            {"name": "Turn", "duration": 3, "motion_prompt": "fashion model spins gracefully on city pavement, dress flares out with realistic cloth physics, urban neon lights, cinematic slow motion, camera focus breathing"}
        ],
        "outerwear": [
            {"name": "Detail", "duration": 2, "motion_prompt": "close-up of jacket zipper and fabric texture, model adjusting collar naturally, handheld camera feel, raw concrete backdrop, natural lighting shadows"},
            {"name": "Walk", "duration": 3, "motion_prompt": "streetwear model struts confidently down wet city street, street style film, camera tracking backwards smoothly, subtle camera shake, reflections in puddles"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model pulls hood over curls, turns profile looking away, graffiti wall background, cinematic lighting, realistic camera drift, handheld camera"}
        ],
        "pants": [
            {"name": "Walk", "duration": 3, "motion_prompt": "low angle camera tracking model's walking stride, showing sneakers and loose pants in motion, organic fabric folds, handheld camera micro-drift, gritty street environment"},
            {"name": "Turn", "duration": 2, "motion_prompt": "model spins slowly, hands in pockets, looking over shoulder at camera with confident attitude, camera refocuses, natural daylight shadows"},
            {"name": "Pose", "duration": 3, "motion_prompt": "street style photoshoot, model strikes pose leaning against concrete pillar, direct confident gaze, handheld camera breathing, shallow depth-of-field"}
        ],
        "tops": [
            {"name": "Detail", "duration": 2, "motion_prompt": "close-up of graphic tee print and cotton texture, model adjusting sleeves, handheld camera micro-drift, urban streetwear vibe, natural lighting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "streetwear model walks casually on urban street, looking towards camera, warm street lamp glow, realistic lens flare, subtle handheld camera breathing"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model strikes a casual streetwear pose, smiles at camera, urban sunset background, film grain texture, handheld camera, natural depth-of-field"}
        ]
    },
    "editorial": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "slow cinematic tilt showing elegant dress hem to shoulders, soft luxury studio lighting, high fashion film, clean plaster wall backdrop, camera micro-drift"},
            {"name": "Walk", "duration": 3, "motion_prompt": "high fashion model walks gracefully on minimal studio runway, elegant posture, slow motion 60fps, wind machine blowing dress hem, realistic cloth physics"},
            {"name": "Reveal", "duration": 3, "motion_prompt": "editorial fashion pose, model turns slowly showing open back details of dress, serene confidence, dramatic key light, high contrast shadows"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "high fashion portrait, model wearing structured blazer, adjusts lapels elegantly, minimalist studio backdrop, soft diffused key lighting, camera breathing"},
            {"name": "Walk", "duration": 3, "motion_prompt": "editorial fashion film, model walks on runway, dramatic spotlight catches structured shoulders of coat, camera tracks back, subtle handheld drift"},
            {"name": "Pose", "duration": 3, "motion_prompt": "editorial pose, model turns slowly, dramatic key light, high contrast shadows, haute couture aesthetic, photorealistic fabric drape and texture"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "editorial shot, model strikes asymmetric pose highlighting clean lines of high-waist trousers, dramatic studio lighting, camera micro-drift"},
            {"name": "Walk", "duration": 3, "motion_prompt": "high fashion model walks with editorial posture, trousers drape beautifully in motion with realistic folds, studio wind machine, camera tracking"},
            {"name": "Pose", "duration": 3, "motion_prompt": "editorial model turns three-quarter, looks directly into camera lens with high-fashion intensity, soft focus background, luxury studio lighting"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "high fashion portrait, close-up of model's face and elegant silk blouse drape, soft diffused studio light, camera focus breathing, realistic silk folds"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walking slowly, silk shirt flowing in wind, wind machine, cinematic camera movement, editorial fashion film, camera micro-drift"},
            {"name": "Pose", "duration": 3, "motion_prompt": "editorial model turns slowly, cross-armed pose, looking at camera with serene expression, dramatic key lighting, photorealistic skin textures"}
        ]
    },
    "casual": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "vlog style video, model holding up dress, turning side to side with a warm friendly smile, cozy bedroom setting, natural window light, handheld camera feel"},
            {"name": "Walk", "duration": 3, "motion_prompt": "lifestyle vlog, model walks happily in sunny garden, dress swaying naturally, handheld camera shake, warm sun flares, natural depth blur"},
            {"name": "Outro", "duration": 3, "motion_prompt": "cozy lifestyle vlog, model sits on cafe bench, turns to camera, laughs and waves, bright inviting aesthetic, handheld camera breathing"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "cozy lifestyle vlog, model wearing warm cardigan, wraps arms around herself smiling, coffee shop background, soft golden hour light, handheld camera feel"},
            {"name": "Walk", "duration": 3, "motion_prompt": "lifestyle vlog, model walks down suburban path, autumn leaves falling, casual friendly energy, natural handheld camera movement, soft depth-of-field"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model turns, smiles and points at cardigan texture, warm cozy lighting, lifestyle vlog aesthetic, camera micro-drift, natural cloth weave"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "casual mirror selfie style video, model showing comfy jeans fit, tilting hips naturally, warm bedroom light, handheld camera shake, cozy atmosphere"},
            {"name": "Walk", "duration": 3, "motion_prompt": "lifestyle vlog, model walks towards camera in sunny park, smiling, casual approach, natural handheld camera drift, grass and trees bokeh"},
            {"name": "Pose", "duration": 3, "motion_prompt": "model leans against park bench, turns to camera and waves, happy friendly best-friend vibes, sunny day, handheld camera breathing"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "cozy vlog intro, model waves to camera wearing cute casual tee, holds up tea mug, bright cozy cafe, natural window light, handheld camera feel"},
            {"name": "Walk", "duration": 3, "motion_prompt": "casual model walks in cafe, turns around smiling, lifestyle vlog aesthetic, dewy skin glow, camera micro-drift, warm indoor lighting"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model laughs, waves goodbye to camera, cozy environment, soft golden hour lighting, handheld camera shake, realistic skin texture"}
        ]
    },
    "authentic_ugc": {
        "dress": [
            {"name": "Mirror Selfie Hook", "duration": 2, "motion_prompt": "UGC TikTok selfie video, young woman holding phone up high, showing off dress fit in her bedroom mirror, excited facial expression, authentic smartphone video"},
            {"name": "Bed Haul", "duration": 3, "motion_prompt": "UGC haul video, woman sitting on unmade bed, holding up the dress to the camera excitedly, pointing at fabric details, ring light lighting, raw iphone footage"},
            {"name": "Full Spin", "duration": 3, "motion_prompt": "TikTok try-on video, woman steps back in her bedroom to show full dress, does a quick casual spin, smiling and talking to camera, messy room background"},
            {"name": "Close Up Detail", "duration": 2, "motion_prompt": "UGC close up selfie video, woman showing the neckline of her dress, talking fast to the camera, messy room background, authentic TikTok style"},
            {"name": "Package Open", "duration": 3, "motion_prompt": "TikTok unboxing, woman sitting on the floor pulling a dress out of a package, holding it up and gasping excitedly, raw smartphone footage"},
            {"name": "Dance Fit", "duration": 3, "motion_prompt": "UGC try-on, woman doing a quick trendy TikTok dance in her bedroom wearing a new dress, laughing, casual relatable vibe"},
            {"name": "Sitting Pose", "duration": 3, "motion_prompt": "UGC casual video, woman sitting on a bedroom chair wearing a new dress, talking animatedly with hand gestures to the phone camera, relatable energy"},
            {"name": "Walking Hook", "duration": 2, "motion_prompt": "TikTok vlog, woman walking towards camera holding her phone, showing off how the dress flows when walking, happy expression"},
            {"name": "Pocket Check", "duration": 2, "motion_prompt": "UGC video, woman excitedly showing that her dress has pockets, putting her hands in the pockets and smiling at the camera"},
            {"name": "Coffee Run", "duration": 3, "motion_prompt": "TikTok lifestyle vlog, woman holding a coffee cup, taking a sip while showing off her dress fit in natural sunlight from her window"},
            {"name": "Back Detail", "duration": 3, "motion_prompt": "UGC mirror selfie, woman turning to show the back cut-out of her dress, looking over her shoulder into the mirror, authentic iPhone footage"},
            {"name": "Fabric Flow", "duration": 2, "motion_prompt": "Woman holding the hem of her dress and swishing it back and forth to show the fabric flow, smiling at the camera, messy bedroom"},
            {"name": "Hanger Show", "duration": 2, "motion_prompt": "UGC unboxing, woman holding the dress on a hanger, pointing at the pattern, talking excitedly to the camera, ring light"},
            {"name": "Try-On Jump", "duration": 2, "motion_prompt": "TikTok transition, woman holding dress, jumps and suddenly she is wearing it, laughing and showing off the fit, authentic smartphone video"},
            {"name": "Adjusting Fit", "duration": 3, "motion_prompt": "UGC video, woman standing in front of mirror, adjusting the straps of her dress, talking to the camera casually like a FaceTime call"},
            {"name": "Shoe Pairing", "duration": 3, "motion_prompt": "Woman sitting on bed, holding up a pair of shoes next to her new dress, asking the camera for styling advice, relatable haul video"},
            {"name": "Candid Laugh", "duration": 2, "motion_prompt": "Close up selfie, woman laughing candidly while talking about her dress, hand covering her mouth slightly, genuine relatable expression"},
            {"name": "Window Light", "duration": 3, "motion_prompt": "Woman standing by her bedroom window, natural light hitting the dress, turning slightly to show how it looks in the sun"},
            {"name": "Quick Zoom", "duration": 2, "motion_prompt": "UGC comedy style, quick zoom into the woman's face as she hypes up the dress, chaotic fun energy, messy bedroom"},
            {"name": "Outro Wave", "duration": 3, "motion_prompt": "UGC outro, woman waves goodbye to the camera, stepping back in her dress, blowing a kiss, authentic TikTok haul ending"}
        ],
        "outerwear": [
            {"name": "Mirror Selfie", "duration": 2, "motion_prompt": "UGC mirror selfie video, woman showing off her new jacket, zipping it up excitedly, messy bedroom background, authentic iphone footage"},
            {"name": "Bed Haul", "duration": 3, "motion_prompt": "TikTok haul video, woman holding up jacket to camera in her room, pointing at the pockets and texture, talking animatedly, ring light reflections"},
            {"name": "Shoulder Drape", "duration": 3, "motion_prompt": "UGC try-on, woman puts on jacket over her shoulders in her bedroom, poses casually for the phone camera, smiles, relatable everyday vibe"},
            {"name": "Close Up", "duration": 2, "motion_prompt": "UGC front-facing video, woman adjusting the collar of her jacket, smiling broadly at the camera, ring light in eyes, authentic TikTok aesthetic"},
            {"name": "Package Open", "duration": 3, "motion_prompt": "TikTok unboxing, woman pulling a jacket from a shipping bag, throwing the bag aside and holding the jacket up with excitement, bedroom floor"},
            {"name": "Full Spin", "duration": 3, "motion_prompt": "UGC try-on, woman steps back to show full jacket fit, turns around to show the back, looking over her shoulder at the camera, messy bedroom"},
            {"name": "Zipper Check", "duration": 2, "motion_prompt": "UGC close up, woman demonstrating how smooth the jacket zipper is, zipping it up and down while talking to camera"},
            {"name": "Hood Pull", "duration": 3, "motion_prompt": "TikTok vlog, woman pulling the jacket hood over her head, doing a cute pose, laughing at the camera, cozy bedroom setting"},
            {"name": "Pocket Deep", "duration": 2, "motion_prompt": "Woman enthusiastically shoving her hands deep into the jacket pockets to show how big they are, excited facial expression"},
            {"name": "Layering Show", "duration": 3, "motion_prompt": "UGC try-on, woman unzipping jacket to show how she layered it over a hoodie, casual streetwear styling video"},
            {"name": "Walking Hook", "duration": 2, "motion_prompt": "Woman walking towards phone camera wearing an oversized jacket, confident stride, messy room background, authentic smartphone look"},
            {"name": "Throw On", "duration": 2, "motion_prompt": "TikTok transition, woman throwing the jacket into the air and suddenly she is wearing it, striking a pose, fun energy"},
            {"name": "Lining Detail", "duration": 2, "motion_prompt": "UGC video, woman opening the jacket to show the inner lining to the camera, pointing at the details, talking fast"},
            {"name": "Cuff Check", "duration": 2, "motion_prompt": "Close up selfie, woman showing the sleeve cuffs of her new jacket, turning her wrist, casual review video"},
            {"name": "Back Logo", "duration": 3, "motion_prompt": "Woman turning her back to the camera to show off the design on the back of the jacket, looking over her shoulder"},
            {"name": "Cozy Hug", "duration": 3, "motion_prompt": "UGC lifestyle, woman wrapping her arms around herself to show how warm and cozy the jacket is, smiling happily"},
            {"name": "Hanger Show", "duration": 2, "motion_prompt": "Woman holding the jacket on a hanger, pointing at it with her other hand, explaining why she bought it"},
            {"name": "Window Light", "duration": 3, "motion_prompt": "Woman standing by her window showing how the jacket material looks in natural sunlight, authentic vlog style"},
            {"name": "Quick Zoom", "duration": 2, "motion_prompt": "Chaotic UGC style, quick zoom on woman's face as she gasps about the jacket quality, funny relatable energy"},
            {"name": "Outro Wave", "duration": 3, "motion_prompt": "UGC outro, woman fully zipped in her jacket, waves goodbye to the camera with a big smile, authentic TikTok ending"}
        ],
        "pants": [
            {"name": "Mirror Selfie", "duration": 2, "motion_prompt": "UGC mirror selfie video, woman angling phone down to show off pants fit, turning hips side to side, casual bedroom setting, raw smartphone video"},
            {"name": "Bed Haul", "duration": 3, "motion_prompt": "TikTok haul video, woman sitting cross-legged on bed, holding pants up to camera, stretching the waistband to show elasticity, excited expression"},
            {"name": "Full Fit", "duration": 3, "motion_prompt": "UGC try-on video, woman stepping back in room to show full pants fit, hands in pockets, casual pose, talking to phone camera, relatable energy"},
            {"name": "Close Up Waist", "duration": 2, "motion_prompt": "UGC close up, woman pointing to the high waist of her pants, talking to the camera to explain the fit, authentic smartphone video"},
            {"name": "Package Open", "duration": 3, "motion_prompt": "TikTok unboxing, woman opening a package on her bed, pulling out pants and holding them up against her legs, smiling widely"},
            {"name": "Dance Fit", "duration": 3, "motion_prompt": "UGC try-on, woman doing a quick hip-sway TikTok trend to show off pants fit, laughing at the camera, relatable and casual"},
            {"name": "Squat Test", "duration": 3, "motion_prompt": "TikTok review, woman doing a quick squat to show that the pants are stretchy and comfortable, giving a thumbs up to the camera"},
            {"name": "Walking Back", "duration": 3, "motion_prompt": "UGC try-on, woman walking away from the camera to show the back fit of the jeans, then turning around and smiling"},
            {"name": "Shoe Styling", "duration": 3, "motion_prompt": "Woman pointing the camera down at her feet to show how the pants drape perfectly over her sneakers, street style vlog"},
            {"name": "Pocket Check", "duration": 2, "motion_prompt": "Woman putting both hands in the front pockets of her pants, rocking back and forth on her heels, casual talking to camera"},
            {"name": "Waist Stretch", "duration": 2, "motion_prompt": "Close up selfie, woman pulling the waistband away from her body to show the stretch, excited facial expression, messy room"},
            {"name": "Sitting Check", "duration": 3, "motion_prompt": "UGC video, woman sitting down on her bed to show that the pants are still comfortable when seated, relatable review"},
            {"name": "Hanger Show", "duration": 2, "motion_prompt": "Woman holding the pants up on a hanger, pointing at the wash and material, talking fast to the camera"},
            {"name": "Jump Transition", "duration": 2, "motion_prompt": "TikTok transition, woman throws pants on the floor, jumps, and is suddenly wearing them, striking a cool pose"},
            {"name": "Ankle Detail", "duration": 2, "motion_prompt": "Woman pointing the phone camera at the hem of her pants to show the crop/flare detail, authentic smartphone angle"},
            {"name": "Side Profile", "duration": 3, "motion_prompt": "UGC mirror selfie, woman standing sideways to show the side profile of the pants fit, holding phone steady"},
            {"name": "Candid Laugh", "duration": 2, "motion_prompt": "Close up selfie, woman laughing while explaining a funny story about buying the pants, genuine relatable expression"},
            {"name": "Material Rub", "duration": 2, "motion_prompt": "Woman rubbing the fabric of the pants to show the texture to the camera, explaining how soft they are"},
            {"name": "Quick Zoom", "duration": 2, "motion_prompt": "UGC comedy style, quick zoom into woman's face as she hypes up how good the pants make her look, fun energy"},
            {"name": "Outro Wave", "duration": 3, "motion_prompt": "UGC outro, woman doing a casual salute or peace sign to the camera, full pants fit visible, authentic TikTok ending"}
        ],
        "tops": [
            {"name": "Mirror Selfie", "duration": 2, "motion_prompt": "UGC front-facing selfie video, woman talking excitedly to camera, holding up a cute top, pointing at the neckline, messy bedroom, ring light"},
            {"name": "Bed Haul", "duration": 3, "motion_prompt": "TikTok unboxing video, woman sitting on bed, pulling top out of package, holding it against her chest, huge smile, raw authentic iphone footage"},
            {"name": "Full Fit", "duration": 3, "motion_prompt": "UGC try-on, woman standing in bedroom, adjusting the fit of her new top, looking into the phone camera like a mirror, casual everyday vibe"},
            {"name": "Close Up", "duration": 2, "motion_prompt": "UGC close up selfie, woman tugging on the sleeve of her top to show the material, talking fast, authentic TikTok aesthetic, messy room"},
            {"name": "Package Open", "duration": 3, "motion_prompt": "TikTok unboxing, woman sitting on floor tearing open a package, pulling out a top and showing it to the camera excitedly"},
            {"name": "Dance Fit", "duration": 3, "motion_prompt": "UGC try-on, woman dancing casually in her bedroom to show off a new top, happy and energetic, authentic smartphone footage"},
            {"name": "Neckline Check", "duration": 2, "motion_prompt": "Woman adjusting the neckline of the top, looking at the camera as if it's a mirror, checking her outfit"},
            {"name": "Sleeve Roll", "duration": 3, "motion_prompt": "UGC video, woman rolling up the sleeves of her top, talking casually to the camera about styling it"},
            {"name": "Tuck In", "duration": 3, "motion_prompt": "TikTok try-on, woman demonstrating how to do a french tuck with the new top into her jeans, smiling at the result"},
            {"name": "Back Detail", "duration": 3, "motion_prompt": "UGC mirror selfie, woman turning to show the back of the top, looking over her shoulder, authentic smartphone aesthetic"},
            {"name": "Sitting Pose", "duration": 3, "motion_prompt": "Woman sitting on her unmade bed, wearing the new top, talking animatedly to the camera with hand gestures like a FaceTime call"},
            {"name": "Coffee Run", "duration": 3, "motion_prompt": "TikTok lifestyle vlog, woman holding a coffee cup, taking a sip while showing off her top in natural sunlight from her window"},
            {"name": "Hanger Show", "duration": 2, "motion_prompt": "Woman holding the top on a hanger, pointing at the print/graphic, talking excitedly to the camera"},
            {"name": "Try-On Jump", "duration": 2, "motion_prompt": "TikTok transition, woman holding top, covers the camera lens with it, uncovers it and she is wearing it, smiling"},
            {"name": "Fabric Stretch", "duration": 2, "motion_prompt": "Close up selfie, woman pulling the fabric of the top to show how stretchy and soft it is, messy bedroom"},
            {"name": "Candid Laugh", "duration": 2, "motion_prompt": "Close up selfie, woman laughing candidly while talking about her top, genuine relatable expression, ring light in eyes"},
            {"name": "Window Light", "duration": 3, "motion_prompt": "Woman standing by her bedroom window, natural light hitting the top, turning slightly to show the color in the sun"},
            {"name": "Styling Ideas", "duration": 3, "motion_prompt": "Woman holding up a jacket or accessory next to the top, asking the camera for styling opinions, interactive vlog style"},
            {"name": "Quick Zoom", "duration": 2, "motion_prompt": "Chaotic UGC style, quick zoom on woman's face as she hypes up the top, funny relatable Gen-Z energy"},
            {"name": "Outro Wave", "duration": 3, "motion_prompt": "UGC outro, woman blows a kiss to the camera and waves goodbye, wearing the top, authentic TikTok haul ending"}
        ]
    },    "curvy-chic": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy fashion model turns slowly in minimalist studio, showing hourglass fit of dress, warm golden lighting, camera focus breathing, soft shadows"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks confidently towards camera, dress drape emphasizes curves with realistic cloth folds, slow cinematic camera push-in, micro-drift"},
            {"name": "Outro", "duration": 3, "motion_prompt": "curvy model strikes confident pose, hand on hip, looks over shoulder with warm gorgeous smile, studio wall background, cinematic lighting"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "plus-size model wearing tailored blazer, adjusts front button, confident chic styling, studio key light, camera micro-drift, photorealistic skin pores"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks down city street, open coat flows in wind, camera follows her movement with dynamic tracking, subtle camera shake, urban daylight"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model pauses, looks back over shoulder, shrugs coat slightly, gorgeous smile, soft bokeh background, warm sunset light, handheld camera breathing"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy model waist-down profile pose, highlighting fit of high-waist jeans, hand on back pocket naturally, studio lighting, camera focus breathing"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy fashion model struts confidently, showing jeans fit in motion, camera at low angle tracking stride, handheld camera micro-drift, fabric folds"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model turns 360 degrees showing full jeans fit, finishes with confident hand-on-hip pose, warm smile, studio backdrop, cinematic lighting"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy model styling vlog, adjusts neckline of elegant top, smiles confidently, warm studio lighting, handheld camera feel, photorealistic drape"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks in modern studio, top moves naturally with her stride, soft lifestyle camera follow, micro-drift, natural depth blur"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model strikes a confident pose, turns side profile then smiles at camera, chic fashion presentation, studio sunset lighting, handheld camera shake"}
        ]
    }
}

def get_storyboard_scenes(style_direction: str, product_category: str) -> list:
    import random
    style_dict = SCENE_TEMPLATES.get(style_direction, SCENE_TEMPLATES["casual"])
    # Fallback to "tops" if category not found in the style
    category_scenes = style_dict.get(product_category, style_dict.get("tops"))
    
    # If the pool has more than 3 scenes, randomly select 3 to ensure variety
    if len(category_scenes) > 3:
        # We ensure the hook (index 0) is often included, but randomize the rest.
        # For true randomness, we can just sample 3.
        sampled_scenes = random.sample(category_scenes, 3)
        return sampled_scenes
        
    return category_scenes

def download_file(url: str, output_path: str) -> bool:
    print(f"  → Downloading generated clip from {url[:60]}... to {output_path}")
    try:
        resp = requests.get(url, stream=True)
        resp.raise_for_status()
        with open(output_path, 'wb') as f:
            for chunk in resp.iter_content(chunk_size=8192):
                f.write(chunk)
        return True
    except Exception as e:
        print(f"  ✗ Failed to download video clip: {e}")
        return False

def concat_videos_ffmpeg(video_paths: list, output_path: str) -> bool:
    print("  → Stitching video clips with FFmpeg...")
    list_path = output_path + ".txt"
    try:
        # Write the input list file
        with open(list_path, 'w') as f:
            for vp in video_paths:
                f.write(f"file '{os.path.abspath(vp)}'\n")
        
        # Try fast concat copy first (zero quality loss, <0.1s run time)
        cmd = [
            FFMPEG_BIN, "-y",
            "-f", "concat", "-safe", "0",
            "-i", list_path,
            "-c", "copy",
            output_path
        ]
        print(f"    Running FFmpeg: {' '.join(cmd)}")
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if res.returncode == 0:
            print("  ✓ FFmpeg fast concat completed successfully!")
            return True
            
        # Fallback: re-encode concat
        print("  ⚠ FFmpeg fast concat failed. Falling back to re-encode concat...")
        cmd_fallback = [
            FFMPEG_BIN, "-y",
            "-f", "concat", "-safe", "0",
            "-i", list_path,
            "-c:v", "libx264", "-pix_fmt", "yuv420p",
            output_path
        ]
        print(f"    Running FFmpeg fallback: {' '.join(cmd_fallback)}")
        res_fallback = subprocess.run(cmd_fallback, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if res_fallback.returncode == 0:
            print("  ✓ FFmpeg re-encode concat completed successfully!")
            return True
        else:
            print(f"  ✗ FFmpeg concat failed: {res_fallback.stderr.decode()}")
            return False
    except Exception as e:
        print(f"  ✗ Exception during FFmpeg stitching: {e}")
        return False
    finally:
        # Clean up list file
        if os.path.exists(list_path):
            try:
                os.remove(list_path)
            except:
                pass

# ─── Audio and Voiceover Helpers ──────────────────────────────────────────────

DEFAULT_BGM_URL = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

VOICEOVER_VOICES_ELEVENLABS = {
    "Aria": "21m00Tcm4TlvDq8ikWAM",  # Rachel (editorial, warm)
    "Luna": "piTKgcLEGmPEe242Cchg",  # Nicole (streetwear, youthful)
    "Nova": "z9fAnwMTq79t1maRnPAg",  # Glinda (casual, upbeat)
    "Sasha": "EXAVITQu4vr4xnSDxMaL", # Bella (curvy-chic, confident)
}

VOICEOVER_VOICES_EDGETTS = {
    "Aria": "en-US-AvaNeural",
    "Luna": "en-US-AnaNeural",
    "Nova": "en-US-EmmaMultilingualNeural",
    "Sasha": "en-US-JennyNeural",
}

def generate_voiceover(text: str, model_name: str, output_path: str) -> bool:
    print(f"  → Generating voiceover for model '{model_name}'...")
    
    # 1. Try ElevenLabs if API key is present
    elevenlabs_key = os.getenv("ELEVENLABS_API_KEY")
    if elevenlabs_key:
        voice_id = VOICEOVER_VOICES_ELEVENLABS.get(model_name, "21m00Tcm4TlvDq8ikWAM")
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
        headers = {
            "Accept": "audio/mpeg",
            "Content-Type": "application/json",
            "xi-api-key": elevenlabs_key
        }
        data = {
            "text": text,
            "model_id": "eleven_monolingual_v1",
            "voice_settings": {
                "stability": 0.5,
                "similarity_boost": 0.75
            }
        }
        try:
            print("    [TTS] Sending request to ElevenLabs API...")
            resp = requests.post(url, json=data, headers=headers)
            if resp.status_code == 200:
                with open(output_path, 'wb') as f:
                    f.write(resp.content)
                print(f"    ✓ ElevenLabs voiceover saved to: {output_path}")
                return True
            else:
                print(f"    ⚠ ElevenLabs API returned status code {resp.status_code}: {resp.text}")
        except Exception as e:
            print(f"    ⚠ Exception during ElevenLabs TTS generation: {e}")
            
    # 2. Fallback to Microsoft Edge TTS (Free Neural)
    print("    [TTS] Falling back to Microsoft Edge Neural TTS...")
    voice = VOICEOVER_VOICES_EDGETTS.get(model_name, "en-US-AvaNeural")
    try:
        import asyncio
        import edge_tts
        
        async def tts_main():
            communicate = edge_tts.Communicate(text, voice)
            await communicate.save(output_path)
            
        asyncio.run(tts_main())
        print(f"    ✓ Edge TTS voiceover saved to: {output_path}")
        return True
    except Exception as e:
        print(f"    ✗ Microsoft Edge TTS generation failed: {e}")
        return False

def mix_audio(silent_video_path: str, voiceover_path: str, bgm_path: str, output_path: str) -> bool:
    print(f"  → Overlaying audio onto {silent_video_path}...")
    try:
        has_vo = voiceover_path and os.path.exists(voiceover_path)
        has_bgm = bgm_path and os.path.exists(bgm_path)
        
        if has_vo and has_bgm:
            # Case 1: Both voiceover and looped BGM
            cmd = [
                FFMPEG_BIN, "-y",
                "-i", silent_video_path,
                "-i", voiceover_path,
                "-stream_loop", "-1",
                "-i", bgm_path,
                "-filter_complex", "[2:a]volume=0.2[bgm];[1:a][bgm]amix=inputs=2:duration=longest[a]",
                "-map", "0:v:0",
                "-map", "[a]",
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                output_path
            ]
        elif has_bgm:
            # Case 2: Only BGM
            cmd = [
                FFMPEG_BIN, "-y",
                "-i", silent_video_path,
                "-stream_loop", "-1",
                "-i", bgm_path,
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                output_path
            ]
        elif has_vo:
            # Case 3: Only voiceover
            cmd = [
                FFMPEG_BIN, "-y",
                "-i", silent_video_path,
                "-i", voiceover_path,
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                output_path
            ]
        else:
            print("    ✗ No audio sources (BGM or voiceover) available to overlay.")
            return False
            
        print(f"    Running FFmpeg audio mix: {' '.join(cmd)}")
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if res.returncode == 0:
            print("  ✓ FFmpeg audio mix completed successfully!")
            return True
        else:
            print(f"  ✗ FFmpeg audio mix failed: {res.stderr.decode()}")
            return False
    except Exception as e:
        print(f"  ✗ Exception during FFmpeg audio mixing: {e}")
        return False

def assemble_ugc_video(
    product_id: str,
    model_name: str,
    clip_paths: list,
    success_count: int,
    scenes: list,
    bgm_path: str,
    script: str,
    final_video_file: str,
    vton_image_url: str
) -> tuple:
    """Stitch clips, generate voiceover, mix audio, and return (video_url, status, error_msg)."""
    if success_count == 0:
        print("  ✗ All scene generations failed. Using image as fallback.")
        return vton_image_url, "partial", "All video scenes failed"
        
    is_partial = success_count < len(scenes)
    if is_partial:
        print(f"  ⚠ Only {success_count}/{len(scenes)} scenes succeeded. Stitching partial scenes...")
        
    temp_silent_file = final_video_file + ".silent.mp4"
    if concat_videos_ffmpeg(clip_paths, temp_silent_file):
        print(f"  ✓ Stitched video created at: {temp_silent_file}")
        
        # Clean up temporary single-shot clips
        for cp in clip_paths:
            try:
                os.remove(cp)
            except:
                pass
                
        # Generate Voiceover
        voiceover_file = f"output_assets/temp_{product_id}_{model_name}_voiceover.mp3"
        voiceover_ok = generate_voiceover(script, model_name, voiceover_file)
        
        # Apply Lip-Sync (Sync.so) if API key exists
        # NOTE: Sync.so requires public URLs, so in production we would upload
        # temp_silent_file and voiceover_file to S3/GCS first, pass the URLs,
        # and then download the synced video back to temp_silent_file.
        if os.getenv("SYNCSO_API_KEY") and voiceover_ok:
            print("  → [TODO] Upload assets to S3 and pass to Sync.so for lip-sync")
            # public_vid = upload_to_s3(temp_silent_file)
            # public_aud = upload_to_s3(voiceover_file)
            # synced_url = lipsync_syncso(public_vid, public_aud)
            # download_file(synced_url, temp_silent_file)
            pass
        
        # Mix voiceover + BGM
        has_mixed_audio = False
        bgm_to_use = bgm_path if (bgm_path and os.path.exists(bgm_path)) else None
        vo_to_use = voiceover_file if (voiceover_ok and os.path.exists(voiceover_file)) else None
        
        if vo_to_use or bgm_to_use:
            if mix_audio(temp_silent_file, vo_to_use, bgm_to_use, final_video_file):
                has_mixed_audio = True
                
        # Clean up voiceover temp file
        if os.path.exists(voiceover_file):
            try:
                os.remove(voiceover_file)
            except:
                pass
                
        if not has_mixed_audio:
            print("  ⚠ Audio overlay failed. Using silent video as fallback.")
            try:
                if os.path.exists(final_video_file):
                    os.remove(final_video_file)
                os.rename(temp_silent_file, final_video_file)
            except Exception as e:
                print(f"  ✗ Failed to rename silent fallback: {e}")
        else:
            # Clean up silent temp file
            try:
                os.remove(temp_silent_file)
            except:
                pass
                
        return final_video_file, "partial" if is_partial else "success", None
    else:
        print("  ✗ FFmpeg stitching failed. Falling back to the first clip.")
        fallback_url = clip_paths[0] if clip_paths else vton_image_url
        return fallback_url, "partial", "FFmpeg stitching failed"

# ─── Prompt Builder ─────────────────────────────────────────────────────────────

def build_fashion_prompt(model_data: dict, product_name: str, product_description: str) -> str:
    """Build a structured 20+ line fashion photography prompt following the skill methodology."""
    demo = model_data["demographics"]
    app = model_data["appearance"]
    photo = model_data["photography"]

    prompt = f"""Professional fashion photography of a {demo['age']} {demo['origin']} female model wearing {product_name}.

SCENE: {model_data['environment']}

MODEL SPECIFICATIONS:
- Origin: {demo['origin']}
- Gender: {demo['gender']}
- Age: {demo['age']}
- Body Type: {demo['body_type']}
- Height: {demo['height']}
- Skin Tone: {app['skin_tone']}
- Hair: {app['hair']}
- Expression: {app['expression']}
- Appearance: {app['features']}
- Makeup: {app['makeup']}

POSE AND BODY LANGUAGE:
{model_data['pose']}

STYLING:
- Main Product: {product_name} — {product_description}
- Fabric Behavior: Natural drape with organic folds, realistic texture and movement
- Accessories: Minimal, complementary, not competing with main product

PHOTOGRAPHY TECHNICAL:
- Camera: {photo['camera']}
- Lens: {photo['lens']}
- Aperture: {photo['aperture']}
- Lighting: {photo['lighting']}
- Composition: {photo['composition']}
- Angle: {photo['angle']}

MOOD AND ATMOSPHERE:
{model_data['mood']}

Professional fashion photography, editorial quality, {model_data['style_direction']} aesthetic,
photorealistic, natural skin texture, visible pores, authentic moment, 9:16 vertical social media format, 8K resolution.

Negative: No AI-smooth skin, no distorted hands, no extra fingers, no waxy appearance, no rigid symmetrical pose, no flat lighting, no watermarks."""

    return prompt.strip()


def build_storyboard_panels(product_name: str, model_name: str, duration: int = 5) -> list:
    """Generate storyboard panel descriptions for a fashion product video."""
    if duration <= 5:
        return [
            {"panel": 1, "duration": "0.5s", "beat": f"Extreme close-up: {product_name} texture and fabric detail, macro lens feel"},
            {"panel": 2, "duration": "0.5s", "beat": f"{model_name}'s hand reaches into frame, fingers touch the garment naturally"},
            {"panel": 3, "duration": "0.5s", "beat": f"Medium shot: {model_name} turns to reveal full silhouette of {product_name}"},
            {"panel": 4, "duration": "0.5s", "beat": "Close-up: fabric movement and drape as model shifts weight"},
            {"panel": 5, "duration": "0.5s", "beat": f"Full body: {model_name} walks toward camera with confidence"},
            {"panel": 6, "duration": "0.5s", "beat": "Low angle: dynamic power shot looking up at model"},
            {"panel": 7, "duration": "0.5s", "beat": f"Detail shot: accessory or brand element of {product_name}"},
            {"panel": 8, "duration": "0.5s", "beat": f"Three-quarter: {model_name} pauses, looks directly at camera"},
            {"panel": 9, "duration": "1.0s", "beat": f"Wide reveal: full look in styled environment, {product_name} as hero piece"},
        ]
    return [
        {"panel": i + 1, "duration": "1.0s", "beat": f"Panel {i+1} action beat"}
        for i in range(9)
    ]


def generate_video_script(product_name: str, model_name: str, style_dir: str = "casual", category: str = "tops") -> str:
    """
    Generate an authentic, high-converting short UGC script based on the Reddit user framework:
      1. Hook (bold opener specific to persona)
      2. Pain Point / Problem (relatable struggle specific to product type/style)
      3. Solution & CTA (natural pivot with no marketing lingo)
    """
    # 1. Hooks based on persona/style
    hooks = {
        "editorial": [
            f"I thought this {product_name} was just hype, but the drape is gorgeous.",
            f"This {product_name} is the definition of quiet luxury.",
            f"If you've been looking for that perfect editorial piece, this is it."
        ],
        "streetwear": [
            f"Streetwear check: this new {product_name} is literally everything.",
            f"If you've tried finding a styled {product_name} that actually fits, watch this.",
            f"I saw this {product_name} trending and had to give it a shot."
        ],
        "casual": [
            f"Okay, I am genuinely obsessed with this {product_name}.",
            f"I just found the absolute cutest {product_name} ever.",
            f"If you need a sign to upgrade your daily style, here it is."
        ],
        "curvy-chic": [
            f"Curvy style check! Finding a {product_name} that actually fits is so hard, but...",
            f"This is the most flattering {product_name} I have ever styled, hands down.",
            f"Confidence level 100 in this fit. Look at the silhouette."
        ]
    }
    
    # 2. Pain points/Problems based on style and category
    problems = {
        "editorial": [
            "Usually high-end tailoring is so stiff, but this flows naturally.",
            "Tailored pieces are usually either way too expensive or feel cheap, but this fabric...",
            "Most brand pieces gap or don't hold structure, but this tailor-made feel..."
        ],
        "streetwear": [
            "Usually streetwear fits get stiff or fade after one wash, but this quality...",
            "Most oversized styles look totally boxy, but this drape is perfect.",
            "It's super hard to find cozy fits that still look elevated, but..."
        ],
        "casual": [
            "I've tried so many daily pieces that lose their softness, but this cotton...",
            "Normally, casual stuff looks super plain, but the design details here...",
            "I wanted something soft but still stylish, and this hits the spot."
        ],
        "curvy-chic": [
            "Usually clothing either gaps at the waist or pinches my hips, but this...",
            "Most brands do not design for curvy bodies, but this hugs in all the right places.",
            "It gives you that comfortable hold without feeling restrictive at all."
        ]
    }

    # 3. Solutions / CTAs
    ctas = [
        f"It's not just a basic—it's a total wardrobe upgrade. Comment 'FIT' and I'll DM you the link!",
        f"The fabric and quality are unmatched. Just comment 'LINK' to get it sent straight to your DMs.",
        f"I seriously wish I had found this earlier. Comment 'STYLE' and I'll DM you the link to shop!"
    ]
    
    # Choose elements
    style_hooks = hooks.get(style_dir, hooks["casual"])
    style_problems = problems.get(style_dir, problems["casual"])
    
    hook = random.choice(style_hooks)
    problem = random.choice(style_problems)
    cta = random.choice(ctas)
    
    # Combine into a natural sounding, punchy UGC script
    script = f"{hook} {problem} {cta}"
    return script





# ─── Mid-Flight State Management ────────────────────────────────────────────────

def get_state_path(product_id: str) -> str:
    return get_absolute_path(os.path.join("output_assets", f"in_flight_{product_id}.json"))

def load_in_flight_state(product_id: str) -> dict:
    path = get_state_path(product_id)
    if os.path.exists(path):
        try:
            with open(path, "r") as f:
                return json.load(f)
        except Exception:
            pass
    return {}

def save_in_flight_state(product_id: str, state_data: dict):
    path = get_state_path(product_id)
    try:
        with open(path, "w") as f:
            json.dump(state_data, f, indent=2)
    except Exception as e:
        print(f"  → Warning: Could not write in-flight state file: {e}")

def update_in_flight_state(product_id: str, key_path: list, task_id: str):
    state = load_in_flight_state(product_id)
    curr = state
    for k in key_path[:-1]:
        if k not in curr:
            curr[k] = {}
        curr = curr[k]
    curr[key_path[-1]] = task_id
    save_in_flight_state(product_id, state)

def clear_in_flight_state(product_id: str):
    path = get_state_path(product_id)
    if os.path.exists(path):
        os.remove(path)

# ─── EachLabs VTON Integration ──────────────────────────────────────────────────

def _eachlabs_predict(model_slug: str, input_data: dict, existing_task_id: str = None, on_task_started=None) -> dict:
    """Submit prediction to EachLabs and poll until complete."""
    api_key = os.getenv("EACHLABS_API_KEY")
    headers = {"Content-Type": "application/json", "X-API-Key": api_key}

    prediction_id = existing_task_id
    if not prediction_id:
        # Submit prediction
        resp = requests.post(f"{EACHLABS_BASE_URL}/prediction", headers=headers, json={
            "model": model_slug,
            "version": "0.0.1",
            "input": input_data,
        })
        if resp.status_code not in (200, 201):
            try:
                err_json = resp.json()
                err_msg = err_json.get("details") or err_json.get("error") or resp.text
                raise Exception(f"EachLabs API Error ({resp.status_code}): {err_msg}")
            except Exception as e:
                if "EachLabs API Error" in str(e):
                    raise e
                resp.raise_for_status()
        result = resp.json()
        prediction_id = result.get("id") or result.get("prediction_id") or result.get("predictionID")
        
        if not prediction_id:
            return result
            
        if on_task_started:
            on_task_started(prediction_id)
    else:
        print(f"  → Resuming existing EachLabs task: {prediction_id}")

    # Poll for completion
    for _ in range(120):  # Max 10 minutes
        time.sleep(5)
        status_resp = requests.get(f"{EACHLABS_BASE_URL}/prediction/{prediction_id}", headers=headers)
        status_resp.raise_for_status()
        status_data = status_resp.json()

        status = status_data.get("status", "")
        if status == "success":
            return status_data
        elif status in ("failed", "error"):
            err_msg = status_data.get("output") or status_data.get("error") or "Unknown error"
            raise Exception(f"EachLabs prediction failed with status '{status}': {err_msg}")

        print(f"    ⏳ EachLabs status: {status}...")

    raise TimeoutError("EachLabs prediction timed out after 10 minutes")


def vton_eachlabs(product_image_url: str, model_data: dict, existing_task_id: str = None, on_task_started=None) -> str:
    """Virtual try-on using EachLabs Kolors VTON — puts real product on AI model."""
    print(f"  → Virtual Try-On via EachLabs ({EACHLABS_VTON_MODEL})...")
    
    result = _eachlabs_predict(EACHLABS_VTON_MODEL, {
        "garment_image_url": product_image_url,
        "human_image_url": model_data["human_image_url"],
    }, existing_task_id, on_task_started)

    # Extract output URL
    output = result.get("output", {})
    if isinstance(output, dict):
        return output.get("image_url", "") or output.get("url", "")
    elif isinstance(output, str):
        return output
    return ""


def image_to_image_eachlabs(image_url: str, prompt: str, style_dir: str, existing_task_id: str = None, on_task_started=None) -> str:
    """Generate a character-consistent storyboard image using EachLabs Google Nano Banana 2 Edit."""
    print(f"  → Storyboard keyframe image generation via EachLabs ({EACHLABS_IMG2IMG_MODEL})...")
    
    result = _eachlabs_predict(EACHLABS_IMG2IMG_MODEL, {
        "prompt": prompt,
        "image_urls": [image_url],
        "aspect_ratio": "9:16",
        "resolution": "1K",
        "thinking_level": "high",
    }, existing_task_id, on_task_started)

    output = result.get("output", {})
    if isinstance(output, dict):
        return output.get("image_url", "") or output.get("url", "")
    elif isinstance(output, str):
        return output
    return ""



def video_eachlabs(image_url: str, motion_prompt: str, duration: int = 5, existing_task_id: str = None, on_task_started=None) -> str:
    """Generate runway video using EachLabs Pixverse."""
    print(f"  → Video generation via EachLabs ({EACHLABS_VIDEO_MODEL})...")

    result = _eachlabs_predict(EACHLABS_VIDEO_MODEL, {
        "image_url": image_url,
        "prompt": motion_prompt,
        "duration": str(duration),
        "resolution": "1080p",
    }, existing_task_id, on_task_started)

    output = result.get("output", {})
    if isinstance(output, dict):
        return output.get("video_url", "") or output.get("url", "")
    elif isinstance(output, str):
        return output
    return ""


# ─── Higgsfield Integration ────────────────────────────────────────────────────

def image_higgsfield(prompt: str, existing_task_id: str = None, on_task_started=None) -> str:
    """Generate image using Higgsfield Soul."""
    print(f"  → Image generation via Higgsfield Soul...")
    
    if existing_task_id:
        print(f"  → Resuming existing Higgsfield task: {existing_task_id}")
        request_id = existing_task_id
    else:
        req = higgsfield_client.submit(
            HF_IMAGE_MODEL,
            arguments={"prompt": prompt, "aspect_ratio": "9:16", "resolution": "720p"},
        )
        # Handle SyncRequestController attributes, fallback if different structure
        request_id = getattr(req, 'request_id', None)
        if not request_id and hasattr(req, '__dict__'):
            for v in req.__dict__.values():
                if isinstance(v, str) and len(v) > 10:
                    request_id = v
                    break
        if on_task_started and request_id:
            on_task_started(request_id)
            
    # Poll
    for _ in range(120): # 10 mins
        time.sleep(5)
        status = higgsfield_client.status(request_id)
        if type(status) in higgsfield_client.DONE_STATUSES:
            if type(status) == higgsfield_client.types_.Completed:
                result = higgsfield_client.result(request_id)
                images = result.get("images", [])
                return images[0].get("url", "") if images else ""
            else:
                raise Exception(f"Higgsfield failed with status: {type(status)}")
    raise TimeoutError("Higgsfield generation timed out")


def video_higgsfield(image_url: str, motion_prompt: str, duration: int = 5) -> str:
    """Generate video using Higgsfield DoP/Veo3."""
    print(f"  → Video generation via Higgsfield (model: {HF_VIDEO_MODEL})...")
    result = higgsfield_client.subscribe(
        HF_VIDEO_MODEL,
        arguments={
            "image": image_url,
            "image_url": image_url,
            "prompt": motion_prompt,
            "duration": duration
        },
    )
    video = result.get("video", {})
    if isinstance(video, dict) and video.get("url"):
        return video["url"]
    output = result.get("output", {})
    if isinstance(output, dict):
        url = output.get("video_url") or output.get("url")
        if url:
            return url
    elif isinstance(output, str) and output.startswith("http"):
        return output
    images = result.get("images", [])
    if images and isinstance(images, list):
        first_img = images[0]
        if isinstance(first_img, dict) and first_img.get("url"):
            return first_img["url"]
        elif isinstance(first_img, str) and first_img.startswith("http"):
            return first_img
    return ""


# ─── Main Pipeline ──────────────────────────────────────────────────────────────

def run_ugc_pipeline(
    product_id: str,
    product_name: str,
    product_description: str,
    product_image_url: str,
    product_type: str,
    duration: int = 5,
    bgm_url: str = DEFAULT_BGM_URL,
    model_id_filter: str = None,
) -> list:
    """
    Full UGC video generation pipeline.

    Priority:
    1. EachLabs VTON (if EACHLABS_API_KEY set) — real product on model
    2. Higgsfield (if HF_KEY set) — text-to-image + image-to-video
    3. Mock fallback
    """
    # Determine available backends
    has_eachlabs = bool(os.getenv("EACHLABS_API_KEY")) and HAS_REQUESTS
    has_higgsfield = bool(os.getenv("HF_KEY") or (os.getenv("HF_API_KEY") and os.getenv("HF_API_SECRET"))) and HAS_HIGGSFIELD

    if not has_eachlabs and not has_higgsfield:
        raise ValueError("No API credentials set. Please configure EACHLABS_API_KEY or HF_KEY/HF_API_KEY/HF_API_SECRET in the environment.")

    # Hybrid configuration logic
    img_backend = "eachlabs" if has_eachlabs else "higgsfield"
    video_backend = "higgsfield" if has_higgsfield else "eachlabs"
    combined_backend = f"{img_backend}+{video_backend}"

    print(f"\n{'='*60}")
    print(f"  Meshada UGC Pipeline v2 — Image: {img_backend.upper()} | Video: {video_backend.upper()}")
    print(f"  Product: {product_name}")
    print(f"{'='*60}")

    # Ensure output directory exists
    os.makedirs(get_absolute_path("output_assets"), exist_ok=True)
 
    cache_path = get_absolute_path(os.path.join("output_assets", f"cache_{product_id}.json"))
    cache_data = {}
    if os.path.exists(cache_path):
        try:
            with open(cache_path, "r") as f:
                cache_data = json.load(f)
            print(f"  → Loaded cached VTON and storyboard image assets from: {cache_path}")
        except Exception as e:
            print(f"  → Warning: Could not read cache file: {e}")
 
    # Download BGM if provided
    bgm_path = None
    if bgm_url:
        bgm_filename = "background_music.mp3"
        if bgm_url != DEFAULT_BGM_URL:
            # Generate a cached filename based on custom URL
            import urllib.parse
            import hashlib
            parsed = urllib.parse.urlparse(bgm_url)
            path_nodes = [node for node in parsed.path.split('/') if node]
            if path_nodes:
                bgm_filename = f"bgm_{path_nodes[-1]}"
            else:
                bgm_filename = f"bgm_{hashlib.md5(bgm_url.encode()).hexdigest()}.mp3"
        
        bgm_path = get_absolute_path(os.path.join("output_assets", bgm_filename))
        if not os.path.exists(bgm_path):
            print(f"  → Downloading background music from {bgm_url[:60]}...")
            if not download_file(bgm_url, bgm_path):
                print("  ⚠ Failed to download background music. final videos will not have background music.")
                bgm_path = None
 
    # Detect product category
    category = detect_product_category(product_name, product_description)
    print(f"  Detected Category: {category.upper()}")
 
    final_outputs = []

    # If no model filter is specified, randomly pick ONE model to save API costs
    if not model_id_filter:
        import random
        model_id_filter = random.choice(list(AI_MODELS.keys()))
        print(f"  [Optimizer] No specific model requested. Randomly selected: {AI_MODELS[model_id_filter]['name']}")

    for model_id, model_data in AI_MODELS.items():
        if model_id_filter and model_id != model_id_filter:
            continue
        model_name = model_data["name"]
        style_dir = model_data["style_direction"]
        print(f"\n{'─'*60}")
        print(f"  Persona: {model_name} ({style_dir})")
        print(f"{'─'*60}")
 
        model_cache = cache_data.get(model_id, {})
        cached_vton = model_cache.get("vton_image")
        cached_storyboard = model_cache.get("storyboard_images", [])
        in_flight = load_in_flight_state(product_id).get(model_id, {})
 
        try:
            # ── Step 1: Generate model image (VTON or text-to-image) ──
            if cached_vton:
                print(f"[1/5] Reusing cached Virtual Try-On image...")
                vton_image_url = cached_vton
            elif img_backend == "eachlabs":
                print(f"[1/5] Virtual Try-On — putting real product on {model_name}...")
                vton_image_url = vton_eachlabs(product_image_url, model_data)
            else:
                print(f"[1/5] Fashion photography generation for {model_name}...")
                fashion_prompt = build_fashion_prompt(model_data, product_name, product_description)
                vton_image_url = image_higgsfield(fashion_prompt)
 
            if not vton_image_url:
                print(f"  ✗ Image generation failed for {model_name}")
                final_outputs.append({
                    "model_id": model_id, "model_name": model_name,
                    "status": "failed", "error": "Image generation returned no URL",
                })
                continue
            print(f"  ✓ Image: {vton_image_url[:80]}...")
 
            # ── Step 2: Resolve dynamic storyboard scenes ──
            raw_scenes = get_storyboard_scenes(style_dir, category)
            scenes = []
            for sc in raw_scenes:
                motion_prompt = sc["motion_prompt"]
                # Dynamically replace generic clothing words with the actual product name
                # to prevent contradictions that trigger Google's safety filters / empty responses
                motion_prompt = motion_prompt.replace("silk blouse", product_name)
                motion_prompt = motion_prompt.replace("silk shirt", product_name)
                motion_prompt = motion_prompt.replace("graphic tee", product_name)
                motion_prompt = motion_prompt.replace("casual tee", product_name)
                motion_prompt = motion_prompt.replace("silk", "fabric")
                
                # Prevent physical contradictions for fitted items (like tank tops or tees)
                if any(x in product_name.lower() or x in product_description.lower() for x in ["tank top", "t-shirt", "tee", "bodysuit", "fitted"]):
                    motion_prompt = motion_prompt.replace("flowing in wind", "moving naturally")
                    motion_prompt = motion_prompt.replace("flowing in the wind", "moving naturally")
                    motion_prompt = motion_prompt.replace("swaying naturally", "fitting perfectly")
                
                scenes.append({
                    "name": sc["name"],
                    "duration": sc["duration"],
                    "motion_prompt": motion_prompt
                })
            
            print(f"[2/5] Storyboard planning ({len(scenes)} scenes)...")
            for idx, sc in enumerate(scenes):
                print(f"    Scene {idx+1} ({sc['name']}): {sc['motion_prompt'][:65]}... ({sc['duration']}s)")
 
            # ── Step 3: Generate storyboard keyframe images ──
            storyboard_images = []
            if cached_storyboard and len(cached_storyboard) == len(scenes):
                print(f"[3/5] Reusing {len(cached_storyboard)} cached storyboard keyframe images...")
                storyboard_images = cached_storyboard
            else:
                print(f"[3/5] Generating consistent storyboard keyframe images...")
                for idx, sc in enumerate(scenes):
                    print(f"  → Generating keyframe {idx+1}/{len(scenes)} ({sc['name']})...")
                    # Build an edit instruction prompt optimized for Google's Nano Banana 2 Edit
                    keyframe_prompt = (
                        f"Modify the pose and scene environment to match: {sc['motion_prompt']}. "
                        f"Keep the model's identity, face, hair, and the clothing/attire (colors, patterns, style) "
                        f"exactly the same as the original image. Retain high-end professional fashion photography quality."
                    )
                    
                    scene_img_url = None
                    if img_backend == "eachlabs":
                        try:
                            scene_img_url = image_to_image_eachlabs(vton_image_url, keyframe_prompt, style_dir)
                        except Exception as e:
                            print(f"    ⚠ Storyboard image generation failed for scene {idx+1}: {e}")
                    
                    if not scene_img_url:
                        print(f"    → Using base VTON image as fallback for scene {idx+1}")
                        scene_img_url = vton_image_url
                    
                    print(f"    ✓ Keyframe {idx+1} image: {scene_img_url[:80]}...")
                    storyboard_images.append(scene_img_url)

            # ── Step 4: Script generation ──
            script = generate_video_script(product_name, model_name, style_dir, category)
            print(f"[4/5] Script: \"{script[:60]}...\"")

            # ── Step 5: Multi-scene Video generation & stitching ──
            print(f"[5/5] Generating and stitching scenes...")
            clip_paths = []
            
            # Ensure output directory exists
            os.makedirs(get_absolute_path("output_assets"), exist_ok=True)
            
            success_count = 0
            for idx, sc in enumerate(scenes):
                print(f"  → Animating Scene {idx+1}/{len(scenes)} ({sc['name']})...")
                scene_prompt = sc["motion_prompt"]
                scene_dur = sc["duration"]
                scene_base_img = storyboard_images[idx]
                
                # Request video generation from the backend
                scene_key = f"scene_{idx}"
                scene_task_id = in_flight.get(scene_key)
                def on_scene_started(tid):
                    update_in_flight_state(product_id, [model_id, scene_key], tid)
                    
                if video_backend == "eachlabs":
                    scene_video_url = video_eachlabs(scene_base_img, scene_prompt, scene_dur, existing_task_id=scene_task_id, on_task_started=on_scene_started)
                    # Dynamic Fallback to Higgsfield if EachLabs fails
                    if not scene_video_url and has_higgsfield:
                        print("    ⚠ EachLabs video failed, attempting fallback to Higgsfield...")
                        scene_video_url = video_higgsfield(scene_base_img, scene_prompt, scene_dur, existing_task_id=scene_task_id, on_task_started=on_scene_started)
                else:
                    scene_video_url = video_higgsfield(scene_base_img, scene_prompt, scene_dur, existing_task_id=scene_task_id, on_task_started=on_scene_started)
                    # Dynamic Fallback to EachLabs if Higgsfield fails
                    if not scene_video_url and has_eachlabs:
                        print("    ⚠ Higgsfield video failed, attempting fallback to EachLabs...")
                        scene_video_url = video_eachlabs(scene_base_img, scene_prompt, scene_dur, existing_task_id=scene_task_id, on_task_started=on_scene_started)
                
                if scene_video_url:
                    print(f"    ✓ Scene {idx+1} video URL: {scene_video_url[:80]}...")
                    # Download the clip locally
                    clip_file = get_absolute_path(f"output_assets/temp_{product_id}_{model_name}_scene_{idx+1}.mp4")
                    if download_file(scene_video_url, clip_file):
                        clip_paths.append(clip_file)
                        success_count += 1
                else:
                    print(f"    ✗ Scene {idx+1} generation returned no URL")
            

            # Assemble the final video (stitch, voiceover, and bgm mix)
            final_video_file = get_absolute_path(f"output_assets/prod_{product_id}_{model_name}_final.mp4")
            video_url, status, error = assemble_ugc_video(
                product_id=product_id,
                model_name=model_name,
                clip_paths=clip_paths,
                success_count=success_count,
                scenes=scenes,
                bgm_path=bgm_path,
                script=script,
                final_video_file=final_video_file,
                vton_image_url=vton_image_url
            )
            
            # Convert absolute paths to relative paths starting with 'output_assets/' for returned JSON
            rel_video_url = video_url
            if video_url and not video_url.startswith("http"):
                rel_video_url = os.path.relpath(video_url, SCRIPT_DIR)
            rel_vton_image = vton_image_url
            if vton_image_url and not vton_image_url.startswith("http"):
                rel_vton_image = os.path.relpath(vton_image_url, SCRIPT_DIR)

            final_outputs.append({
                "model_id": model_id,
                "model_name": model_name,
                "style": style_dir,
                "vton_image": rel_vton_image,
                "storyboard_images": storyboard_images,
                "script": script,
                "storyboard_panels": len(scenes),
                "final_video_url": rel_video_url,
                "backend": combined_backend,
                "status": status,
                **({"error": error} if error else {})
            })
 
            if vton_image_url and storyboard_images and status in ("success", "partial"):
                if model_id not in cache_data:
                    cache_data[model_id] = {}
                cache_data[model_id]["vton_image"] = vton_image_url
                cache_data[model_id]["storyboard_images"] = storyboard_images
                try:
                    with open(cache_path, "w") as f:
                        json.dump(cache_data, f, indent=2)
                    print(f"  ✓ Cached VTON and storyboard image URLs for {model_name}")
                except Exception as e:
                    print(f"  → Warning: Could not write cache file: {e}")


        except Exception as e:
            print(f"  ✗ Error for {model_name}: {e}")
            traceback.print_exc()
            final_outputs.append({
                "model_id": model_id, "model_name": model_name,
                "status": "failed", "error": str(e),
            })

    if not final_outputs or all(o.get("status") == "failed" for o in final_outputs):
        raise RuntimeError(f"All models failed to generate video. Details: {final_outputs}")

    # Clear in-flight state now that everything successfully generated
    clear_in_flight_state(product_id)

    return final_outputs





if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Meshada Fashion UGC Pipeline v2")
    parser.add_argument("--product_id", type=str, required=True)
    parser.add_argument("--product_name", type=str, required=True)
    parser.add_argument("--product_description", type=str, default="")
    parser.add_argument("--product_image_url", type=str, required=True)
    parser.add_argument("--product_type", type=str, default="fashion")
    parser.add_argument("--duration", type=int, default=5)
    parser.add_argument("--bgm_url", type=str, default=DEFAULT_BGM_URL)
    parser.add_argument("--model_id", type=str, default=None)

    args = parser.parse_args()
    outputs = run_ugc_pipeline(
        product_id=args.product_id,
        product_name=args.product_name,
        product_description=args.product_description,
        product_image_url=args.product_image_url,
        product_type=args.product_type,
        duration=args.duration,
        bgm_url=args.bgm_url,
        model_id_filter=args.model_id,
    )

    print(json.dumps(outputs))
