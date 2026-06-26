import os
import sys
import tempfile
import json
import pytest
import subprocess
from unittest.mock import patch, MagicMock, mock_open

# Mock higgsfield_client before importing orchestrator so import doesn't fail or side-effect
sys.modules['higgsfield_client'] = MagicMock()

import orchestrator
from orchestrator import (
    detect_product_category,
    get_storyboard_scenes,
    build_fashion_prompt,
    generate_video_script,
    get_state_path,
    load_in_flight_state,
    save_in_flight_state,
    clear_in_flight_state,
    get_absolute_path,
    download_file,
    concat_videos_ffmpeg,
    generate_voiceover,
    mix_audio,
    _eachlabs_predict,
    vton_eachlabs,
    image_to_image_eachlabs,
    video_eachlabs,
    image_higgsfield,
    video_higgsfield,
    assemble_ugc_video,
    run_ugc_pipeline,
    AI_MODELS,
    build_storyboard_panels,
    update_in_flight_state,
)

def test_detect_product_category():
    assert detect_product_category("Beautiful Evening Dress", "A long silk gown") == "dress"
    assert detect_product_category("Leather Jacket", "Black biker outerwear") == "outerwear"
    assert detect_product_category("Denim Jeans", "Slim fit trousers") == "pants"
    assert detect_product_category("Running Shoes", "Athletic sneakers") == "shoes"
    assert detect_product_category("Leather Handbag", "Stylish accessory belt") == "accessories"
    assert detect_product_category("Graphic Tee", "100% cotton casual shirt") == "tops"

def test_get_storyboard_scenes():
    scenes = get_storyboard_scenes("streetwear", "tops")
    assert len(scenes) > 0
    assert scenes[0]["name"] == "Detail"
    
    scenes_editorial = get_storyboard_scenes("editorial", "dress")
    assert len(scenes_editorial) > 0
    
    scenes_invalid = get_storyboard_scenes("streetwear", "invalid_cat")
    assert len(scenes_invalid) > 0

def test_build_fashion_prompt():
    model_data = AI_MODELS["model_2"]
    prompt = build_fashion_prompt(model_data, "Red Dress", "Silk party wear")
    assert "Red Dress" in prompt
    assert "Silk party wear" in prompt

def test_generate_video_script():
    script = generate_video_script("Leather Jacket", "Aria", "streetwear", "outerwear")
    assert "Leather Jacket" in script
    assert any(keyword in script for keyword in ["FIT", "LINK", "STYLE"])

def test_state_management():
    product_id = "test_prod_123"
    clear_in_flight_state(product_id)
    assert load_in_flight_state(product_id) == {}
    
    state_data = {"current_step": "vton", "task_id": "job_abc"}
    save_in_flight_state(product_id, state_data)
    assert load_in_flight_state(product_id) == state_data
    
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

@patch("subprocess.run")
def test_concat_videos_ffmpeg_success(mock_run):
    mock_run.return_value.returncode = 0
    success = concat_videos_ffmpeg(["clip1.mp4", "clip2.mp4"], "output.mp4")
    assert success

@patch("subprocess.run")
def test_concat_videos_ffmpeg_fallback(mock_run):
    # First run fails, second run succeeds
    first_res = MagicMock()
    first_res.returncode = 1
    second_res = MagicMock()
    second_res.returncode = 0
    mock_run.side_effect = [first_res, second_res]
    
    success = concat_videos_ffmpeg(["clip1.mp4", "clip2.mp4"], "output.mp4")
    assert success

@patch("subprocess.run")
def test_concat_videos_ffmpeg_failure(mock_run):
    mock_run.return_value.returncode = 1
    success = concat_videos_ffmpeg(["clip1.mp4", "clip2.mp4"], "output.mp4")
    assert not success

@patch("requests.post")
@patch("os.getenv")
def test_generate_voiceover_elevenlabs_success(mock_env, mock_post):
    mock_env.return_value = "fake_key"
    mock_resp = mock_post.return_value
    mock_resp.status_code = 200
    mock_resp.content = b"fake_mp3"
    
    with tempfile.NamedTemporaryFile(delete=True) as tmp:
        success = generate_voiceover("Hello", "Luna", tmp.name)
        assert success
        with open(tmp.name, "rb") as f:
            assert f.read() == b"fake_mp3"

