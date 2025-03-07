from fastapi import FastAPI, HTTPException, Depends
from app.models import PdfGenerationRequest, PdfGenerationResponse
from app.services import PdfAssemblyService

app = FastAPI(title="AutoBook PDF Assembly Service")

pdf_service = PdfAssemblyService()


@app.get("/")
async def root():
    return {"message": "AutoBook PDF Assembly Service is running"}


@app.post("/generate-pdf", response_model=PdfGenerationResponse)
async def generate_pdf(request: PdfGenerationRequest):
    try:
        response = await pdf_service.generate_pdf(request)
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health_check():
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)