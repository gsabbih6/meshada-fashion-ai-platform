import os
import tempfile
import json
import pytest
from unittest.mock import patch, mock_open
from orchestrator import (
    detect_product_category,
    get_storyboard_scenes,
    build_fashion_prompt,
    generate_video_script,
    get_state_path,
    load_in_flight_state,
    save_in_flight_state,
    clear_in_flight_state,
    AI_MODELS,
    get_absolute_path,
    download_file,
)

def test_detect_product_category():
    # Test dress category
    assert detect_product_category("Beautiful Evening Dress", "A long silk gown") == "dress"
    assert detect_product_category("Floral Maxi", "Summer wear") == "dress"
    
    # Test outerwear category
    assert detect_product_category("Leather Jacket", "Black biker outerwear") == "outerwear"
    assert detect_product_category("Winter Coat", "Heavy warm cardigan") == "outerwear"
    
    # Test pants category
    assert detect_product_category("Denim Jeans", "Slim fit trousers") == "pants"
    assert detect_product_category("Cargo Shorts", "Casual cotton pants") == "pants"
    
    # Test shoes category
    assert detect_product_category("Running Shoes", "Athletic sneakers") == "shoes"
    
    # Test accessories category
    assert detect_product_category("Leather Handbag", "Stylish shoulder bag") == "accessories"
    
    # Test default tops category
    assert detect_product_category("Graphic Tee", "100% cotton casual shirt") == "tops"

def test_get_storyboard_scenes():
    # Test streetwear tops
    scenes = get_storyboard_scenes("streetwear", "tops")
    assert len(scenes) > 0
    assert scenes[0]["name"] == "Detail"
    
    # Test editorial dress
    scenes_editorial = get_storyboard_scenes("editorial", "dress")
    assert len(scenes_editorial) > 0
    
    # Test invalid category fallback (defaults to tops)
    scenes_invalid = get_storyboard_scenes("streetwear", "invalid_cat")
    assert len(scenes_invalid) > 0

def test_build_fashion_prompt():
    model_data = AI_MODELS["model_2"]
    prompt = build_fashion_prompt(model_data, "Red Dress", "Silk party wear")
    assert "Red Dress" in prompt
    assert "Silk party wear" in prompt
    assert "luna" in prompt.lower() or "female" in prompt.lower()

def test_generate_video_script():
    script = generate_video_script("Leather Jacket", "Aria", "streetwear", "outerwear")
    assert "Leather Jacket" in script
    assert any(keyword in script for keyword in ["FIT", "LINK", "STYLE"])

def test_state_management():
    product_id = "test_prod_123"
    
    # Ensure state file clean start
    clear_in_flight_state(product_id)
    
    # Load empty state
    empty_state = load_in_flight_state(product_id)
    assert empty_state == {}
    
    # Save state
    state_data = {"current_step": "vton", "task_id": "job_abc"}
    save_in_flight_state(product_id, state_data)
    
    # Load and verify
    loaded_state = load_in_flight_state(product_id)
    assert loaded_state == state_data
    
    # Clear state
    clear_in_flight_state(product_id)
    assert load_in_flight_state(product_id) == {}

def test_get_absolute_path():
    path = get_absolute_path("test.txt")
    assert os.path.isabs(path)
    assert path.endswith("test.txt")

@patch("requests.get")
def test_download_file_success(mock_get):
    mock_resp = mock_get.return_value
    mock_resp.status_code = 200
    mock_resp.iter_content.return_value = [b"chunk1", b"chunk2"]
    
    with tempfile.NamedTemporaryFile(delete=True) as tmp:
        success = download_file("https://example.com/video.mp4", tmp.name)
        assert success
        with open(tmp.name, "rb") as f:
            content = f.read()
            assert content == b"chunk1chunk2"

@patch("requests.get")
def test_download_file_failure(mock_get):
    mock_get.side_effect = Exception("Connection error")
    
    with tempfile.NamedTemporaryFile(delete=True) as tmp:
        success = download_file("https://example.com/video.mp4", tmp.name)
        assert not success