@patch("requests.post")
@patch("os.getenv")
@patch("edge_tts.Communicate")
@patch("asyncio.run")
def test_generate_voiceover_fallback_success(mock_run, mock_comm, mock_env, mock_post):
    mock_env.return_value = None # No ElevenLabs key -> fallback
    
    # Mock edge_tts save async function
    mock_save = MagicMock()
    mock_comm.return_value.save = mock_save
    
    with tempfile.NamedTemporaryFile(delete=True) as tmp:
        success = generate_voiceover("Hello", "Luna", tmp.name)
        assert success

@patch("os.path.exists")
@patch("subprocess.run")
def test_mix_audio_both_success(mock_run, mock_exists):
    mock_exists.return_value = True # BGM and Voiceover files exist
    mock_run.return_value.returncode = 0
    
    success = mix_audio("silent.mp4", "voice.mp3", "bgm.mp3", "output.mp4")
    assert success
    assert mock_run.call_count == 1

@patch("os.path.exists")
@patch("subprocess.run")
def test_mix_audio_bgm_only_success(mock_run, mock_exists):
    # BGM exists, Voiceover doesn't
    mock_exists.side_effect = lambda path: "bgm.mp3" in path
    mock_run.return_value.returncode = 0
    
    success = mix_audio("silent.mp4", "", "bgm.mp3", "output.mp4")
    assert success

@patch("os.path.exists")
@patch("subprocess.run")
def test_mix_audio_vo_only_success(mock_run, mock_exists):
    # Voiceover exists, BGM doesn't
    mock_exists.side_effect = lambda path: "voice.mp3" in path
    mock_run.return_value.returncode = 0
    
    success = mix_audio("silent.mp4", "voice.mp3", "", "output.mp4")
    assert success

@patch("os.path.exists")
def test_mix_audio_no_audio(mock_exists):
    mock_exists.return_value = False
    success = mix_audio("silent.mp4", "", "", "output.mp4")
    assert not success

@patch("requests.post")
@patch("requests.get")
@patch("time.sleep")
def test_eachlabs_predict_success(mock_sleep, mock_get, mock_post):
    # Mock POST start job
    mock_post_resp = MagicMock()
    mock_post_resp.status_code = 200
    mock_post_resp.json.return_value = {"id": "task_123"}
    mock_post.return_value = mock_post_resp
    
    # Mock GET poll job (first running, then completed)
    resp_running = MagicMock()
    resp_running.status_code = 200
    resp_running.json.return_value = {"status": "PROCESSING"}
    
    resp_completed = MagicMock()
    resp_completed.status_code = 200
    resp_completed.json.return_value = {"status": "success", "result": {"output": "http://result.url"}}
    
    mock_get.side_effect = [resp_running, resp_completed]
    
    res = _eachlabs_predict("model-slug", {"input": "data"})
    assert res == {"status": "success", "result": {"output": "http://result.url"}}

@patch("orchestrator._eachlabs_predict")
def test_vton_eachlabs_success(mock_predict):
    mock_predict.return_value = {"output": "http://image.url"}
    res = vton_eachlabs("http://prod.jpg", AI_MODELS["model_2"])
    assert res == "http://image.url"

@patch("orchestrator._eachlabs_predict")
def test_image_to_image_eachlabs_success(mock_predict):
    mock_predict.return_value = {"output": "http://image.url"}
    res = image_to_image_eachlabs("http://source.jpg", "prompt", "streetwear")
    assert res == "http://image.url"

@patch("orchestrator._eachlabs_predict")
def test_video_eachlabs_success(mock_predict):
    mock_predict.return_value = {"output": "http://video.url"}
    res = video_eachlabs("http://source.jpg", "motion prompt")
    assert res == "http://video.url"

@patch("orchestrator.HAS_HIGGSFIELD", True)
@patch("orchestrator.higgsfield_client")
@patch("time.sleep")
def test_image_higgsfield_success(mock_sleep, mock_hf):
    class Completed:
        pass
    mock_hf.types_.Completed = Completed
    mock_hf.DONE_STATUSES = [Completed]
    
    mock_req = MagicMock()
    mock_req.request_id = "req_123"
    mock_hf.submit.return_value = mock_req
    
    mock_hf.status.return_value = Completed()
    mock_hf.result.return_value = {"images": [{"url": "http://image.url"}]}
    
    res = image_higgsfield("prompt")
    assert res == "http://image.url"

