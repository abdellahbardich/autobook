import requests
import json
import time
import base64
import os
import io
from PIL import Image, ImageDraw, ImageFont
from typing import List, Dict, Tuple


class ImageGenerator:
    def __init__(self, nvidia_api_key: str = None, local_mode: bool = True):
        self.nvidia_api_key = nvidia_api_key or os.environ.get("NVIDIA_API_KEY", "")
        self.local_mode = local_mode
        self.consistory_url = "https://ai.api.nvidia.com/v1/genai/nvidia/consistory"
        
    def generate_images_batch(self, 
                             subject_prompt: str, 
                             subject_tokens: List[str], 
                             scenes: List[Dict], 
                             style_prompt: str) -> List[Dict]:
        """Generate images in batch using init and extra modes"""
        
        if self.local_mode:
            return self._generate_locally(subject_prompt, subject_tokens, scenes, style_prompt)
        else:
            return self._generate_with_nvidia(subject_prompt, subject_tokens, scenes, style_prompt)
    
    def _generate_locally(self, subject_prompt: str, subject_tokens: List[str], scenes: List[Dict], style_prompt: str) -> List[Dict]:
        """Generate placeholder images locally for testing"""
        result_images = []
        
        for i, scene in enumerate(scenes):
            img = Image.new('RGB', (512, 512), color='lightblue')
            draw = ImageDraw.Draw(img)
            
            draw.text((20, 20), f"Scene {i+1}", fill='black')
            
            desc = scene['scene_description']
            lines = []
            line = ""
            for word in desc.split():
                if len(line + word) < 40:
                    line += word + " "
                else:
                    lines.append(line)
                    line = word + " "
            lines.append(line)
            
            y = 50
            for line in lines:
                draw.text((20, y), line, fill='black')
                y += 20
            
            buffer = io.BytesIO()
            img.save(buffer, format="JPEG")
            img_data = buffer.getvalue()
            
            base64_str = base64.b64encode(img_data).decode('utf-8')
            
            result_images.append({
                'scene_number': i + 1,
                'base64_data': base64_str
            })
            
        return result_images
        
    def _generate_with_nvidia(self, subject_prompt: str, subject_tokens: List[str], scenes: List[Dict], style_prompt: str) -> List[Dict]:
        """Generate images using NVIDIA Consistory API"""
        headers = {
            "Authorization": f"Bearer {self.nvidia_api_key}",
            "Content-Type": "application/json",
            "accept": "application/json"
        }
        
        result_images = []
        try:
            print("\nGenerating initial scenes...")
            init_payload = {
                "mode": "init",
                "subject_prompt": subject_prompt,
                "subject_tokens": subject_tokens[:5],  
                "subject_seed": int(time.time()) % 1000000,
                "style_prompt": style_prompt,
                "scene_prompt1": scenes[0]['scene_description'],
                "scene_prompt2": scenes[1]['scene_description'] if len(scenes) > 1 else scenes[0]['scene_description'],
                "attention_dropout": 0.5,
                "cfg_scale": 7,
                "negative_prompt": "ugly, blurry, low quality, distorted, deformed",
                "same_initial_noise": False
            }
            
            response = requests.post(self.consistory_url, headers=headers, json=init_payload)
            response.raise_for_status()
            
            result = response.json()
            print("Initial generation complete")
            
            if 'artifacts' in result:
                for i, artifact in enumerate(result['artifacts']):
                    if 'base64' in artifact:
                        result_images.append({
                            'scene_number': i + 1,
                            'base64_data': artifact['base64']
                        })
                        print(f"✓ Processed initial scene {i+1}")

            subject_seed = result['artifacts'][0]['seed'] if result.get('artifacts') else int(time.time()) % 1000000
            
            for i, scene in enumerate(scenes[2:], start=3):
                print(f"\nGenerating scene {i}...")
                extra_payload = {
                    "mode": "extra",
                    "subject_prompt": subject_prompt,
                    "subject_tokens": subject_tokens[:5],
                    "subject_seed": subject_seed,  
                    "style_prompt": style_prompt,
                    "scene_prompt1": scenes[0]['scene_description'],
                    "scene_prompt2": scenes[1]['scene_description'] if len(scenes) > 1 else scenes[0]['scene_description'],
                    "additional_scene_prompt": scene['scene_description'],
                    "additional_scene_seed": int(time.time()) % 1000000,
                    "attention_dropout": 0.5,
                    "cfg_scale": 7,
                    "negative_prompt": "ugly, blurry, low quality, distorted, deformed",
                    "same_initial_noise": False
                }
                
                response = requests.post(self.consistory_url, headers=headers, json=extra_payload)
                response.raise_for_status()
                
                result = response.json()
                
                if 'artifacts' in result:
                    for artifact in result['artifacts']:
                        if 'base64' in artifact:
                            result_images.append({
                                'scene_number': i,
                                'base64_data': artifact['base64']
                            })
                            print(f"✓ Processed scene {i}")
                
                time.sleep(2)
                
        except Exception as e:
            print(f"Error generating images: {e}")
            
        scene_numbers = {img['scene_number'] for img in result_images}
        for i in range(1, len(scenes) + 1):
            if i not in scene_numbers:
                print(f"⚠ Adding placeholder for scene {i}")
                img = Image.new('RGB', (512, 512), color='lightgray')
                draw = ImageDraw.Draw(img)
                text = f"Scene {i} - Image Generation Failed"
                draw.text((100, 256), text, fill='black')
                
                buffer = io.BytesIO()
                img.save(buffer, format="JPEG")
                img_data = buffer.getvalue()
                
                base64_str = base64.b64encode(img_data).decode('utf-8')
                
                result_images.append({
                    'scene_number': i,
                    'base64_data': base64_str
                })
        
        result_images.sort(key=lambda x: x['scene_number'])
        return result_images