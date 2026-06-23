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

from dotenv import load_dotenv
load_dotenv()

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
        "style_direction": "casual",
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
            "hair": "Dark brown-black, soft waves, side-parted, face-framing layers",
            "features": "Soft rounded features, bright eyes, natural smile lines",
            "expression": "Genuine warm smile, approachable, like FaceTiming a friend",
            "makeup": "Fresh minimal — dewy skin, soft blush, tinted lip balm, natural brows",
        },
        "pose": (
            "Relaxed casual stance leaning slightly against a warm-toned wall. "
            "Right hand holding phone loosely, left hand gesturing naturally as if mid-sentence. "
            "Weight on right leg, left knee bent casually. Head tilted with genuine smile, "
            "looking directly at camera like sharing a secret with a friend."
        ),
        "environment": "Warm cafe interior with golden hour light streaming through windows, wooden surfaces, potted plants, cozy lifestyle setting",
        "photography": {
            "camera": "Mirrorless camera, lifestyle photography style",
            "lens": "50mm for natural perspective",
            "aperture": "f/2.8 for dreamy background blur",
            "lighting": "Golden hour window light from right, warm color temperature, natural soft shadows",
            "composition": "Medium shot, model centered, intimate framing, negative space above",
            "angle": "Eye-level, straight-on, intimate and friendly",
        },
        "mood": "Warm, approachable, genuine, lifestyle, relatable, inviting, best-friend energy",
        "voice_style": "friendly, upbeat, relatable",
        "motion_prompt": "Young woman in cozy setting turns to camera with genuine smile, holds up product naturally, warm golden hour lighting, lifestyle vlog aesthetic, gentle camera push-in",
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
            {"name": "Detail", "duration": 2, "motion_prompt": "extreme close-up of fashion model's dress detail, slow pan highlighting fabric texture, professional fashion film"},
            {"name": "Walk", "duration": 3, "motion_prompt": "street style photography, fashion model walking towards camera in city alley, dynamic stride, confident smile"},
            {"name": "Turn", "duration": 3, "motion_prompt": "fashion model spins gracefully, dress flares out, urban background with neon lights, slow motion cinematic"}
        ],
        "outerwear": [
            {"name": "Detail", "duration": 2, "motion_prompt": "close-up of jacket texture, model zipping up jacket collar naturally, urban streetwear aesthetic"},
            {"name": "Walk", "duration": 3, "motion_prompt": "fashion model struts confidently down wet city street, street style photoshoot, camera tracks back"},
            {"name": "Outro", "duration": 3, "motion_prompt": "fashion model pulls hood over curls, turns profile looking away, street art background, handheld camera"}
        ],
        "pants": [
            {"name": "Walk", "duration": 3, "motion_prompt": "low angle camera tracking model's walking stride, showing sneakers and pants in motion, street background"},
            {"name": "Turn", "duration": 2, "motion_prompt": "fashion model spins slowly, hands in pockets, looking over shoulder at camera with confident attitude"},
            {"name": "Pose", "duration": 3, "motion_prompt": "fashion model strikes a streetwear pose leaning against concrete wall, direct confident gaze"}
        ],
        "tops": [
            {"name": "Detail", "duration": 2, "motion_prompt": "close-up of graphic tee print, model adjusts sleeves naturally, urban streetwear mood"},
            {"name": "Walk", "duration": 3, "motion_prompt": "fashion model walks casually on urban street, looking towards camera, warm street lighting"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model strikes a casual pose, smiles at camera, urban background, film grain texture"}
        ]
    },
    "editorial": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "slow cinematic tilt showing elegant dress hem to shoulders, soft luxury studio lighting, high fashion"},
            {"name": "Walk", "duration": 3, "motion_prompt": "high fashion model walks gracefully on minimal studio runway, elegant movement, slow motion 60fps"},
            {"name": "Reveal", "duration": 3, "motion_prompt": "editorial fashion pose, model turns slowly showing open back of dress, serene confidence, dramatic shadow"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "high fashion model wearing structured blazer, adjusts lapels elegantly, minimalist studio backdrop"},
            {"name": "Walk", "duration": 3, "motion_prompt": "fashion model walks on runway, dramatic lighting catches structured shoulders of coat, cinematic fashion film"},
            {"name": "Pose", "duration": 3, "motion_prompt": "editorial pose, model turns slowly, dramatic key light, high contrast shadows, haute couture aesthetic"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "editorial shot, model strikes asymmetric pose highlighting clean lines of high-waist trousers, studio lighting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walks with high-fashion posture, trousers drape beautifully in motion, studio wind machine"},
            {"name": "Pose", "duration": 3, "motion_prompt": "model turns three-quarter, looks directly into camera lens with high-fashion intensity"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "high fashion portrait, close-up of model's face and elegant silk blouse drape, soft diffused studio light"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walking slowly, silk shirt flowing, wind machine, cinematic camera movement, editorial fashion"},
            {"name": "Pose", "duration": 3, "motion_prompt": "model turns slowly, cross-armed pose, looking at camera with serene editorial expression"}
        ]
    },
    "casual": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "vlog style, model holding up dress, turning side to side with a warm friendly smile, cozy bedroom setting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walks happily in sunny garden, dress swaying naturally, handheld camera feel, warm sun flares"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model sits on cafe bench, turns to camera, laughs and waves, bright inviting lifestyle aesthetic"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "cozy lifestyle vlog, model wearing warm cardigan, wraps arms around herself smiling, coffee shop background"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walks down suburban path holding cup of tea, autumn leaves falling, casual friendly energy"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model turns, smiles and points at cardigan texture, warm cozy lighting, lifestyle vlog feel"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "casual mirror selfie style, model showing comfy jeans fit, tilting hips naturally, warm bedroom light"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walks towards camera in sunny park, smiling, casual approach, natural handheld movement"},
            {"name": "Pose", "duration": 3, "motion_prompt": "model leans against park bench, turns to camera and waves, happy friendly best-friend vibes"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "cozy vlog intro, model waves to camera wearing cute casual tee, holds up tea mug, bright cozy cafe"},
            {"name": "Walk", "duration": 3, "motion_prompt": "model walks casually in cafe, turns around smiling, lifestyle vlog aesthetic, dewy skin glow"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model laughs, waves goodbye to camera, cozy environment, soft golden hour lighting"}
        ]
    },
    "curvy-chic": {
        "dress": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy fashion model turns slowly in minimalist studio, showing hourglass fit of dress, warm golden lighting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks confidently towards camera, dress drape emphasizes curves beautifully, slow cinematic push-in"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model strikes confident pose, hand on hip, looks over shoulder with warm gorgeous smile, studio wall"}
        ],
        "outerwear": [
            {"name": "Intro", "duration": 2, "motion_prompt": "plus-size model wearing tailored blazer, adjusts front button, confident chic styling studio shot"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks down city street, open coat flows in wind, camera follows her movement with dynamic tracking"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model pauses, looks back over shoulder, shrugs coat slightly, gorgeous smile, soft bokeh background"}
        ],
        "pants": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy model waist-down profile pose, highlighting fit of high-waist jeans, hand on back pocket naturally"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy fashion model struts confidently, showing jeans fit in motion, camera at low angle tracking stride"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model turns 360 degrees showing full jeans fit, finishes with confident hand-on-hip pose, warm smile"}
        ],
        "tops": [
            {"name": "Intro", "duration": 2, "motion_prompt": "curvy model styling vlog, adjusts neckline of elegant top, smiles confidently, warm studio lighting"},
            {"name": "Walk", "duration": 3, "motion_prompt": "curvy model walks in modern studio, top moves naturally with her stride, soft lifestyle camera follow"},
            {"name": "Outro", "duration": 3, "motion_prompt": "model strikes a confident pose, turns side profile then smiles at camera, chic fashion presentation"}
        ]
    }
}