@patch("orchestrator.HAS_HIGGSFIELD", True)
@patch("orchestrator.higgsfield_client")
def test_video_higgsfield_success(mock_hf):
    mock_hf.subscribe.return_value = {"video": {"url": "http://video.url"}}
    res = video_higgsfield("http://source.jpg", "motion prompt")
    assert res == "http://video.url"

@patch("orchestrator.generate_voiceover")
@patch("orchestrator.mix_audio")
@patch("orchestrator.concat_videos_ffmpeg")
@patch("orchestrator.download_file")
@patch("os.path.exists")
def test_assemble_ugc_video_success(mock_exists, mock_download, mock_concat, mock_mix, mock_vo):
    mock_exists.return_value = True
    mock_download.return_value = True
    mock_concat.return_value = True
    mock_mix.return_value = True
    mock_vo.return_value = True
    
    res = assemble_ugc_video(
        product_id="prod_123",
        model_name="Luna",
        clip_paths=["clip1.mp4"],
        success_count=1,
        scenes=[{"name": "Detail", "duration": 2}],
        bgm_path="bgm.mp3",
        script="Script",
        final_video_file="output.mp4",
        vton_image_url="http://vton.jpg"
    )
    assert res == ("output.mp4", "success", None)

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_eachlabs_success(mock_clear, mock_assemble, mock_script, mock_video, mock_img2img, mock_vton, mock_download):
    mock_download.return_value = True
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.return_value = "http://modelshoot.jpg"
    mock_video.return_value = "http://clip1.mp4"
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    res = run_ugc_pipeline(
        product_id="prod_eachlabs",
        product_name="Jacket",
        product_description="Cool",
        product_image_url="http://prod.jpg",
        product_type="tops",
        duration=5
    )
    
    assert len(res) == 1
    assert res[0]["backend"] == "eachlabs+eachlabs"
    assert res[0]["final_video_url"] == "http://app/final.mp4"

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_model_id_filter(mock_clear, mock_assemble, mock_script, mock_video, mock_img2img, mock_vton, mock_download):
    mock_download.return_value = True
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.return_value = "http://modelshoot.jpg"
    mock_video.return_value = "http://clip1.mp4"
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    res = run_ugc_pipeline(
        product_id="prod_filtered",
        product_name="Jacket",
        product_description="Cool",
        product_image_url="http://prod.jpg",
        product_type="tops",
        duration=5,
        model_id_filter="model_2" # Only run for Luna (model_2)
    )
    
    assert len(res) == 1
    assert res[0]["model_name"] == "Luna"
    assert res[0]["final_video_url"] == "http://app/final.mp4"

def test_build_storyboard_panels():
    panels_short = build_storyboard_panels("Jacket", "Luna", 5)
    assert len(panels_short) == 9
    assert panels_short[0]["panel"] == 1
    
    panels_long = build_storyboard_panels("Jacket", "Luna", 10)
    assert len(panels_long) == 9
    assert "Panel 1 action beat" in panels_long[0]["beat"]

def test_get_storyboard_scenes_sampling():
    from orchestrator import SCENE_TEMPLATES, get_storyboard_scenes
    SCENE_TEMPLATES["test_style"] = {
        "tops": [
            {"name": "1", "duration": 1, "motion_prompt": "1"},
            {"name": "2", "duration": 1, "motion_prompt": "2"},
            {"name": "3", "duration": 1, "motion_prompt": "3"},
            {"name": "4", "duration": 1, "motion_prompt": "4"},
        ]
    }
    scenes = get_storyboard_scenes("test_style", "tops")
    assert len(scenes) == 3
    del SCENE_TEMPLATES["test_style"]

def test_update_in_flight_state():
    product_id = "test_in_flight"
    clear_in_flight_state(product_id)
    
    update_in_flight_state(product_id, ["model_2", "scene_0"], "task_vton_123")
    state = load_in_flight_state(product_id)
    assert state == {"model_2": {"scene_0": "task_vton_123"}}
    
    clear_in_flight_state(product_id)

@patch("requests.post")
def test_eachlabs_predict_api_error(mock_post):
    mock_resp = MagicMock()
    mock_resp.status_code = 400
    mock_resp.json.return_value = {"error": "API Key Invalid"}
    mock_post.return_value = mock_resp
    
    with pytest.raises(Exception, match="EachLabs API Error"):
        _eachlabs_predict("model-slug", {"input": "data"})

