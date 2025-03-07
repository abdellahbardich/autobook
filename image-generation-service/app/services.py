import os
import base64
import boto3
import io
from typing import Dict, List, Tuple
from app.image_generator import ImageGenerator
from app.models import GenerateImagesRequest, GenerateImagesResponse, ImageResponse, SceneDescription


class ImageGenerationService:
    def __init__(self):
        self.image_generator = ImageGenerator(
            nvidia_api_key=os.environ.get("NVIDIA_API_KEY", ""),
            local_mode=os.environ.get("LOCAL_MODE", "True").lower() == "true"
        )
        
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.environ.get("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=os.environ.get("AWS_SECRET_ACCESS_KEY"),
            region_name=os.environ.get("AWS_REGION", "us-east-1")
        )
        self.s3_bucket = os.environ.get("S3_BUCKET", "autobook-storage")
    
    async def generate_images(self, request: GenerateImagesRequest) -> GenerateImagesResponse:
        scenes_dict = [
            {
                'number': scene.number,
                'narrative': scene.narrative,
                'scene_description': scene.scene_description
            } 
            for scene in request.scenes
        ]
        
        images = self.image_generator.generate_images_batch(
            subject_prompt=request.subject_prompt,
            subject_tokens=request.subject_tokens,
            scenes=scenes_dict,
            style_prompt=request.style_prompt
        )
        
        response_images = []
        for image in images:
            scene_number = image['scene_number']
            base64_data = image['base64_data']
            
            s3_key = f"books/{request.user_id}/{request.book_id}/images/scene_{scene_number}.jpg"
            img_data = base64.b64decode(base64_data)
            
            self.s3_client.put_object(
                Bucket=self.s3_bucket,
                Key=s3_key,
                Body=img_data,
                ContentType='image/jpeg'
            )
            
            url = self.s3_client.generate_presigned_url(
                'get_object',
                Params={'Bucket': self.s3_bucket, 'Key': s3_key},
                ExpiresIn=3600  
            )
            
            response_images.append(
                ImageResponse(
                    scene_number=scene_number,
                    image_url=url,
                    s3_key=s3_key
                )
            )
        
        return GenerateImagesResponse(
            images=response_images,
            user_id=request.user_id,
            book_id=request.book_id
        )