def get_storyboard_scenes(style_direction: str, product_category: str) -> list:
    style_dict = SCENE_TEMPLATES.get(style_direction, SCENE_TEMPLATES["casual"])
    # Fallback to "tops" if category not found in the style
    category_scenes = style_dict.get(product_category, style_dict.get("tops"))
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


def generate_video_script(product_name: str, model_name: str) -> str:
    """Generate a short voiceover/caption script for the video."""
    scripts = {
        "Aria": f"Obsessed with this {product_name}. The fit is absolutely flawless. Tap the link to shop my exact look.",
        "Luna": f"Streetwear essential unlocked 🔥 This {product_name} is everything. Check the link to cop this fit.",
        "Nova": f"Hey! I just found the cutest {product_name} ever. It's so good, I had to share. Link in bio! 💕",
        "Sasha": f"Curvy-chic style check ✨ This {product_name} hugs every single curve perfectly. Link to shop is in my bio!",
    }
    return scripts.get(model_name, f"I'm loving this {product_name}. Shop it now through the link!")


# ─── EachLabs VTON Integration ──────────────────────────────────────────────────

def _eachlabs_predict(model_slug: str, input_data: dict) -> dict:
    """Submit prediction to EachLabs and poll until complete."""
    api_key = os.getenv("EACHLABS_API_KEY")
    headers = {"Content-Type": "application/json", "X-API-Key": api_key}

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