@patch("requests.post")
@patch("requests.get")
@patch("time.sleep")
def test_eachlabs_predict_failed_status(mock_sleep, mock_get, mock_post):
    mock_post.return_value.status_code = 200
    mock_post.return_value.json.return_value = {"id": "task_123"}
    
    mock_get.return_value.status_code = 200
    mock_get.return_value.json.return_value = {"status": "failed", "error": "Prediction process crashed"}
    
    with pytest.raises(Exception, match="EachLabs prediction failed with status 'failed'"):
        _eachlabs_predict("model-slug", {"input": "data"})

@patch("requests.post")
@patch("requests.get")
@patch("time.sleep")
def test_eachlabs_predict_timeout(mock_sleep, mock_get, mock_post):
    mock_post.return_value.status_code = 200
    mock_post.return_value.json.return_value = {"id": "task_123"}
    
    mock_get.return_value.status_code = 200
    mock_get.return_value.json.return_value = {"status": "processing"}
    
    with pytest.raises(TimeoutError, match="EachLabs prediction timed out"):
        _eachlabs_predict("model-slug", {"input": "data"})

@patch("orchestrator.HAS_HIGGSFIELD", True)
@patch("orchestrator.higgsfield_client")
@patch("time.sleep")
def test_image_higgsfield_failed(mock_sleep, mock_hf):
    class Failed:
        pass
    mock_hf.types_.Completed = classmethod(lambda cls: None)
    mock_hf.DONE_STATUSES = [Failed]
    
    mock_req = MagicMock()
    mock_req.request_id = "req_123"
    mock_hf.submit.return_value = mock_req
    mock_hf.status.return_value = Failed()
    
    with pytest.raises(Exception, match="Higgsfield failed with status"):
        image_higgsfield("prompt")

@patch("orchestrator.HAS_HIGGSFIELD", True)
@patch("orchestrator.higgsfield_client")
@patch("time.sleep")
def test_image_higgsfield_timeout(mock_sleep, mock_hf):
    mock_hf.DONE_STATUSES = []
    mock_req = MagicMock()
    mock_req.request_id = "req_123"
    mock_hf.submit.return_value = mock_req
    mock_hf.status.return_value = MagicMock()
    
    with pytest.raises(TimeoutError, match="Higgsfield generation timed out"):
        image_higgsfield("prompt")

import runpy

@patch("requests.post")
@patch("requests.get")
@patch("orchestrator.download_file")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
@patch("time.sleep")
@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
def test_main_cli_execution(mock_sleep, mock_clear, mock_assemble, mock_download, mock_get, mock_post):
    mock_post.return_value.status_code = 200
    mock_post.return_value.json.return_value = {"id": "task_123"}
    
    mock_get.return_value.status_code = 200
    mock_get.return_value.json.return_value = {"status": "success", "output": "http://vton.jpg"}
    
    mock_download.return_value = True
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    test_args = [
        "orchestrator.py",
        "--product_id", "prod_cli",
        "--product_name", "Jacket",
        "--product_image_url", "http://prod.jpg"
    ]
    with patch("sys.argv", test_args):
        runpy.run_path("orchestrator.py", run_name="__main__")

@patch("requests.post")
@patch("os.getenv")
def test_generate_voiceover_elevenlabs_non_200(mock_env, mock_post):
    mock_env.return_value = "fake_key"
    mock_resp = MagicMock()
    mock_resp.status_code = 500
    mock_resp.text = "Internal Server Error"
    mock_post.return_value = mock_resp
    
    with patch("edge_tts.Communicate", side_effect=Exception("Edge TTS failed")):
        success = generate_voiceover("Hello", "Luna", "out.mp3")
        assert not success

@patch("requests.post")
@patch("os.getenv")
def test_generate_voiceover_elevenlabs_exception(mock_env, mock_post):
    mock_env.return_value = "fake_key"
    mock_post.side_effect = Exception("ElevenLabs connection error")
    
    with patch("edge_tts.Communicate", side_effect=Exception("Edge TTS failed")):
        success = generate_voiceover("Hello", "Luna", "out.mp3")
        assert not success

@patch("subprocess.run")
def test_mix_audio_ffmpeg_failed(mock_run):
    mock_run.return_value.returncode = 1
    mock_run.return_value.stderr = b"FFmpeg mix error"
    with patch("os.path.exists", return_value=True):
        success = mix_audio("silent.mp4", "voice.mp3", "bgm.mp3", "output.mp4")
        assert not success

