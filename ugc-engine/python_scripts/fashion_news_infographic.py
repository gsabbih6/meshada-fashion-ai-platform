#!/usr/bin/env python3
"""
Meshada Fashion News Graphic Generator - Carousel Upgrade.
Fetches recent fashion headlines, selects and summarizes the most viral story using OpenAI,
stylizes the image, and generates a 3-slide social media carousel using Pillow.
"""

import os
import sys
import time
import json
import argparse
import random
import requests
import urllib.request
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont, ImageOps
from dotenv import load_dotenv

# Load env variables with robust path resolution
current_dir = os.path.dirname(os.path.abspath(__file__))
env_paths = [
    os.path.join(current_dir, ".env"),
    os.path.join(current_dir, "..", ".env"),
    os.path.join(current_dir, "..", "..", ".env")
]
for path in env_paths:
    if os.path.exists(path):
        load_dotenv(path, override=True)

EACHLABS_BASE_URL = "https://api.eachlabs.ai/v1"
EACHLABS_STYLE_MODEL = "wan-v2-6-image-to-image"

# Default RSS feeds to poll
FASHION_FEEDS = [
    "https://www.vogue.com/feed/rss",
    "https://fashionista.com/.rss/full/"
]

FALLBACK_IMAGE_URL = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=1080" # High-end fashion photo
SECONDARY_IMAGE_URL = "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1080" # Alternate high-end fashion photo
CTA_IMAGE_URL = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1080" # Alternate clothing rack photo

def fetch_rss_stories():
    stories = []
    print("[RSS] Fetching fashion news feeds...")
    for url in FASHION_FEEDS:
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as response:
                xml_data = response.read()
            
            root = ET.fromstring(xml_data)
            for item in root.findall(".//item"):
                title = item.find("title")
                link = item.find("link")
                description = item.find("description")
                
                title_text = title.text if title is not None else ""
                link_text = link.text if link is not None else ""
                desc_text = description.text if description is not None else ""
                
                # Try to extract an image URL
                img_url = None
                
                # Check media:content or media:thumbnail
                for child in item:
                    if 'content' in child.tag or 'thumbnail' in child.tag:
                        url_attr = child.attrib.get('url')
                        if url_attr:
                            img_url = url_attr
                            break
                            
                # Check description HTML for img tag
                if not img_url and desc_text:
                    if "<img" in desc_text:
                        try:
                            start = desc_text.find('src="') + 5
                            end = desc_text.find('"', start)
                            if start > 4 and end > start:
                                img_url = desc_text[start:end]
                        except:
                            pass
                
                # If still no image, check enclosure
                enclosure = item.find("enclosure")
                if not img_url and enclosure is not None:
                    img_url = enclosure.attrib.get("url")

                if title_text and link_text:
                    stories.append({
                        "title": title_text,
                        "link": link_text,
                        "description": desc_text[:300],
                        "image_url": img_url
                    })
        except Exception as e:
            print(f"[RSS] Warning: Failed to parse feed {url}: {e}")
            
    print(f"[RSS] Sourced {len(stories)} articles.")
    return stories