def vton_eachlabs(product_image_url: str, model_data: dict) -> str:
    """Virtual try-on using EachLabs Kolors VTON — puts real product on AI model."""
    print(f"  → Virtual Try-On via EachLabs ({EACHLABS_VTON_MODEL})...")
    
    result = _eachlabs_predict(EACHLABS_VTON_MODEL, {
        "garment_image_url": product_image_url,
        "human_image_url": model_data["human_image_url"],
    })

    # Extract output URL
    output = result.get("output", {})
    if isinstance(output, dict):
        return output.get("image_url", "") or output.get("url", "")
    elif isinstance(output, str):
        return output
    return ""


def video_eachlabs(image_url: str, motion_prompt: str, duration: int = 5) -> str:
    """Generate runway video using EachLabs Pixverse."""
    print(f"  → Video generation via EachLabs ({EACHLABS_VIDEO_MODEL})...")

    result = _eachlabs_predict(EACHLABS_VIDEO_MODEL, {
        "image_url": image_url,
        "prompt": motion_prompt,
        "duration": str(duration),
        "resolution": "1080p",
    })

    output = result.get("output", {})
    if isinstance(output, dict):
        return output.get("video_url", "") or output.get("url", "")
    elif isinstance(output, str):
        return output
    return ""


# ─── Higgsfield Integration ────────────────────────────────────────────────────

def image_higgsfield(prompt: str) -> str:
    """Generate image using Higgsfield Soul."""
    print(f"  → Image generation via Higgsfield Soul...")
    result = higgsfield_client.subscribe(
        HF_IMAGE_MODEL,
        arguments={"prompt": prompt, "aspect_ratio": "9:16", "resolution": "720p"},
    )
    images = result.get("images", [])
    return images[0].get("url", "") if images else ""


def video_higgsfield(image_url: str, motion_prompt: str, duration: int = 5) -> str:
    """Generate video using Higgsfield DoP."""
    print(f"  → Video generation via Higgsfield DoP...")
    result = higgsfield_client.subscribe(
        HF_VIDEO_MODEL,
        arguments={"image_url": image_url, "prompt": motion_prompt, "duration": duration},
    )
    video = result.get("video", {})
    if video and video.get("url"):
        return video["url"]
    images = result.get("images", [])
    return images[0].get("url", "") if images else ""


# ─── Main Pipeline ──────────────────────────────────────────────────────────────

