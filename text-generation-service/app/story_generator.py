import subprocess
import traceback
import re
from typing import List, Dict, Tuple


class StoryGenerator:
    def __init__(self):
        pass

    def get_story_from_summary(self, summary: str, num_scenes: int) -> Tuple[str, List[Dict]]:
        """Generate a complete story with scenes from a summary"""
        prompt = f"""Based on this summary: "{summary}"
        Write a story with exactly {num_scenes} scenes. 
        For each scene:
        1. Start with "Scene X:" where X is the scene number
        2. Write a descriptive paragraph for the scene
        3. Include details about the setting and any characters present
        4. End each scene with a clear scene description in [square brackets]
        Make sure the scene descriptions are detailed and consistent.
        
        Example format:
        Scene 1: 
        The sun rose over the misty mountains as Sarah approached the ancient temple...
        [A young woman with flowing red hair and a green cloak standing before a weathered stone temple at dawn]
        """

        try:
            cmd = ['ollama', 'run', 'llama3.2', prompt]
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding='utf-8',
                errors='replace'
            )
            
            story_text, error = process.communicate()
            
            if process.returncode != 0:
                raise Exception(f"Ollama error: {error}")

            if not story_text.strip():
                raise Exception("No story generated")

            scenes = self._parse_story_into_scenes(story_text)
            
            return story_text, scenes

        except Exception as e:
            print(f"Error generating story: {e}")
            return "", []

    def extract_character_description(self, story_text: str) -> Tuple[str, List[str]]:
        """Extract main character description and identifying tokens"""
        prompt = f"""From this story: 
        {story_text}
        
        1. Identify the main character/subject
        2. Provide a detailed visual description
        3. List 3-5 key identifying words (tokens) that define their appearance
        
        Format as:
        Description: [Detailed visual description]
        Tokens: [comma-separated list of identifying words]
        """

        try:
            cmd = ['ollama', 'run', 'llama3.2', prompt]
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding='utf-8',
                errors='replace'
            )
            
            output, error = process.communicate()
            
            if process.returncode != 0:
                raise Exception(f"Ollama error: {error}")

            description = ""
            tokens = []
            
            lines = output.strip().split('\n')
            for line in lines:
                if line.startswith("Description:"):
                    description = line.split(":", 1)[1].strip()
                elif line.startswith("Tokens:"):
                    tokens_str = line.split(":", 1)[1].strip()
                    tokens = [t.strip() for t in tokens_str.split(",")]

            return description, tokens

        except Exception as e:
            print(f"Error extracting character description: {e}")
            return "", []

    def _parse_story_into_scenes(self, story_text: str) -> List[Dict]:
        """Parse the story text into structured scenes"""
        scenes = []
        current_scene = None
        current_text = []
        scene_pattern = re.compile(r'Scene \d+:', re.IGNORECASE)
        
        for line in story_text.split('\n'):
            line = line.strip()
            if not line:
                continue
                
            if scene_pattern.match(line):
                if current_scene is not None and current_text:
                    scene_content = ' '.join(current_text)
                    desc_match = re.search(r'\[(.*?)\]', scene_content)
                    scene_desc = desc_match.group(1) if desc_match else scene_content
                    narrative = scene_content.replace(f"[{scene_desc}]", '').strip()
                    
                    scenes.append({
                        'number': current_scene,
                        'narrative': narrative,
                        'scene_description': scene_desc
                    })
                
                current_scene = len(scenes) + 1
                current_text = []
            else:
                current_text.append(line)

        if current_scene is not None and current_text:
            scene_content = ' '.join(current_text)
            desc_match = re.search(r'\[(.*?)\]', scene_content)
            scene_desc = desc_match.group(1) if desc_match else scene_content
            narrative = scene_content.replace(f"[{scene_desc}]", '').strip()
            
            scenes.append({
                'number': current_scene,
                'narrative': narrative,
                'scene_description': scene_desc
            })

        return scenes