def select_and_summarize_news(stories, dry_run=False):
    if dry_run or not os.getenv("OPENAI_API_KEY") or not stories:
        print("[LLM] Running in mock/dry-run mode for news selection.")
        return {
            "selected_headline": "ZENDAYA STUNS ON THE RUNWAY IN A VINTAGE 1998 ARCHIVAL GOWN AND FANS ARE GOING WILD",
            "slide2_text": "Zendaya made a surprise appearance at a fashion gala in Paris wearing a rare archival gown from the 1998 collection.",
            "slide3_text": "The stunning look sparked a massive increase in searches for vintage slip dresses. Tap below to shop the aesthetic!",
            "social_caption": "Archival fashion is officially having a moment. Zendaya just proved that vintage is the ultimate luxury. 👑 Swipe to see details!",
            "original_link": "https://www.vogue.com/article/celebrity-fashion-trends",
            "original_title": "Zendaya's Vintage Archival Runway Gown in Paris",
            "image_url": FALLBACK_IMAGE_URL,
            "secondary_image_url": SECONDARY_IMAGE_URL,
            "styling_prompt": "Zendaya walking on a runway in a rare vintage slip dress, high-end editorial studio fashion photoshoot, soft studio lighting"
        }

    from openai import OpenAI
    client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

    sample_stories = stories[:15]
    prompt_stories = []
    for idx, s in enumerate(sample_stories):
        prompt_stories.append(f"[{idx}] TITLE: {s['title']}\nLINK: {s['link']}\nDESC: {s['description']}\nIMAGE: {s['image_url'] or 'None'}")

    system_prompt = (
        "You are an elite fashion editor for Meshada Fashion. Your goal is to review a list of recent fashion news stories "
        "and select the single most viral, high-converting story (celebrity outfits, budget dupes, viral fashion items). "
        "Divide the selected story into a 3-slide social media outline:\n"
        "1. selected_headline: A punchy 1-line uppercase headline (LADbible style, e.g. 'FANS ARE OBSESSED WITH DUA LIPA\\'S NEW BEACH STYLE').\n"
        "2. slide2_text: The main details of what happened (1-2 sentences, max 20 words).\n"
        "3. slide3_text: A call to action with a takeaway (e.g. 'Tap below to shop similar styles or read the full story.').\n"
        "Return your response ONLY as a JSON object with keys: "
        "'selected_headline', 'slide2_text', 'slide3_text', 'social_caption', 'original_link', 'original_title', 'image_url', 'styling_prompt'."
    )

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"Select the best fashion news from this list:\n\n" + "\n\n".join(prompt_stories)}
            ]
        )
        result = json.loads(response.choices[0].message.content)
        
        selected_link = result.get("original_link")
        matched = False
        for s in sample_stories:
            if s["link"] == selected_link or s["title"] in result.get("original_title", ""):
                result["original_link"] = s["link"]
                result["original_title"] = s["title"]
                if s["image_url"]:
                    result["image_url"] = s["image_url"]
                matched = True
                break
                
        if not result.get("image_url") or result["image_url"] == "None":
            result["image_url"] = FALLBACK_IMAGE_URL

        # Add default secondary/CTA links
        result["secondary_image_url"] = SECONDARY_IMAGE_URL
        print(f"[LLM] Selected Story: {result.get('original_title')}")
        return result
    except Exception as e:
        print(f"[LLM] Error selecting news story: {e}. Falling back to direct RSS story parsing.")
        if stories:
            import re
            first_story = stories[0]
            desc_clean = re.sub('<[^<]+?>', '', first_story["description"]).strip()
            desc_words = desc_clean.split()
            
            # Split description into slide 2 and 3
            slide2 = " ".join(desc_words[:20])
            slide3 = " ".join(desc_words[20:40])
            if len(desc_words) > 40:
                slide3 += "..."
            else:
                slide3 += " Tap below to read more."
                
            headline = first_story["title"].strip()
            if " - " in headline:
                headline = headline.split(" - ")[0]
            elif " | " in headline:
                headline = headline.split(" | ")[0]
                
            return {
                "selected_headline": headline.upper(),
                "slide2_text": slide2,
                "slide3_text": slide3,
                "social_caption": f"Breaking: {headline} 💅 Swipe to read details & click below for full story!",
                "original_link": first_story["link"],
                "original_title": first_story["title"],
                "image_url": first_story["image_url"] if first_story["image_url"] else FALLBACK_IMAGE_URL,
                "secondary_image_url": SECONDARY_IMAGE_URL,
                "styling_prompt": "High-end editorial studio fashion photoshoot style, premium brand look"
            }
        else:
            return select_and_summarize_news(None, dry_run=True)

