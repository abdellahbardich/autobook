from fastapi import FastAPI, HTTPException, Depends
from app.models import GenerateImagesRequest, GenerateImagesResponse
from app.services import ImageGenerationService

app = FastAPI(title="AutoBook Image Generation Service")

image_service = ImageGenerationService()


@app.get("/")
async def root():
    return {"message": "AutoBook Image Generation Service is running"}


@app.post("/generate-images", response_model=GenerateImagesResponse)
async def generate_images(request: GenerateImagesRequest):
    try:
        response = await image_service.generate_images(request)
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health_check():
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)