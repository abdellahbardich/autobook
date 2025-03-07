from fastapi import FastAPI, HTTPException, Depends
from app.models import StoryRequest, StoryResponse
from app.services import TextGenerationService

app = FastAPI(title="AutoBook Text Generation Service")

text_service = TextGenerationService()


@app.get("/")
async def root():
    return {"message": "AutoBook Text Generation Service is running"}


@app.post("/generate-story", response_model=StoryResponse)
async def generate_story(request: StoryRequest):
    try:
        response = await text_service.generate_story(request)
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health_check():
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)