def download_temp_image(url, suffix=""):
    try:
        temp_path = os.path.join("/tmp", f"news_temp_{suffix}_{int(time.time())}.jpg")
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=15) as response:
            with open(temp_path, "wb") as f:
                f.write(response.read())
        return temp_path
    except Exception as e:
        print(f"[Image Download] Error downloading image {url}: {e}")
        return None

def wrap_text(text, font, max_width):
    words = text.split()
    lines = []
    current_line = []
    for word in words:
        test_line = ' '.join(current_line + [word])
        bbox = font.getbbox(test_line)
        width = bbox[2] - bbox[0]
        if width <= max_width:
            current_line.append(word)
        else:
            if current_line:
                lines.append(' '.join(current_line))
            current_line = [word]
    if current_line:
        lines.append(' '.join(current_line))
    return lines

def load_custom_font(font_size):
    # Try local macOS system paths first
    for path in ["/System/Library/Fonts/Supplemental/Impact.ttf", "/System/Library/Fonts/Supplemental/Arial Bold.ttf"]:
        if os.path.exists(path):
            return ImageFont.truetype(path, font_size)
            
    # Try standard Linux paths
    for path in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"]:
        if os.path.exists(path):
            return ImageFont.truetype(path, font_size)
            
    # Download Roboto-Bold if not present
    local_font = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Roboto-Bold.ttf")
    if not os.path.exists(local_font):
        try:
            print("[Font] Downloading Roboto-Bold.ttf from Google Fonts...")
            url = "https://github.com/google/fonts/raw/main/apache/roboto/static/Roboto-Bold.ttf"
            urllib.request.urlretrieve(url, local_font)
            print("[Font] Download complete.")
        except Exception as e:
            print(f"[Font] Warning: Failed to download font: {e}")
            
    if os.path.exists(local_font):
        return ImageFont.truetype(local_font, font_size)
        
    return ImageFont.load_default()

def composite_slide1_hook(image_path, headline_text, output_path):
    print(f"[Pillow] Compositing Slide 1: Hook from: {image_path}")
    try:
        img = Image.open(image_path).convert("RGBA")
        target_w, target_h = 1080, 1350
        img = ImageOps.fit(img, (target_w, target_h), Image.Resampling.LANCZOS)
        
        overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
        
        # Bottom gradient fade
        gradient_start_y = 800
        for y in range(gradient_start_y, target_h):
            alpha = int(230 * (y - gradient_start_y) / (target_h - gradient_start_y))
            draw.line([(0, y), (target_w, y)], fill=(0, 0, 0, alpha))
            
        # Semi-transparent MESHADA FASHION logo box in top-right
        logo_box = [800, 50, 1030, 110]
        draw.rounded_rectangle(logo_box, radius=5, fill=(255, 255, 255, 120))
        
        logo_font = load_custom_font(20)
        logo_text = "MESHADA"
        
        try:
            l_bbox = logo_font.getbbox(logo_text)
            l_w, l_h = l_bbox[2] - l_bbox[0], l_bbox[3] - l_bbox[1]
            logo_x = logo_box[0] + (logo_box[2] - logo_box[0] - l_w) / 2
            logo_y = logo_box[1] + (logo_box[3] - logo_box[1] - l_h) / 2 - 2
            draw.text((logo_x, logo_y), logo_text, font=logo_font, fill=(0, 0, 0, 220))
        except Exception:
            draw.text((820, 70), logo_text, fill=(0, 0, 0, 220))
            
        font_size = 56
        font = load_custom_font(font_size)
        
        headline_upper = headline_text.upper()
        max_text_width = 920
        lines = wrap_text(headline_upper, font, max_text_width)
        
        margin_left = 80
        start_y = target_h - 100 - (len(lines) * (font_size + 15))
        
        current_y = start_y
        for line in lines:
            draw.text((margin_left, current_y), line, font=font, fill=(255, 255, 255, 255))
            current_y += font_size + 15
            
        final_img = Image.alpha_composite(img, overlay).convert("RGB")
        final_img.save(output_path, "JPEG", quality=95)
        return True
    except Exception as e:
        print(f"[Pillow] Slide 1 Error: {e}")
        return False

