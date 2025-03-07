import os
import boto3
from io import BytesIO
from typing import Dict, List
from app.pdf_generator import PdfGenerator
from app.models import PdfGenerationRequest, PdfGenerationResponse


class PdfAssemblyService:
    def __init__(self):
        self.pdf_generator = PdfGenerator()
        
        # S3 setup
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.environ.get("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=os.environ.get("AWS_SECRET_ACCESS_KEY"),
            region_name=os.environ.get("AWS_REGION", "us-east-1")
        )
        self.s3_bucket = os.environ.get("S3_BUCKET", "autobook-storage")
    
    async def generate_pdf(self, request: PdfGenerationRequest) -> PdfGenerationResponse:
        # Convert pydantic scenes to dict for the generator
        scenes_dict = [
            {
                'number': scene.number,
                'narrative': scene.narrative,
                'scene_description': scene.scene_description,
                'image_url': scene.image_url
            } 
            for scene in request.scenes
        ]
        
        # Generate the appropriate PDF type
        pdf_buffer = None
        
        if request.template_type == "TEXT_IMAGE":
            pdf_buffer = self.pdf_generator.create_text_image_pdf(
                request.title, 
                request.main_character_desc, 
                scenes_dict
            )
        elif request.template_type == "TEXT_ONLY":
            pdf_buffer = self.pdf_generator.create_text_only_pdf(
                request.title, 
                request.main_character_desc, 
                scenes_dict
            )
        elif request.template_type == "IMAGE_ONLY":
            pdf_buffer = self.pdf_generator.create_image_only_pdf(
                request.title, 
                scenes_dict
            )
        elif request.template_type == "COLORING":
            pdf_buffer = self.pdf_generator.create_coloring_pdf(
                request.title, 
                scenes_dict
            )
        else:
            raise ValueError(f"Unsupported template type: {request.template_type}")
        
        # Upload PDF to S3
        s3_key = f"books/{request.user_id}/{request.book_id}/pdf/{request.title.replace(' ', '_')}_{request.template_type.lower()}.pdf"
        
        self.s3_client.put_object(
            Bucket=self.s3_bucket,
            Key=s3_key,
            Body=pdf_buffer.getvalue(),
            ContentType='application/pdf'
        )
        
        # Generate presigned URL
        url = self.s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': self.s3_bucket, 'Key': s3_key},
            ExpiresIn=3600  # 1 hour
        )
        
        return PdfGenerationResponse(
            pdf_url=url,
            s3_key=s3_key,
            user_id=request.user_id,
            book_id=request.book_id,
            title=request.title
        )