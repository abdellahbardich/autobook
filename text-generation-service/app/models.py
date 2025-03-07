from pydantic import BaseModel
from typing import List, Dict, Optional


class StoryRequest(BaseModel):
    summary: str
    num_scenes: int
    user_id: int
    book_id: int


class SceneDescription(BaseModel):
    number: int
    narrative: str
    scene_description: str


class CharacterDescription(BaseModel):
    description: str
    tokens: List[str]


class StoryResponse(BaseModel):
    story_text: str
    scenes: List[SceneDescription]
    main_character: CharacterDescription
    user_id: int
    book_id: int