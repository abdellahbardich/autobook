from pydantic import BaseModel
from typing import List, Dict, Optional


class SceneContent(BaseModel):
    number: int
    narrative: str
    scene_description: str
    image_url: Optional[str] = None
    image_s3_key: Optional[str] = None


class PdfGenerationRequest(BaseModel):
    title: str
    main_character_desc: str
    scenes: List[SceneContent]
    template_type: str 
    user_id: int
    book_id: int


class PdfGenerationResponse(BaseModel):
    pdf_url: str
    s3_key: str
    user_id: int
    book_id: int
    title: str