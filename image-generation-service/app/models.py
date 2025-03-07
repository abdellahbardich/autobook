from pydantic import BaseModel
from typing import List, Dict, Optional


class SceneDescription(BaseModel):
    number: int
    narrative: str
    scene_description: str


class GenerateImagesRequest(BaseModel):
    subject_prompt: str
    subject_tokens: List[str]
    scenes: List[SceneDescription]
    style_prompt: str
    user_id: int
    book_id: int


class ImageResponse(BaseModel):
    scene_number: int
    image_url: str
    s3_key: str


class GenerateImagesResponse(BaseModel):
    images: List[ImageResponse]
    user_id: int
    book_id: int