def run_ugc_pipeline(
    product_id: str,
    product_name: str,
    product_description: str,
    product_image_url: str,
    product_type: str,
    duration: int = 5,
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
        print("[WARN] No API credentials set. Returning mock data.")
        print("[WARN] Set EACHLABS_API_KEY for VTON or HF_KEY for Higgsfield.")
        return _mock_outputs(product_id, product_name)

    # Hybrid configuration logic
    img_backend = "eachlabs" if has_eachlabs else "higgsfield"
    video_backend = "higgsfield" if has_higgsfield else "eachlabs"
    combined_backend = f"{img_backend}+{video_backend}"

    print(f"\n{'='*60}")
    print(f"  Meshada UGC Pipeline v2 — Image: {img_backend.upper()} | Video: {video_backend.upper()}")
    print(f"  Product: {product_name}")
    print(f"{'='*60}")

    # Detect product category
    category = detect_product_category(product_name, product_description)
    print(f"  Detected Category: {category.upper()}")

    final_outputs = []

    for model_id, model_data in AI_MODELS.items():
        model_name = model_data["name"]
        style_dir = model_data["style_direction"]
        print(f"\n{'─'*60}")
        print(f"  Persona: {model_name} ({style_dir})")
        print(f"{'─'*60}")

        try:
            # ── Step 1: Generate model image (VTON or text-to-image) ──
            if img_backend == "eachlabs":
                print(f"[1/4] Virtual Try-On — putting real product on {model_name}...")
                vton_image_url = vton_eachlabs(product_image_url, model_data)
            else:
                print(f"[1/4] Fashion photography generation for {model_name}...")
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
            scenes = get_storyboard_scenes(style_dir, category)
            print(f"[2/4] Storyboard planning ({len(scenes)} scenes)...")
            for idx, sc in enumerate(scenes):
                print(f"    Scene {idx+1} ({sc['name']}): {sc['motion_prompt'][:65]}... ({sc['duration']}s)")

            # ── Step 3: Script generation ──
            script = generate_video_script(product_name, model_name)
            print(f"[3/4] Script: \"{script[:60]}...\"")

            # ── Step 4: Multi-scene Video generation & stitching ──
            print(f"[4/4] Generating and stitching scenes...")
            clip_paths = []
            
            # Ensure output directory exists
            os.makedirs("output_assets", exist_ok=True)
            
            success_count = 0
            for idx, sc in enumerate(scenes):
                print(f"  → Animating Scene {idx+1}/{len(scenes)} ({sc['name']})...")
                scene_prompt = sc["motion_prompt"]
                scene_dur = sc["duration"]
                
                # Request video generation from the backend
                if video_backend == "eachlabs":
                    scene_video_url = video_eachlabs(vton_image_url, scene_prompt, scene_dur)
                else:
                    scene_video_url = video_higgsfield(vton_image_url, scene_prompt, scene_dur)
                
                if scene_video_url:
                    print(f"    ✓ Scene {idx+1} video URL: {scene_video_url[:80]}...")
                    # Download the clip locally
                    clip_file = f"output_assets/temp_{product_id}_{model_name}_scene_{idx+1}.mp4"
                    if download_file(scene_video_url, clip_file):
                        clip_paths.append(clip_file)
                        success_count += 1
                else:
                    print(f"    ✗ Scene {idx+1} generation returned no URL")
            
            # Stitch the videos together using FFmpeg
            final_video_file = f"output_assets/prod_{product_id}_{model_name}_final.mp4"
            
            if success_count == len(scenes):
                # All scenes generated and downloaded successfully, stitch them!
                if concat_videos_ffmpeg(clip_paths, final_video_file):
                    print(f"  ✓ Multi-scene stitched video created at: {final_video_file}")
                    
                    # Clean up temporary single-shot clips
                    for cp in clip_paths:
                        try:
                            os.remove(cp)
                        except:
                            pass
                    
                    final_outputs.append({
                        "model_id": model_id,
                        "model_name": model_name,
                        "style": style_dir,
                        "vton_image": vton_image_url,
                        "script": script,
                        "storyboard_panels": len(scenes),
                        "final_video_url": final_video_file,
                        "backend": combined_backend,
                        "status": "success",
                    })
                else:
                    # Stitch failed
                    print("  ✗ FFmpeg stitching failed. Falling back to the first clip.")
                    final_outputs.append({
                        "model_id": model_id,
                        "model_name": model_name,
                        "style": style_dir,
                        "vton_image": vton_image_url,
                        "script": script,
                        "final_video_url": clip_paths[0] if clip_paths else vton_image_url,
                        "backend": combined_backend,
                        "status": "partial",
                        "error": "FFmpeg stitching failed"
                    })
            elif success_count > 0:
                # Some scenes succeeded but not all. Stitch what we have!
                print(f"  ⚠ Only {success_count}/{len(scenes)} scenes succeeded. Stitching partial scenes...")
                if concat_videos_ffmpeg(clip_paths, final_video_file):
                    # Clean up temp files
                    for cp in clip_paths:
                        try:
                            os.remove(cp)
                        except:
                            pass
                    final_outputs.append({
                        "model_id": model_id,
                        "model_name": model_name,
                        "style": style_dir,
                        "vton_image": vton_image_url,
                        "script": script,
                        "final_video_url": final_video_file,
                        "backend": combined_backend,
                        "status": "partial",
                    })
                else:
                    final_outputs.append({
                        "model_id": model_id,
                        "model_name": model_name,
                        "style": style_dir,
                        "vton_image": vton_image_url,
                        "script": script,
                        "final_video_url": clip_paths[0] if clip_paths else vton_image_url,
                        "backend": combined_backend,
                        "status": "partial",
                    })
            else:
                # No scenes succeeded, use VTON image as fallback
                print("  ✗ All scene generations failed. Using image as fallback.")
                final_outputs.append({
                    "model_id": model_id,
                    "model_name": model_name,
                    "style": style_dir,
                    "vton_image": vton_image_url,
                    "script": script,
                    "final_video_url": vton_image_url,
                    "backend": combined_backend,
                    "status": "partial",
                    "error": "All video scenes failed"
                })

        except Exception as e:
            print(f"  ✗ Error for {model_name}: {e}")
            traceback.print_exc()
            final_outputs.append({
                "model_id": model_id, "model_name": model_name,
                "status": "failed", "error": str(e),
            })

    if not final_outputs or all(o.get("status") == "failed" for o in final_outputs):
        print("\n[FALLBACK] All models failed. Returning mock data.")
        return _mock_outputs(product_id, product_name)

    return final_outputs


def _mock_outputs(product_id: str, product_name: str) -> list:
    """Fallback mock data when APIs are unavailable."""
    return [
        {
            "model_id": "model_1", "model_name": "Aria", "style": "editorial",
            "vton_image": "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400",
            "script": f"Obsessed with this {product_name}. The fit is absolutely flawless.",
            "final_video_url": "https://www.w3schools.com/html/mov_bbb.mp4",
            "backend": "mock", "status": "mock",
        },
        {
            "model_id": "model_2", "model_name": "Luna", "style": "streetwear",
            "vton_image": "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=400",
            "script": f"Streetwear essential unlocked 🔥 This {product_name} is everything.",
            "final_video_url": "https://www.w3schools.com/html/mov_bbb.mp4",
            "backend": "mock", "status": "mock",
        },
        {
            "model_id": "model_3", "model_name": "Nova", "style": "casual",
            "vton_image": "https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=400",
            "script": f"Hey! I just found the cutest {product_name} ever. Link in bio! 💕",
            "final_video_url": "https://www.w3schools.com/html/mov_bbb.mp4",
            "backend": "mock", "status": "mock",
        },
    ]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Meshada Fashion UGC Pipeline v2")
    parser.add_argument("--product_id", type=str, required=True)
    parser.add_argument("--product_name", type=str, required=True)
    parser.add_argument("--product_description", type=str, default="")
    parser.add_argument("--product_image_url", type=str, required=True)
    parser.add_argument("--product_type", type=str, default="fashion")
    parser.add_argument("--duration", type=int, default=5)

    args = parser.parse_args()
    outputs = run_ugc_pipeline(
        product_id=args.product_id,
        product_name=args.product_name,
        product_description=args.product_description,
        product_image_url=args.product_image_url,
        product_type=args.product_type,
        duration=args.duration,
    )
    print(json.dumps(outputs))