def composite_slide2_story(image_path, story_text, output_path):
    print(f"[Pillow] Compositing Slide 2: Story Details from: {image_path}")
    return composite_slide1_hook(image_path, story_text, output_path)

def composite_slide3_cta(image_path, cta_text, output_path):
    print(f"[Pillow] Compositing Slide 3: CTA from: {image_path}")
    return composite_slide1_hook(image_path, cta_text, output_path)

def main():
    parser = argparse.ArgumentParser(description="Meshada Fashion News Carousel Generator")
    parser.add_argument("--dry-run", action="store_true", help="Run with mock data, skip API calls")
    parser.add_argument("--feed-url", type=str, help="Optional direct RSS feed url")
    parser.add_argument("--skimlinks-id", type=str, default="12345X67890", help="Skimlinks Publisher ID")
    args = parser.parse_args()

    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output_assets")
    os.makedirs(output_dir, exist_ok=True)

    if args.feed_url:
        global FASHION_FEEDS
        FASHION_FEEDS = [args.feed_url]
        
    stories = []
    if not args.dry_run:
        stories = fetch_rss_stories()
        
    news_data = select_and_summarize_news(stories, dry_run=args.dry_run)

    # Download Slide 1 Image
    local_image1 = download_temp_image(news_data["image_url"], "slide1")
    if not local_image1:
        local_image1 = download_temp_image(FALLBACK_IMAGE_URL, "slide1")

    # Download Slide 2 Image
    local_image2 = download_temp_image(news_data["secondary_image_url"], "slide2")
    if not local_image2:
        local_image2 = download_temp_image(SECONDARY_IMAGE_URL, "slide2")

    # Download Slide 3 Image
    local_image3 = download_temp_image(CTA_IMAGE_URL, "slide3")
    if not local_image3:
        local_image3 = download_temp_image(CTA_IMAGE_URL, "slide3")

    # Composite Carousel Slides
    timestamp = int(time.time())
    
    fn_slide1 = f"news_post_{timestamp}_slide1.jpg"
    fn_slide2 = f"news_post_{timestamp}_slide2.jpg"
    fn_slide3 = f"news_post_{timestamp}_slide3.jpg"
    
    path_slide1 = os.path.join(output_dir, fn_slide1)
    path_slide2 = os.path.join(output_dir, fn_slide2)
    path_slide3 = os.path.join(output_dir, fn_slide3)
    
    success1 = composite_slide1_hook(local_image1, news_data["selected_headline"], path_slide1)
    success2 = composite_slide2_story(local_image2, news_data["slide2_text"], path_slide2)
    success3 = composite_slide3_cta(local_image3, news_data["slide3_text"], path_slide3)
    
    # Clean up temp files
    for temp_img in [local_image1, local_image2, local_image3]:
        if temp_img:
            try:
                os.remove(temp_img)
            except:
                pass

    success = success1 and success2 and success3

    original_url = news_data.get("original_link")
    monetized_url = f"https://go.redirectingat.com/?id={args.skimlinks_id}&url={original_url}"

    response_payload = {
        "status": "success" if success else "failed",
        "headline": news_data.get("selected_headline"),
        "slide2_text": news_data.get("slide2_text"),
        "slide3_text": news_data.get("slide3_text"),
        "social_caption": news_data.get("social_caption"),
        "original_url": original_url,
        "monetized_url": monetized_url,
        "original_title": news_data.get("original_title"),
        "final_graphic_urls": [
            f"/output_assets/{fn_slide1}",
            f"/output_assets/{fn_slide2}",
            f"/output_assets/{fn_slide3}"
        ] if success else [],
        "local_file_paths": [
            path_slide1,
            path_slide2,
            path_slide3
        ] if success else []
    }

    print("\n--- JSON OUTPUT START ---")
    print(json.dumps(response_payload))
    print("--- JSON OUTPUT END ---")

    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