@patch("subprocess.run")
def test_mix_audio_exception(mock_run):
    mock_run.side_effect = Exception("FFmpeg binary not found")
    with patch("os.path.exists", return_value=True):
        success = mix_audio("silent.mp4", "voice.mp3", "bgm.mp3", "output.mp4")
        assert not success

@patch("subprocess.run")
def test_concat_videos_ffmpeg_exception(mock_run):
    mock_run.side_effect = Exception("FFmpeg concat exception")
    success = concat_videos_ffmpeg(["clip1.mp4"], "output.mp4")
    assert not success

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_custom_bgm_and_failed_download(mock_clear, mock_assemble, mock_script, mock_video, mock_img2img, mock_vton, mock_download):
    mock_download.return_value = False
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.return_value = "http://modelshoot.jpg"
    mock_video.return_value = "http://clip1.mp4"
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    res = run_ugc_pipeline(
        product_id="prod_bgm",
        product_name="Jacket",
        product_description="Cool",
        product_image_url="http://prod.jpg",
        product_type="tops",
        duration=5,
        bgm_url="http://example.com/custom_track.mp3",
        model_id_filter="model_2"
    )
    
    assert len(res) == 1
    assert mock_download.call_args_list[0][0][0] == "http://example.com/custom_track.mp3"

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_vton_fails(mock_clear, mock_vton, mock_download):
    mock_download.return_value = True
    mock_vton.return_value = ""
    
    with pytest.raises(RuntimeError, match="All models failed to generate video"):
        run_ugc_pipeline(
            product_id="prod_failed_vton",
            product_name="Jacket",
            product_description="Cool",
            product_image_url="http://prod.jpg",
            product_type="tops",
            duration=5,
            model_id_filter="model_2"
        )

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_cache_write_error(mock_clear, mock_assemble, mock_script, mock_video, mock_img2img, mock_vton, mock_download):
    mock_download.return_value = True
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.return_value = "http://modelshoot.jpg"
    mock_video.return_value = "http://clip1.mp4"
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    with patch("json.dump", side_effect=IOError("Disk Full")):
        res = run_ugc_pipeline(
            product_id="prod_cache_fail",
            product_name="Jacket",
            product_description="Cool",
            product_image_url="http://prod.jpg",
            product_type="tops",
            duration=5,
            model_id_filter="model_2"
        )
    
    assert len(res) == 1
    assert res[0]["status"] == "success"

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_overall_exception(mock_clear, mock_vton, mock_download):
    mock_download.return_value = True
    mock_vton.side_effect = RuntimeError("Pipeline crashed")
    
    with pytest.raises(RuntimeError, match="All models failed to generate video"):
        run_ugc_pipeline(
            product_id="prod_crash",
            product_name="Jacket",
            product_description="Cool",
            product_image_url="http://prod.jpg",
            product_type="tops",
            duration=5,
            model_id_filter="model_2"
        )

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "fake_key"
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_higgsfield")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_higgsfield_to_eachlabs_fallback(
    mock_clear, mock_assemble, mock_script, mock_video_el, mock_video_hf, mock_img2img, mock_vton, mock_download
):
    mock_download.return_value = True
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.return_value = "http://modelshoot.jpg"
    
    # Higgsfield video fails (returns None or raises exception)
    mock_video_hf.side_effect = Exception("Higgsfield failed")
    # Eachlabs video succeeds
    mock_video_el.return_value = "http://clip1.mp4"
    
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    res = run_ugc_pipeline(
        product_id="prod_fallback",
        product_name="Jacket",
        product_description="Cool",
        product_image_url="http://prod.jpg",
        product_type="tops",
        duration=5,
        model_id_filter="model_2"
    )
    
    assert len(res) == 1
    assert res[0]["backend"] == "eachlabs+higgsfield"
    assert res[0]["status"] == "success"
    mock_video_hf.assert_called()
    mock_video_el.assert_called()

