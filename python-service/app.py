import os
import base64
import requests
import subprocess
import time
from flask import Flask, request, jsonify
import logging
from PIL import Image
import io
import uuid

app = Flask(__name__)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

NVIDIA_API_KEY = os.environ.get('NVIDIA_API_KEY',
                                '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')
CONSISTORY_URL = "https://ai.api.nvidia.com/v1/genai/nvidia/consistory"

UPLOAD_DIR = "./uploads"
if not os.path.exists(UPLOAD_DIR):
    os.makedirs(UPLOAD_DIR)


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy"}), 200


@app.route('/generate/text', methods=['POST'])
def generate_text():
    """Generate text using Ollama"""
    data = request.json
    if not data or 'prompt' not in data:
        return jsonify({"error": "Missing prompt parameter"}), 400

    prompt = data['prompt']
    model = data.get('model', 'llama3.2')

    try:
        logger.info(f"Generating text with {model}: {prompt[:50]}...")

        cmd = ['ollama', 'run', model, prompt]
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding='utf-8',
            errors='replace'
        )

        generated_text, error = process.communicate()
        logger.info(f"Generated text : {generated_text}")

        if process.returncode != 0:
            logger.error(f"Ollama error: {error}")
            return jsonify({"error": f"Ollama error: {error}"}), 500

        if not generated_text.strip():
            logger.error("No text generated")
            return jsonify({"error": "No text generated"}), 500

        return jsonify({"response": generated_text.strip()}), 200

    except Exception as e:
        logger.error(f"Error generating text: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/generate/cover', methods=['POST'])
def generate_cover_image():
    """Generate cover image using Consistory"""
    data = request.json
    if not data or 'title' not in data:
        return jsonify({"error": "Missing title parameter"}), 400

    title = data['title']
    style = data.get('style', 'A detailed illustration of')

    try:
        subject_prompt = f"{title}"

        tokens = [word.lower() for word in title.split()
                  if len(word) > 3 and word.lower() not in {'with', 'that', 'this', 'from', 'into', 'about'}][:5]

        if not tokens:
            tokens = ["book", "cover"]

        logger.info(f"Generating cover image with subject: {subject_prompt}, tokens: {tokens}")

        headers = {
            "Authorization": f"Bearer {NVIDIA_API_KEY}",
            "Accept": "application/json",
            "Content-Type": "application/json"
        }

        payload = {
            "mode": "init",
            "subject_prompt": subject_prompt,
            "subject_tokens": tokens,
            "subject_seed": int(time.time()) % 1000,
            "style_prompt": style,
            "scene_prompt1": f"book cover of {title}",
            "scene_prompt2": f"elegant book cover design for {title}",
            "negative_prompt": "ugly, blurry, text, words, writing, low quality",
            "cfg_scale": 7,
            "same_initial_noise": False
        }

        response = requests.post(CONSISTORY_URL, headers=headers, json=payload)
        response.raise_for_status()
        data = response.json()

        if 'artifacts' in data and len(data['artifacts']) > 0:
            img_base64 = data['artifacts'][0]["base64"]

            img_bytes = base64.b64decode(img_base64)
            img_filename = f"cover_{uuid.uuid4()}.jpg"
            img_path = os.path.join(UPLOAD_DIR, img_filename)

            with open(img_path, "wb") as f:
                f.write(img_bytes)

            logger.info(f"Cover image saved to: {img_path}")
            return jsonify({"coverImagePath": img_path}), 200
        else:
            logger.error("No images generated from Consistory")
            return jsonify({"error": "No images generated from Consistory"}), 500

    except Exception as e:
        logger.error(f"Error generating cover image: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/generate/illustration', methods=['POST'])
def generate_illustration():
    """Generate illustration using Consistory"""
    data = request.json
    if not data or 'prompt' not in data:
        return jsonify({"error": "Missing prompt parameter"}), 400

    prompt = data['prompt']
    style = data.get('style', 'A detailed illustration')

    try:
        description_parts = prompt.split()
        tokens = [word.lower() for word in description_parts
                  if len(word) > 3 and word.lower() not in {'with', 'that', 'this', 'from', 'into', 'about'}][:5]

        if not tokens:
            tokens = ["illustration"]

        logger.info(f"Generating illustration with prompt: {prompt}, tokens: {tokens}")

        headers = {
            "Authorization": f"Bearer {NVIDIA_API_KEY}",
            "Accept": "application/json",
            "Content-Type": "application/json"
        }

        payload = {
            "mode": "init",
            "subject_prompt": prompt,
            "subject_tokens": tokens,
            "subject_seed": int(time.time()) % 1000,
            "style_prompt": style,
            "scene_prompt1": prompt,
            "scene_prompt2": f"{prompt} detailed view",
            "negative_prompt": "ugly, blurry, text, words, writing, low quality",
            "cfg_scale": 7,
            "same_initial_noise": False
        }

        response = requests.post(CONSISTORY_URL, headers=headers, json=payload)
        response.raise_for_status()
        data = response.json()

        results = []

        if 'artifacts' in data and len(data['artifacts']) > 0:
            for idx, artifact in enumerate(data['artifacts']):
                img_base64 = artifact["base64"]

                img_bytes = base64.b64decode(img_base64)
                img_filename = f"illustration_{uuid.uuid4()}.jpg"
                img_path = os.path.join(UPLOAD_DIR, img_filename)

                with open(img_path, "wb") as f:
                    f.write(img_bytes)

                results.append({"illustrationPath": img_path})

            logger.info(f"Generated {len(results)} illustrations")
            return jsonify(results), 200
        else:
            logger.error("No images generated from Consistory")
            return jsonify({"error": "No images generated from Consistory"}), 500

    except Exception as e:
        logger.error(f"Error generating illustration: {str(e)}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=True)