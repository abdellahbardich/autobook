from typing import Dict, List, Tuple
from app.story_generator import StoryGenerator
from app.models import StoryRequest, StoryResponse, SceneDescription, CharacterDescription


class TextGenerationService:
    def __init__(self):
        self.story_generator = StoryGenerator()
    
    async def generate_story(self, request: StoryRequest) -> StoryResponse:
        # Generate story with scenes
        story_text, scenes = self.story_generator.get_story_from_summary(
            request.summary, request.num_scenes)
        
        # Extract character description
        character_desc, character_tokens = self.story_generator.extract_character_description(story_text)
        
        # Format the response
        formatted_scenes = [
            SceneDescription(
                number=scene['number'],
                narrative=scene['narrative'],
                scene_description=scene['scene_description']
            ) for scene in scenes
        ]
        
        main_character = CharacterDescription(
            description=character_desc,
            tokens=character_tokens
        )
        
        return StoryResponse(
            story_text=story_text,
            scenes=formatted_scenes,
            main_character=main_character,
            user_id=request.user_id,
            book_id=request.book_id
        )