@patch("orchestrator.generate_voiceover")
@patch("orchestrator.mix_audio")
@patch("orchestrator.concat_videos_ffmpeg")
@patch("orchestrator.download_file")
@patch("os.path.exists")
@patch.dict(os.environ, {"SYNCSO_API_KEY": "fake_sync_key"})
def test_assemble_ugc_video_with_syncso(mock_exists, mock_download, mock_concat, mock_mix, mock_vo):
    mock_exists.return_value = True
    mock_download.return_value = True
    mock_concat.return_value = True
    mock_mix.return_value = True
    mock_vo.return_value = True
    
    res = assemble_ugc_video(
        product_id="prod_123",
        model_name="Luna",
        clip_paths=["clip1.mp4"],
        success_count=1,
        scenes=[{"name": "Detail", "duration": 2}],
        bgm_path="bgm.mp3",
        script="Script",
        final_video_file="output.mp4",
        vton_image_url="http://vton.jpg"
    )
    assert res == ("output.mp4", "success", None)

def test_assemble_ugc_video_zero_success():
    res = assemble_ugc_video(
        product_id="prod_123",
        model_name="Luna",
        clip_paths=[],
        success_count=0,
        scenes=[{"name": "Detail", "duration": 2}],
        bgm_path="bgm.mp3",
        script="Script",
        final_video_file="output.mp4",
        vton_image_url="http://vton.jpg"
    )
    assert res == ("http://vton.jpg", "partial", "All video scenes failed")

@patch("orchestrator.generate_voiceover")
@patch("orchestrator.mix_audio")
@patch("orchestrator.concat_videos_ffmpeg")
@patch("orchestrator.download_file")
@patch("os.path.exists")
def test_assemble_ugc_video_partial_success(mock_exists, mock_download, mock_concat, mock_mix, mock_vo):
    mock_exists.return_value = True
    mock_download.return_value = True
    mock_concat.return_value = True
    mock_mix.return_value = True
    mock_vo.return_value = True
    
    res = assemble_ugc_video(
        product_id="prod_123",
        model_name="Luna",
        clip_paths=["clip1.mp4"],
        success_count=1,
        scenes=[{"name": "Detail", "duration": 2}, {"name": "Walk", "duration": 3}],
        bgm_path="bgm.mp3",
        script="Script",
        final_video_file="output.mp4",
        vton_image_url="http://vton.jpg"
    )
    assert res == ("output.mp4", "partial", None)

@patch("orchestrator.HAS_HIGGSFIELD", True)
@patch("orchestrator.higgsfield_client")
def test_video_higgsfield_variants(mock_hf):
    mock_hf.subscribe.return_value = {"output": {"url": "http://url2"}}
    assert video_higgsfield("http://source.jpg", "prompt") == "http://url2"
    
    mock_hf.subscribe.return_value = {"output": "http://url3"}
    assert video_higgsfield("http://source.jpg", "prompt") == "http://url3"
    
    mock_hf.subscribe.return_value = {"images": [{"url": "http://url4"}]}
    assert video_higgsfield("http://source.jpg", "prompt") == "http://url4"
    
    mock_hf.subscribe.return_value = {"images": ["http://url5"]}
    assert video_higgsfield("http://source.jpg", "prompt") == "http://url5"

@patch.dict(os.environ, {
    "EACHLABS_API_KEY": "fake_key",
    "HF_KEY": "",
    "HF_API_KEY": "",
    "HF_API_SECRET": ""
})
@patch("orchestrator.download_file")
@patch("orchestrator.vton_eachlabs")
@patch("orchestrator.image_to_image_eachlabs")
@patch("orchestrator.video_eachlabs")
@patch("orchestrator.generate_video_script")
@patch("orchestrator.assemble_ugc_video")
@patch("orchestrator.clear_in_flight_state")
def test_run_ugc_pipeline_img2img_fails_fallback_to_vton(
    mock_clear, mock_assemble, mock_script, mock_video, mock_img2img, mock_vton, mock_download
):
    mock_download.return_value = True
    mock_vton.return_value = "http://vton.jpg"
    mock_img2img.side_effect = RuntimeError("img2img failed")
    
    mock_video.return_value = "http://clip1.mp4"
    mock_script.return_value = "Mock Script"
    mock_assemble.return_value = ("http://app/final.mp4", "success", None)
    
    res = run_ugc_pipeline(
        product_id="prod_img2img_fail",
        product_name="Jacket",
        product_description="Cool",
        product_image_url="http://prod.jpg",
        product_type="tops",
        duration=5,
        model_id_filter="model_2"
    )
    
    assert len(res) == 1
    assert res[0]["status"] == "success"
    assert res[0]["storyboard_images"] == ["http://vton.jpg", "http://vton.jpg", "http://vton.jpg"]






